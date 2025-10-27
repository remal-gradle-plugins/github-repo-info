package name.remal.gradle_plugins.github_repository_info;

import static name.remal.gradle_plugins.github_repository_info.GitHubJsonDeserializer.deserializerGitHubRepositoryContributorsInfo;
import static name.remal.gradle_plugins.github_repository_info.GitHubJsonDeserializer.deserializerGitHubRepositoryInfo;
import static name.remal.gradle_plugins.github_repository_info.GitHubJsonDeserializer.deserializerGitHubRepositoryLanguagesInfo;
import static name.remal.gradle_plugins.github_repository_info.GitHubJsonDeserializer.deserializerGitHubRepositoryLicenseFileInfo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.INTEGER;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Optional;
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
            });
        });

        // TODO: inherit `CI` and `GITHUB_ACTIONS` environment variables

        // TODO: inherit `GITHUB_TOKEN` and `GITHUB_ACTIONS_TOKEN` environment variables instead
        project.putGradleProperty(
            "name.remal.github-repository-info.api-token",
            Optional.ofNullable(System.getenv("GITHUB_TOKEN"))
                .or(() -> Optional.ofNullable(System.getenv("GITHUB_ACTIONS_TOKEN")))
                .or(() -> Optional.ofNullable(System.getProperty("name.remal.github-repository-info.api-token")))
                .orElse(null)
        );
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


    @Nested
    class Tasks {

        final Path outputFile = project.resolveRelativePath("output.json");

        @Test
        void repository() {
            project.forBuildFile(build -> {
                build.addImport(RetrieveGitHubRepositoryInfo.class);
                build.block("tasks.register('retrieveInfo', RetrieveGitHubRepositoryInfo)", task -> {
                    task.line("outputJsonFile = file('%s')", task.escapeString(outputFile.toString()));
                });
            });

            project.assertBuildSuccessfully("retrieveInfo");

            var repository = deserializerGitHubRepositoryInfo(outputFile);
            assertEquals("remal-gradle-plugins/github-repository-info", repository.getFullName());
        }

        @Test
        void license() {
            project.forBuildFile(build -> {
                build.addImport(RetrieveGitHubRepositoryLicenseFileInfo.class);
                build.block("tasks.register('retrieveInfo', RetrieveGitHubRepositoryLicenseFileInfo)", task -> {
                    task.line("outputJsonFile = file('%s')", task.escapeString(outputFile.toString()));
                });
            });

            project.assertBuildSuccessfully("retrieveInfo");

            var licenseFile = deserializerGitHubRepositoryLicenseFileInfo(outputFile);
            assertEquals("LICENSE", licenseFile.getPath());
        }

        @Test
        void contributors() {
            project.forBuildFile(build -> {
                build.addImport(RetrieveGitHubRepositoryContributorsInfo.class);
                build.block("tasks.register('retrieveInfo', RetrieveGitHubRepositoryContributorsInfo)", task -> {
                    task.line("outputJsonFile = file('%s')", task.escapeString(outputFile.toString()));
                });
            });

            project.assertBuildSuccessfully("retrieveInfo");

            var contributors = deserializerGitHubRepositoryContributorsInfo(outputFile);
            assertThat(contributors).anyMatch(map -> "remal".equals(map.getLogin()));
        }

        @Test
        void languages() {
            project.forBuildFile(build -> {
                build.addImport(RetrieveGitHubRepositoryLanguagesInfo.class);
                build.block("tasks.register('retrieveInfo', RetrieveGitHubRepositoryLanguagesInfo)", task -> {
                    task.line("outputJsonFile = file('%s')", task.escapeString(outputFile.toString()));
                });
            });

            project.assertBuildSuccessfully("retrieveInfo");

            var languages = deserializerGitHubRepositoryLanguagesInfo(outputFile);
            assertThat(languages)
                .extractingByKey("Java")
                .asInstanceOf(INTEGER)
                .isGreaterThan(0);
        }

    }

}
