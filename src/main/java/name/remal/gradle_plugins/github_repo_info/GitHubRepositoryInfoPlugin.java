package name.remal.gradle_plugins.github_repo_info;

import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public abstract class GitHubRepositoryInfoPlugin implements Plugin<Project> {

    public static final String GITHUB_REPO_INFO_EXTENSION_NAME = doNotInline("githubRepositoryInfo");

    @Override
    public void apply(Project project) {
        project.getExtensions().create(GITHUB_REPO_INFO_EXTENSION_NAME, GitHubRepositoryInfoExtension.class);
    }

}
