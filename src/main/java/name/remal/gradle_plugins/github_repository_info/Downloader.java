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
import static lombok.AccessLevel.PUBLIC;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

        var gzipContentOut = new ByteArrayOutputStream();
        var allContentEncodings = response.headers().allValues(CONTENT_ENCODING);
        var isGzipEncoding = !allContentEncodings.isEmpty()
            && allContentEncodings.stream().allMatch("gzip"::equalsIgnoreCase);
        if (isGzipEncoding) {
            gzipContentOut.write(response.body());
        } else {
            try (var out = new GZIPOutputStream(gzipContentOut)) {
                out.write(response.body());
            }
        }
        var gzipContent = gzipContentOut.toByteArray();

        var charset = response.headers().firstValue(CONTENT_TYPE)
            .map(contentType -> {
                try {
                    return MediaType.parse(contentType);
                } catch (Exception ignored) {
                    return null;
                }
            })
            .flatMap(mediaType -> mediaType.charset().toJavaUtil())
            .orElse(UTF_8);

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
            var message = format(
                "GitHub REST API request to %s %s failed with status code %s",
                request.method(),
                request.uri(),
                statusCode
            );
            if (statusCode == 408
                || statusCode == 429
                || statusCode >= 500
            ) {
                throw new GitHubRestApiRequestException.Retryable(message);
            } else {
                throw new GitHubRestApiRequestException.NotRetryable(message);
            }
        }

        return response;
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
