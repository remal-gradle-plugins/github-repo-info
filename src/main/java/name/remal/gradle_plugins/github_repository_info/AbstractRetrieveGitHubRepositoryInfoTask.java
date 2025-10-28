package name.remal.gradle_plugins.github_repository_info;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedWriter;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.github_repository_info.JsonUtils.GSON;
import static name.remal.gradle_plugins.toolkit.PathUtils.createParentDirectories;
import static name.remal.gradle_plugins.toolkit.PathUtils.deleteRecursively;
import static name.remal.gradle_plugins.toolkit.PathUtils.normalizePath;

import com.google.errorprone.annotations.ForOverride;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.ParameterizedType;
import javax.inject.Inject;
import lombok.Getter;
import name.remal.gradle_plugins.github_repository_info.info.GitHubInfoType;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.initialization.BuildCancellationToken;

abstract class AbstractRetrieveGitHubRepositoryInfoTask<InfoType>
    extends DefaultTask
    implements GitHubRepositoryInfoSettings {

    @ForOverride
    protected abstract String createRelativeUrl();

    @Getter(value = PRIVATE, onMethod_ = {@Internal})
    private final TypeToken<InfoType> infoType;

    @SuppressWarnings({"unchecked"})
    protected AbstractRetrieveGitHubRepositoryInfoTask() {
        var type = com.google.common.reflect.TypeToken.of(getClass())
            .getSupertype(AbstractRetrieveGitHubRepositoryInfoTask.class)
            .getType();
        if (type instanceof ParameterizedType) {
            var infoType = ((ParameterizedType) type).getActualTypeArguments()[0];
            var infoTypeClass = com.google.common.reflect.TypeToken.of(infoType).getRawType();
            if (infoTypeClass == GitHubInfoType.class || infoTypeClass.isAssignableFrom(Object.class)) {
                throw new AssertionError("Type argument T is not specified for " + getClass());
            }
            this.infoType = (TypeToken<InfoType>) TypeToken.get(infoType);
        } else {
            throw new AssertionError("Not a parameterized type: " + type);
        }
    }


    @Input
    protected abstract Property<String> getRelativeUrl();

    {
        getRelativeUrl().value(getProviders().provider(
            this::createRelativeUrl
        )).finalizeValueOnRead();
    }

    @OutputFile
    @org.gradle.api.tasks.Optional
    protected abstract RegularFileProperty getCacheFile();

    {
        getCacheFile().value(
            getLayout().getBuildDirectory().file(getProviders().provider(() -> {
                getGitHubDataFetcher().finalizeValueOnRead();
                getGithubApiUrl().finalizeValueOnRead();
                getRelativeUrl().finalizeValueOnRead();

                var cacheFile = getGitHubDataFetcher().get()
                    .getCacheFile(getGithubApiUrl().get(), getRelativeUrl().get());
                return cacheFile != null ? cacheFile.toString() : null;
            }))
        ).finalizeValueOnRead();
    }

    @OutputFile
    public abstract RegularFileProperty getOutputJsonFile();

    {
        getOutputJsonFile().convention(
            getCacheFile()
                .orElse(getLayout().getBuildDirectory().file(getName() + "/output.json"))
        );
    }


    @Internal
    protected abstract Property<GitHubDataFetcher> getGitHubDataFetcher();

    @TaskAction
    public final void execute() throws Throwable {
        var outputPath = normalizePath(getOutputJsonFile().get().getAsFile().toPath());
        deleteRecursively(outputPath);

        var result = getGitHubDataFetcher().get().fetch(
            getGithubApiUrl().get(),
            getRelativeUrl().get(),
            getGithubApiToken().getOrNull(),
            infoType,
            getCancellationToken()
        );

        createParentDirectories(outputPath);
        try (var writer = newBufferedWriter(outputPath, UTF_8)) {
            GSON.toJson(result, writer);
        }
    }


    @Inject
    protected abstract ProviderFactory getProviders();

    @Inject
    protected abstract ProjectLayout getLayout();

    @Inject
    protected abstract BuildCancellationToken getCancellationToken();

}
