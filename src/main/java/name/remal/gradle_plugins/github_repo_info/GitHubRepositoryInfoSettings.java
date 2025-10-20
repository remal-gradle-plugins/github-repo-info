package name.remal.gradle_plugins.github_repo_info;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

interface GitHubRepositoryInfoSettings {

    @Input
    Property<String> getGitHubApiUrl();

    @Input
    Property<String> getGitHubApiToken();

    @Input
    Property<String> getRepositoryFullName();

}
