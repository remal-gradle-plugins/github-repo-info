package name.remal.gradle_plugins.github_repo_info;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.CacheableTask;

@CacheableTask
public abstract class RetrieveGitHubRepoInfo
    extends DefaultTask
    implements GitHubRepoInfoSettings {
}
