package name.remal.gradle_plugins.github_repository_info;

import name.remal.gradle_plugins.github_repository_info.info.GitHubLicenseContent;
import org.gradle.api.tasks.CacheableTask;

@CacheableTask
public abstract class RetrieveGitHubRepositoryLicenseFileInfo
    extends AbstractRetrieveGitHubRepositoryInfoTask<GitHubLicenseContent> {

    @Override
    protected String createRelativeUrl() {
        return "/repos/" + getRepositoryFullName().get() + "/license";
    }

}
