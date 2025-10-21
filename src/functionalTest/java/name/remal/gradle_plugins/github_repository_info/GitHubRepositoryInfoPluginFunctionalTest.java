package name.remal.gradle_plugins.github_repository_info;

import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.toolkit.testkit.functional.GradleProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class GitHubRepositoryInfoPluginFunctionalTest {

    final GradleProject project;

    @BeforeEach
    void beforeEach() {
        project.forBuildFile(build -> {
            build.applyPlugin("name.remal.github-repository-info");

            build.block("githubRepositoryInfo", ext -> {
                ext.line("githubApiUrl = 'https://api.github.com'");
                ext.line("repositoryFullName = 'remal-gradle-plugins/github-repository-info'");
                ext.line("githubApiToken = ''");
            });
        });
    }

    @Nested
    class ExtensionProperties {

        @Test
        void repository() {
            project.forBuildFile(build -> {
                build.line("def repository = githubRepositoryInfo.repository.orNull");
                build.line("assert repository != null");
                build.line("assert repository?.name == 'github-repository-info'");
                build.line("assert repository?.fullName == 'remal-gradle-plugins/github-repository-info'");
                build.line("assert repository?.owner?.login == 'remal-gradle-plugins'");
            });

            project.assertBuildSuccessfully("help");
        }

        @Test
        void license() {
            project.forBuildFile(build -> {
                build.line("def licenseFile = githubRepositoryInfo.licenseFile.orNull");
                build.line("assert licenseFile != null");
                build.line("assert licenseFile?.license?.key == 'mit'");
                build.line("assert licenseFile?.license?.name == 'MIT License'");
            });

            project.assertBuildSuccessfully("help");
        }

        @Test
        void contributors() {
            project.forBuildFile(build -> {
                build.line("def contributors = githubRepositoryInfo.contributors.get()");
                build.line("assert contributors.size() > 0");
                build.line("assert !!contributors.find { it.login == 'remal' }");
            });

            project.assertBuildSuccessfully("help");
        }

        @Test
        void languages() {
            project.forBuildFile(build -> {
                build.line("def languages = githubRepositoryInfo.languages.get()");
                build.line("assert languages.size() > 0");
                build.line("assert languages['Java'] != null && languages['Java'] > 0");
            });

            project.assertBuildSuccessfully("help");
        }

    }

}
