package name.remal.gradle_plugins.github_repository_info;

import static name.remal.gradle_plugins.github_repository_info.GitHubJsonDeserializer.deserializerGitHubRepositoryContributorsInfo;
import static name.remal.gradle_plugins.github_repository_info.GitHubJsonDeserializer.deserializerGitHubRepositoryInfo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
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

        project.inheritEnvironmentVariables(
            "CI",
            "GITHUB_ACTIONS",
            "GITHUB_TOKEN",
            "GITHUB_ACTIONS_TOKEN"
        );

        project.putGradleProperty(
            "name.remal.github-repository-info.api-token",
            System.getProperty("name.remal.github-repository-info.api-token")
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
        void contributors() {
            project.forBuildFile(build -> {
                build.line("def contributors = githubRepositoryInfo.contributors.get()");
                build.line("assert contributors.size() > 0");
                build.line("assert !!contributors.find { it.login == 'remal' }");
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
        void dependencyOnResult() {
            project.withoutConfigurationCache();

            project.forBuildFile(build -> {
                build.addImport(RetrieveGitHubRepositoryLicenseFileInfo.class);
                build.line(
                    "def retrieveInfo = tasks.register('retrieveInfo', RetrieveGitHubRepositoryLicenseFileInfo)"
                );

                build.block("abstract class InfoConsumerTask extends DefaultTask", task -> {
                    task.line("@Input");
                    task.line("abstract Property<Object> getInfo()");

                    task.line("@TaskAction");
                    task.block("void execute()", action -> {
                        action.line("def info = this.info.get()");
                        action.line("assert info.path == 'LICENSE'");
                    });
                });

                build.addStaticImport(GitHubJsonDeserializer.class, "deserializerGitHubRepositoryLicenseFileInfo");
                build.line("def deserializedInfo = retrieveInfo.flatMap { it.outputJsonFile }"
                    + ".map { deserializerGitHubRepositoryLicenseFileInfo(it) }"
                );

                build.block("tasks.register('consumerTask', InfoConsumerTask)", task -> {
                    task.line("info = deserializedInfo");
                });
            });

            project.assertBuildSuccessfully("consumerTask");
        }

    }

}
