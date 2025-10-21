package name.remal.gradle_plugins.github_repository_info;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import name.remal.gradle_plugins.github_repository_info.info.GitHubFullRepository;
import org.gradle.api.services.BuildServiceParameters.None;
import org.junit.jupiter.api.Test;

class DownloaderComponentTest {

    final Downloader downloader = new Downloader() {
        @Override
        @SuppressWarnings("OverridesJavaxInjectableMethod")
        public None getParameters() {
            throw new UnsupportedOperationException();
        }
    };

    @Test
    void repository() {
        var info = downloader.download(
            "https://api.github.com/",
            "/repos/remal-gradle-plugins/github-repository-info",
            null,
            GitHubFullRepository.class
        );

        assertNotNull(info);
        assertEquals("github-repository-info", info.getName());
        assertEquals("remal-gradle-plugins/github-repository-info", info.getFullName());
        assertEquals("remal-gradle-plugins", info.getOwner().getLogin());
    }

}
