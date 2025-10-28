package name.remal.gradle_plugins.github_repository_info;

import static name.remal.gradle_plugins.github_repository_info.GitHubJsonDeserializer.deserializerGitHubRepositoryContributorsInfo;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.gradle.api.Project;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class RetrieveGitHubRepositoryContributorsInfoComponentTest {

    final String githubApiUrl = "https://api.github.com/";

    final String repositoryFullName = "remal-gradle-plugins/github-repository-info";

    @Nullable
    final String githubApiToken = Optional.ofNullable(System.getenv("GITHUB_TOKEN"))
        .or(() -> Optional.ofNullable(System.getenv("GITHUB_ACTIONS_TOKEN")))
        .or(() -> Optional.ofNullable(System.getProperty("name.remal.github-repository-info.api-token")))
        .orElse(null);


    final Project project;
    final RetrieveGitHubRepositoryContributorsInfo task;

    public RetrieveGitHubRepositoryContributorsInfoComponentTest(Project project) {
        project.getPluginManager().apply(GitHubRepositoryInfoPlugin.class);

        this.project = project;
        this.task = project.getTasks().register(
            "infoTask",
            RetrieveGitHubRepositoryContributorsInfo.class,
            task -> {
                task.getGithubApiUrl().set(githubApiUrl);
                task.getRepositoryFullName().set(repositoryFullName);
                task.getGithubApiToken().set(githubApiToken);
            }
        ).get();
    }


    @Test
    void execution() throws Throwable {
        task.execute();

        var contributors = deserializerGitHubRepositoryContributorsInfo(task.getOutputJsonFile().get());
        assertThat(contributors).isNotEmpty();
    }

}
