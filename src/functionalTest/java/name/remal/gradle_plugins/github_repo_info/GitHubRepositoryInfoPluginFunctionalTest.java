package name.remal.gradle_plugins.github_repo_info;

import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.toolkit.testkit.functional.GradleProject;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class GitHubRepositoryInfoPluginFunctionalTest {

    final GradleProject project;

    @Test
    void helpTaskWorks() {
        project.getBuildFile().applyPlugin("name.remal.github-repository-info");
        project.assertBuildSuccessfully("help");
    }

}
