package name.remal.gradle_plugins.github_repository_info;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.build_time_constants.api.BuildTimeConstants.getStringProperty;
import static name.remal.gradle_plugins.github_repository_info.HttpClientUtils.sendHttpRequest;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.NoArgsConstructor;
import org.gradle.initialization.BuildCancellationToken;
import org.jspecify.annotations.Nullable;

@NoArgsConstructor(access = PRIVATE)
class GitHubRestApiHttpClientUtils {

    public static HttpResponse<byte[]> sendGitHubRestApiHttpRequest(
        HttpRequest request,
        @Nullable BuildCancellationToken cancellationToken
    ) {
        return sendHttpRequest(request, cancellationToken, (response, message) -> {
            var curRequest = response.request();

            message
                .append("GitHub REST API request ").append(curRequest.method()).append(' ').append(curRequest.uri())
                .append(" failed with status code ").append(response.statusCode()).append('.');

            response.headers().firstValue("X-RateLimit-Remaining")
                .filter("0"::equals)
                .ifPresent(__ -> {
                    var isAuthenticated = curRequest.headers().firstValue(AUTHORIZATION).isPresent();
                    if (!isAuthenticated) {
                        message.append('\n')
                            .append("Rate limit exceeded, consider setting GitHub REST API key,"
                                + " which is not set right now."
                                + " See the \"Configuration\" section in the documentation for more details: "
                            )
                            .append(getStringProperty("repository.html-url"))
                            .append("#configuration .");
                    }
                });
        });
    }

}
