package name.remal.gradle_plugins.github_repository_info;

import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.packageNameOf;
import static name.remal.gradle_plugins.toolkit.reflection.ReflectionUtils.unwrapGeneratedSubclass;
import static name.remal.gradle_plugins.toolkit.testkit.ProjectValidations.executeAfterEvaluateActions;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import lombok.RequiredArgsConstructor;
import name.remal.gradle_plugins.toolkit.testkit.TaskValidations;
import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiredArgsConstructor
class GitHubRepositoryInfoPluginTest {

    final Project project;

    @BeforeEach
    void beforeEach() {
        project.getPluginManager().apply(GitHubRepositoryInfoPlugin.class);
    }

    @Test
    void tasksCanBeCreated() {
        var tasks = project.getTasks();
        assertDoesNotThrow(() -> tasks.register("repository", RetrieveGitHubRepositoryInfo.class).get());
        assertDoesNotThrow(() -> tasks.register("license", RetrieveGitHubRepositoryLicenseFileInfo.class).get());
        assertDoesNotThrow(() -> tasks.register("contributors", RetrieveGitHubRepositoryContributorsInfo.class).get());
        assertDoesNotThrow(() -> tasks.register("languages", RetrieveGitHubRepositoryLanguagesInfo.class).get());
    }

    @Test
    void pluginTasksDoNotHavePropertyProblems() {
        executeAfterEvaluateActions(project);

        var taskClassNamePrefix = packageNameOf(GitHubRepositoryInfoPlugin.class) + '.';
        project.getTasks().stream().filter(task -> {
            var taskClass = unwrapGeneratedSubclass(task.getClass());
            return taskClass.getName().startsWith(taskClassNamePrefix);
        }).map(TaskValidations::markTaskDependenciesAsSkipped).forEach(TaskValidations::assertNoTaskPropertiesProblems);
    }

}
