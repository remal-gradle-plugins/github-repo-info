package name.remal.gradle_plugins.github_repository_info;

import java.util.Map;
import org.gradle.api.tasks.CacheableTask;

@CacheableTask
public abstract class RetrieveGitHubRepositoryLanguagesInfo
    extends AbstractRetrieveGitHubRepositoryInfoTask<Map<String, Integer>> {

    @Override
    protected String createRelativeUrl() {
        return "/repos/" + getRepositoryFullName().get() + "/languages";
    }

}
