package name.remal.gradle_plugins.github_repository_info;

import name.remal.gradle_plugins.github_repository_info.info.GitHubFullRepository;
import org.gradle.api.tasks.CacheableTask;

@CacheableTask
public abstract class RetrieveGitHubRepositoryInfo
    extends AbstractRetrieveGitHubRepositoryInfoTask<GitHubFullRepository> {

    @Override
    protected String createRelativeUrl() {
        return "/repos/" + getRepositoryFullName().get();
    }

}
