package name.remal.gradle_plugins.github_repository_info;

import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.github_repository_info.JsonUtils.GSON;

import lombok.NoArgsConstructor;
import name.remal.gradle_plugins.github_repository_info.info.GitHubInfoType;

@NoArgsConstructor(access = PRIVATE)
public abstract class GitHubJsonDeserializer {

    public static <T extends GitHubInfoType> T deserializerGitHubJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

}
