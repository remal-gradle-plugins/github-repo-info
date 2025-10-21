package name.remal.gradle_plugins.github_repository_info;

import java.util.List;
import name.remal.gradle_plugins.github_repository_info.info.GitHubContributor;
import org.gradle.api.tasks.CacheableTask;

@CacheableTask
public abstract class RetrieveGitHubRepositoryContributorsInfo
    extends AbstractRetrieveGitHubRepositoryInfoTask<List<GitHubContributor>> {

    @Override
    protected String createRelativeUrl() {
        return "/repos/" + getRepositoryFullName().get() + "/contributors";
    }

}
