package name.remal.gradle_plugins.github_repo_info;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

interface GitHubRepoInfoSettings {

    @Input
    Property<String> getRepoName();

}
