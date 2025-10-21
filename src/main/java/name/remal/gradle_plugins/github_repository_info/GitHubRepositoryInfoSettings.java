package name.remal.gradle_plugins.github_repository_info;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

interface GitHubRepositoryInfoSettings {

    @Input
    Property<String> getGithubApiUrl();

    @Input
    Property<String> getRepositoryFullName();

    @Internal
    Property<String> getGithubApiToken();

}
