package name.remal.gradle_plugins.github_repository_info;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.ACCEPT_ENCODING;
import static com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_ENCODING;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static lombok.AccessLevel.PUBLIC;
import static name.remal.gradle_plugins.build_time_constants.api.BuildTimeConstants.getStringProperty;
import static name.remal.gradle_plugins.github_repository_info.JsonUtils.GSON;

import com.google.common.net.MediaType;
import com.google.gson.reflect.TypeToken;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import name.remal.gradle_plugins.github_repository_info.info.GitHubInfoType;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.jspecify.annotations.Nullable;

@NoArgsConstructor(access = PUBLIC, onConstructor_ = {@Inject})
abstract class Downloader implements BuildService<BuildServiceParameters.None> {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration BASE_TIMEOUT_BETWEEN_ATTEMPTS = Duration.ofSeconds(1);

    private static final boolean ADD_RATE_LIMIT_HEADERS_TO_ERROR_MESSAGE = false;

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(1);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .followRedirects(Redirect.NORMAL)
        .build();


    public <T> T download(
        String apiHost,
        String relativeUrl,
        @Nullable String apiToken,
        TypeToken<T> type
    ) {
        while (apiHost.endsWith("/")) {
            apiHost = apiHost.substring(0, apiHost.length() - 1);
        }
        while (relativeUrl.startsWith("/")) {
            relativeUrl = relativeUrl.substring(1);
        }

        if ("".equals(apiToken)) {
            apiToken = null;
        }

        var cachedContent = getFromCacheOrDownload(apiHost, relativeUrl, apiToken);
        var content = cachedContent.toString();
        var result = GSON.fromJson(content, type);
        return requireNonNull(result);
    }

    public <T extends GitHubInfoType> T download(
        String apiHost,
        String relativeUrl,
        @Nullable String apiToken,
        Class<T> type
    ) {
        return download(
            apiHost,
            relativeUrl,
            apiToken,
            TypeToken.get(type)
        );
    }

    private static final ConcurrentMap<CacheKey, CachedContent> CACHE = new ConcurrentHashMap<>();

    private CachedContent getFromCacheOrDownload(
        String apiHost,
        String relativeUrl,
        @Nullable String apiToken
    ) {
        return CACHE.computeIfAbsent(new CacheKey(apiHost, relativeUrl, apiToken), key ->
            downloadImpl(key.getApiHost(), key.getRelativeUrl(), key.getApiToken())
        );
    }

    @SneakyThrows
    private CachedContent downloadImpl(
        String apiHost,
        String relativeUrl,
        @Nullable String apiToken
    ) {
        var requestUri = URI.create(apiHost + '/' + relativeUrl);
        var requestBuilder = HttpRequest.newBuilder()
            .GET()
            .uri(requestUri)
            .setHeader(ACCEPT, "application/json")
            .setHeader(ACCEPT_LANGUAGE, "en-US")
            .setHeader(ACCEPT_ENCODING, "gzip")
            .timeout(REQUEST_TIMEOUT);
        if (apiToken != null) {
            requestBuilder.header(AUTHORIZATION, "Bearer " + apiToken);
        }
        var request = requestBuilder.build();

        var response = sendRequestWithRetries(request);
        var gzipContent = getGzippedResponseBody(response);
        var charset = getResponseCharset(response);
        return new CachedContent(gzipContent, charset);
    }

    @SneakyThrows
    private HttpResponse<byte[]> sendRequestWithRetries(HttpRequest request) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return sendRequest(request);

            } catch (GitHubRestApiRequestException.Retryable exception) {
                if (attempt < MAX_ATTEMPTS) {
                    var sleepDuration = BASE_TIMEOUT_BETWEEN_ATTEMPTS.multipliedBy(attempt);
                    Thread.sleep(sleepDuration.toMillis());
                } else {
                    throw exception;
                }
            }
        }

        throw new AssertionError("unreachable");
    }

    @SneakyThrows
    @SuppressWarnings("java:S3776")
    private HttpResponse<byte[]> sendRequest(HttpRequest request) {
        final HttpResponse<byte[]> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new GitHubRestApiRequestException.Retryable(
                format(
                    "Failed to send request to GitHub REST API: %s %s",
                    request.method(),
                    request.uri()
                ),
                e
            );
        }

        var statusCode = response.statusCode();
        if (statusCode != 200) {
            var message = new StringBuilder();
            Supplier<StringBuilder> withNewLineIfNeeded = () -> {
                if (message.length() > 0) {
                    message.append('\n');
                }
                return message;
            };

            withNewLineIfNeeded.get()
                .append("GitHub REST API request ").append(request.method()).append(' ').append(request.uri())
                .append(" failed with status code ").append(statusCode).append('.');

            if (ADD_RATE_LIMIT_HEADERS_TO_ERROR_MESSAGE) {
                Stream.of(
                    "X-RateLimit-Limit",
                    "X-RateLimit-Used",
                    "X-RateLimit-Remaining"
                ).forEach(header -> {
                    response.headers().firstValue(header)
                        .filter(not(String::isEmpty))
                        .ifPresent(value -> {
                            withNewLineIfNeeded.get()
                                .append(header).append(": ").append(value).append('.');
                        });
                });
            }

            response.headers().firstValue("X-RateLimit-Remaining")
                .filter("0"::equals)
                .ifPresent(__ -> {
                    withNewLineIfNeeded.get()
                        .append("Rate limit exceeded, consider setting GitHub REST API key."
                            + " See the \"Configuration\" section in the documentation for more details: ")
                        .append(getStringProperty("repository.html-url"))
                        .append("#configuration .");
                });

            var responseBody = response.body();
            if (responseBody.length == 0) {
                withNewLineIfNeeded.get()
                    .append("Response body is empty.");
            } else {
                var decompressedContent = getPlainResponseBody(response);
                if (decompressedContent.length > 8192) {
                    withNewLineIfNeeded.get()
                        .append("Response body of ").append(decompressedContent.length).append(" bytes.");
                } else if (isTextResponse(response)) {
                    var charset = getResponseCharset(response);
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
                throw new GitHubRestApiRequestException.Retryable(message.toString());
            } else {
                throw new GitHubRestApiRequestException.NotRetryable(message.toString());
            }
        }

        return response;
    }

    @SneakyThrows
    private static byte[] getGzippedResponseBody(HttpResponse<byte[]> response) {
        var gzippedOut = new ByteArrayOutputStream();
        if (isGzipEncodingResponse(response)) {
            gzippedOut.write(response.body());
        } else {
            try (var out = new GZIPOutputStream(gzippedOut)) {
                out.write(response.body());
            }
        }
        return gzippedOut.toByteArray();
    }

    @SneakyThrows
    private static byte[] getPlainResponseBody(HttpResponse<byte[]> response) {
        var plainOut = new ByteArrayOutputStream();
        if (isGzipEncodingResponse(response)) {
            try (var unGzipIn = new GZIPInputStream(new ByteArrayInputStream(response.body()))) {
                unGzipIn.transferTo(plainOut);
            }
        } else {
            plainOut.write(response.body());
        }
        return plainOut.toByteArray();
    }

    private static boolean isGzipEncodingResponse(HttpResponse<?> response) {
        var allContentEncodings = response.headers().allValues(CONTENT_ENCODING);
        return !allContentEncodings.isEmpty()
            && allContentEncodings.stream().allMatch("gzip"::equalsIgnoreCase);
    }

    private static final Pattern TEXT_MEDIA_TYPE =
        Pattern.compile("\\b(?:text|html|json|xml|javascript|css|yaml)\\b", CASE_INSENSITIVE);

    private static boolean isTextResponse(HttpResponse<?> response) {
        var plainContentType = getContentType(response)
            .map(MediaType::withoutParameters)
            .map(MediaType::toString)
            .orElse(null);
        if (plainContentType == null) {
            return false;
        }

        return TEXT_MEDIA_TYPE.matcher(plainContentType).find();
    }

    private static Charset getResponseCharset(HttpResponse<?> response) {
        return getContentType(response)
            .flatMap(mediaType -> mediaType.charset().toJavaUtil())
            .orElse(UTF_8);
    }

    private static Optional<MediaType> getContentType(HttpResponse<?> response) {
        return response.headers().firstValue(CONTENT_TYPE)
            .map(contentType -> {
                try {
                    return MediaType.parse(contentType);
                } catch (Exception ignored) {
                    return null;
                }
            });
    }

    @Value
    private static class CacheKey {
        String apiHost;
        String relativeUrl;
        @Nullable String apiToken;
    }

    @Value
    private static class CachedContent {
        byte[] gzipContent;
        Charset charset;

        @Override
        @SneakyThrows
        public String toString() {
            var outBytes = new ByteArrayOutputStream();
            try (var gzipIn = new GZIPInputStream(new ByteArrayInputStream(gzipContent))) {
                gzipIn.transferTo(outBytes);
            }
            return outBytes.toString(charset);
        }
    }

}
