package name.remal.gradle_plugins.github_repository_info;

import static com.google.common.net.HttpHeaders.CONTENT_ENCODING;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.RETRY_AFTER;
import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static lombok.AccessLevel.PRIVATE;

import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.gradle.api.BuildCancelledException;
import org.gradle.initialization.BuildCancellationToken;
import org.jspecify.annotations.Nullable;

@NoArgsConstructor(access = PRIVATE)
abstract class HttpClientUtils {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .followRedirects(Redirect.NORMAL)
        .build();


    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofMinutes(1);


    private static final Set<String> IDEMPOTENT_HTTP_METHODS = ImmutableSet.of(
        "GET",
        "HEAD",
        "PUT",
        "DELETE",
        "OPTIONS",
        "TRACE"
    );

    private static final int MAX_RETRIES = 5;

    private static final Duration BASE_TIMEOUT_BETWEEN_RETRIES = Duration.ofSeconds(1);

    private static final Duration MAX_RETRY_AFTER = Duration.ofSeconds(10);


    private static final Pattern TEXT_MEDIA_TYPE =
        Pattern.compile("\\b(?:text|html|json|xml|javascript|css|yaml)\\b", CASE_INSENSITIVE);


    public static HttpResponse<byte[]> sendHttpRequest(
        HttpRequest request,
        @Nullable BuildCancellationToken cancellationToken
    ) {
        return sendHttpRequest(request, cancellationToken, null);
    }

    public static HttpResponse<byte[]> sendHttpRequest(
        HttpRequest request,
        @Nullable BuildCancellationToken cancellationToken,
        @Nullable BiConsumer<HttpResponse<byte[]>, StringBuilder> errorMessageHeaderGenerator
    ) {
        if (errorMessageHeaderGenerator == null) {
            errorMessageHeaderGenerator = HttpClientUtils::generateDefaultErrorMessage;
        }

        var timeout = request.timeout().orElse(null);
        if (timeout == null) {
            request = newHttpRequestBuilderFrom(request)
                .timeout(DEFAULT_REQUEST_TIMEOUT)
                .build();
        }

        if (isIdempotentRequest(request)) {
            return sendRequestWithRetries(request, cancellationToken, errorMessageHeaderGenerator);
        } else {
            return sendRequestWithoutRetries(request, cancellationToken, errorMessageHeaderGenerator);
        }
    }

    private static void generateDefaultErrorMessage(HttpResponse<byte[]> response, StringBuilder message) {
        var curRequest = response.request();
        message
            .append("HTTP request ").append(curRequest.method()).append(' ').append(curRequest.uri())
            .append(" failed with status code ").append(response.statusCode()).append('.');
    }

    private static boolean isIdempotentRequest(HttpRequest request) {
        var method = request.method();
        return IDEMPOTENT_HTTP_METHODS.contains(method);
    }

    @SneakyThrows
    private static HttpResponse<byte[]> sendRequestWithRetries(
        HttpRequest request,
        @Nullable BuildCancellationToken cancellationToken,
        BiConsumer<HttpResponse<byte[]>, StringBuilder> errorMessageHeaderGenerator
    ) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            if (cancellationToken != null && cancellationToken.isCancellationRequested()) {
                throw new BuildCancelledException();
            }

            try {
                return sendHttpRequestImpl(request, true, cancellationToken, errorMessageHeaderGenerator);

            } catch (HttpRequestException.Retryable exception) {
                if (attempt < MAX_RETRIES) {
                    var sleepDuration = BASE_TIMEOUT_BETWEEN_RETRIES.multipliedBy(attempt);
                    var retryAfter = exception.getRetryAfter();
                    if (retryAfter != null
                        && retryAfter.compareTo(sleepDuration) > 0
                        && retryAfter.compareTo(MAX_RETRY_AFTER) <= 0
                    ) {
                        sleepDuration = retryAfter;
                    }
                    Thread.sleep(sleepDuration.toMillis());
                } else {
                    throw exception;
                }
            }
        }

        throw new AssertionError("unreachable");
    }

    private static HttpResponse<byte[]> sendRequestWithoutRetries(
        HttpRequest request,
        @Nullable BuildCancellationToken cancellationToken,
        BiConsumer<HttpResponse<byte[]>, StringBuilder> errorMessageHeaderGenerator
    ) {
        return sendHttpRequestImpl(request, false, cancellationToken, errorMessageHeaderGenerator);
    }

    @SneakyThrows
    @SuppressWarnings("java:S3776")
    private static HttpResponse<byte[]> sendHttpRequestImpl(
        HttpRequest request,
        boolean isRetryable,
        @Nullable BuildCancellationToken cancellationToken,
        BiConsumer<HttpResponse<byte[]>, StringBuilder> errorMessageHeaderGenerator
    ) {
        var currentThread = Thread.currentThread();
        Runnable cancellationCallback = currentThread::interrupt;
        if (cancellationToken != null) {
            cancellationToken.addCallback(cancellationCallback);
        }

        final HttpResponse<byte[]> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BuildCancelledException();

        } catch (IOException exception) {
            var message = format(
                "Failed to send request to GitHub REST API: %s %s",
                request.method(),
                request.uri()
            );
            if (isRetryable) {
                throw new HttpRequestException.Retryable(message, null, exception);
            } else {
                throw new HttpRequestException.NotRetryable(message, exception);
            }

        } finally {
            if (cancellationToken != null) {
                cancellationToken.removeCallback(cancellationCallback);
            }
        }

        var statusCode = response.statusCode();
        if (statusCode >= 300) {
            var message = new StringBuilder();
            errorMessageHeaderGenerator.accept(response, message);

            Supplier<StringBuilder> withNewLineIfNeeded = () -> {
                if (message.length() > 0) {
                    message.append('\n');
                }
                return message;
            };

            var responseBody = response.body();
            if (responseBody.length == 0) {
                withNewLineIfNeeded.get()
                    .append("Response body is empty.");
            } else {
                var decompressedContent = HttpClientUtils.getPlainHttpResponseBody(response);
                if (decompressedContent.length > 8192) {
                    withNewLineIfNeeded.get()
                        .append("Response body of ").append(decompressedContent.length).append(" bytes.");
                } else if (HttpClientUtils.isTextHttpResponse(response)) {
                    var charset = HttpClientUtils.getHttpResponseCharset(response);
                    var content = new String(decompressedContent, charset);
                    withNewLineIfNeeded.get()
                        .append("Response body:\n").append(content).append('\n');
                } else {
                    withNewLineIfNeeded.get()
                        .append("Binary response body of ").append(decompressedContent.length).append(" bytes.");
                }
            }

            if (statusCode == 408
                || statusCode == 429
                || statusCode >= 500
            ) {
                if (isRetryable) {
                    var retryAfter = HttpClientUtils.parseHttpResponseRetryAfterHeader(response);
                    throw new HttpRequestException.Retryable(message.toString(), retryAfter);
                }
            }
            throw new HttpRequestException.NotRetryable(message.toString());
        }

        return response;
    }


    public static HttpRequest.Builder newHttpRequestBuilderFrom(HttpRequest request) {
        var builder = HttpRequest.newBuilder()
            .uri(request.uri())
            .expectContinue(request.expectContinue())
            .method(request.method(), request.bodyPublisher().orElseGet(BodyPublishers::noBody));

        request.headers().map().forEach((header, values) ->
            values.forEach(value ->
                builder.header(header, value)
            )
        );

        request.version().ifPresent(builder::version);
        request.timeout().ifPresent(builder::timeout);

        return builder;
    }


    @SneakyThrows
    public static byte[] getPlainHttpResponseBody(HttpResponse<byte[]> response) {
        var plainOut = new ByteArrayOutputStream();
        if (isGzipEncodingHttpResponse(response)) {
            try (var unGzipIn = new GZIPInputStream(new ByteArrayInputStream(response.body()))) {
                unGzipIn.transferTo(plainOut);
            }
        } else {
            plainOut.write(response.body());
        }
        return plainOut.toByteArray();
    }

    private static boolean isGzipEncodingHttpResponse(HttpResponse<?> response) {
        var allContentEncodings = response.headers().allValues(CONTENT_ENCODING);
        return !allContentEncodings.isEmpty()
            && allContentEncodings.stream().allMatch("gzip"::equalsIgnoreCase);
    }


    public static Charset getHttpResponseCharset(HttpResponse<?> response) {
        return getHttpResponseContentType(response)
            .flatMap(mediaType -> mediaType.charset().toJavaUtil())
            .orElse(UTF_8);
    }

    public static boolean isTextHttpResponse(HttpResponse<?> response) {
        var plainContentType = getHttpResponseContentType(response)
            .map(MediaType::withoutParameters)
            .map(MediaType::toString)
            .orElse(null);
        if (plainContentType == null) {
            return false;
        }

        return TEXT_MEDIA_TYPE.matcher(plainContentType).find();
    }

    private static Optional<MediaType> getHttpResponseContentType(HttpResponse<?> response) {
        return response.headers().firstValue(CONTENT_TYPE)
            .map(contentType -> {
                try {
                    return MediaType.parse(contentType);
                } catch (Exception ignored) {
                    return null;
                }
            });
    }


    @Nullable
    public static Duration parseHttpResponseRetryAfterHeader(HttpResponse<?> response) {
        var value = response.headers().firstValue(RETRY_AFTER).orElse(null);
        if (value == null || value.isBlank()) {
            return null;
        }

        // Try to parse as seconds (e.g., "120")
        try {
            var seconds = parseLong(value);
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException e) {
            // Not a number, fall through to parse as a date.
        }

        // Try to parse as an HTTP-date (e.g., "Wed, 21 Oct 2026 07:28:00 GMT")
        try {
            var dateTime = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
            var retryInstant = dateTime.toInstant();
            var now = Instant.now();
            return Duration.between(now, retryInstant);
        } catch (DateTimeParseException ignored) {
            // Invalid format for both seconds and date
        }

        return null;
    }
}
