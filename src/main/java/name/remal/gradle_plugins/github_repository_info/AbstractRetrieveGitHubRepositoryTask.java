package name.remal.gradle_plugins.github_repository_info;

import org.gradle.api.DefaultTask;

public abstract class AbstractRetrieveGitHubRepositoryTask
    extends DefaultTask
    implements GitHubRepositoryInfoSettings {

    {
        getProject().getPluginManager().apply(GitHubRepositoryInfoPlugin.class);
    }

}
