package name.remal.gradle_plugins.github_repository_info;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;
import name.remal.gradle_plugins.github_repository_info.info.GitHubFullRepository;
import org.gradle.api.services.BuildServiceParameters.None;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class DownloaderComponentTest {

    final Downloader downloader = new Downloader() {
        @Override
        @SuppressWarnings("OverridesJavaxInjectableMethod")
        public None getParameters() {
            throw new UnsupportedOperationException();
        }
    };

    final String githubApiUrl = "https://api.github.com/";

    @Nullable
    final String githubApiToken = Optional.ofNullable(System.getenv("GITHUB_TOKEN"))
        .or(() -> Optional.ofNullable(System.getenv("GITHUB_ACTIONS_TOKEN")))
        .or(() -> Optional.ofNullable(System.getProperty("name.remal.github-repository-info.api-token")))
        .orElse(null);

    @Test
    void repository() {
        var info = downloader.download(
            githubApiUrl,
            "/repos/remal-gradle-plugins/github-repository-info",
            githubApiToken,
            GitHubFullRepository.class
        );

        assertNotNull(info);
        assertEquals("github-repository-info", info.getName());
        assertEquals("remal-gradle-plugins/github-repository-info", info.getFullName());
        assertEquals("remal-gradle-plugins", info.getOwner().getLogin());
    }

}
