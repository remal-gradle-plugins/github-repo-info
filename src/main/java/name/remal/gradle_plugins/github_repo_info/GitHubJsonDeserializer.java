package name.remal.gradle_plugins.github_repo_info;

import static lombok.AccessLevel.PRIVATE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import java.util.ServiceLoader;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public abstract class GitHubJsonDeserializer {

    private static final Gson GSON;

    static {
        var gsonBuilder = new GsonBuilder();
        ServiceLoader.load(TypeAdapterFactory.class, GitHubJsonDeserializer.class.getClassLoader())
            .forEach(gsonBuilder::registerTypeAdapterFactory);
        GSON = gsonBuilder.create();
    }

    public static <T> T deserializerGitHubJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

}
