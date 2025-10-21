package name.remal.gradle_plugins.github_repository_info;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import name.remal.gradle_plugins.github_repository_info.info.GitHubFullRepository;
import name.remal.gradle_plugins.github_repository_info.info.GitHubLicenseContent;
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
        var repositoryInfo = downloader.download(
            "https://api.github.com/",
            "/repos/remal-gradle-plugins/github-repository-info",
            null,
            GitHubFullRepository.class
        );

        assertNotNull(repositoryInfo);
        assertEquals("github-repository-info", repositoryInfo.getName());
        assertEquals("remal-gradle-plugins/github-repository-info", repositoryInfo.getFullName());
        assertEquals("remal-gradle-plugins", repositoryInfo.getOwner().getLogin());
    }

    @Test
    void licenseFile() {
        var licenseFile = downloader.download(
            "https://api.github.com/",
            "/repos/remal-gradle-plugins/github-repository-info/license",
            null,
            GitHubLicenseContent.class
        );

        assertNotNull(licenseFile);
        assertNotNull(licenseFile.getLicense());
        assertEquals("mit", licenseFile.getLicense().getKey());
        assertEquals("MIT License", licenseFile.getLicense().getName());
    }

}
