package name.remal.gradle_plugins.github_repository_info;

import static com.google.gson.Strictness.LENIENT;
import static lombok.AccessLevel.PRIVATE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import java.util.ServiceLoader;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
abstract class JsonUtils {

    public static final Gson GSON;

    static {
        var gsonBuilder = new GsonBuilder()
            .setStrictness(LENIENT)
            .setPrettyPrinting();

        ServiceLoader.load(TypeAdapterFactory.class, GitHubJsonDeserializer.class.getClassLoader())
            .forEach(gsonBuilder::registerTypeAdapterFactory);

        GSON = gsonBuilder.create();
    }

}
