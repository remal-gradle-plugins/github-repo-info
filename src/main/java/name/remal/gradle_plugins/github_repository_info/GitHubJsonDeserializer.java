package name.remal.gradle_plugins.github_repository_info;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readString;
import static lombok.AccessLevel.PRIVATE;
import static name.remal.gradle_plugins.github_repository_info.JsonUtils.GSON;
import static name.remal.gradle_plugins.toolkit.PathUtils.normalizePath;

import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import name.remal.gradle_plugins.github_repository_info.info.GitHubContributor;
import name.remal.gradle_plugins.github_repository_info.info.GitHubFullRepository;
import name.remal.gradle_plugins.github_repository_info.info.GitHubLicenseContent;
import org.gradle.api.file.RegularFile;
import org.intellij.lang.annotations.Language;

@NoArgsConstructor(access = PRIVATE)
public abstract class GitHubJsonDeserializer {

    public static GitHubFullRepository deserializerGitHubRepositoryInfo(@Language("JSON") String json) {
        return GSON.fromJson(json, GitHubFullRepository.class);
    }

    @SneakyThrows
    public static GitHubFullRepository deserializerGitHubRepositoryInfo(Path jsonFile) {
        @Language("JSON") var json = readString(normalizePath(jsonFile), UTF_8);
        return deserializerGitHubRepositoryInfo(json);
    }

    public static GitHubFullRepository deserializerGitHubRepositoryInfo(File jsonFile) {
        return deserializerGitHubRepositoryInfo(jsonFile.toPath());
    }

    public static GitHubFullRepository deserializerGitHubRepositoryInfo(RegularFile jsonFile) {
        return deserializerGitHubRepositoryInfo(jsonFile.getAsFile());
    }


    public static GitHubLicenseContent deserializerGitHubRepositoryLicenseFileInfo(@Language("JSON") String json) {
        return GSON.fromJson(json, GitHubLicenseContent.class);
    }

    @SneakyThrows
    public static GitHubLicenseContent deserializerGitHubRepositoryLicenseFileInfo(Path jsonFile) {
        @Language("JSON") var json = readString(normalizePath(jsonFile), UTF_8);
        return deserializerGitHubRepositoryLicenseFileInfo(json);
    }

    public static GitHubLicenseContent deserializerGitHubRepositoryLicenseFileInfo(File jsonFile) {
        return deserializerGitHubRepositoryLicenseFileInfo(jsonFile.toPath());
    }

    public static GitHubLicenseContent deserializerGitHubRepositoryLicenseFileInfo(RegularFile jsonFile) {
        return deserializerGitHubRepositoryLicenseFileInfo(jsonFile.getAsFile());
    }


    public static List<GitHubContributor> deserializerGitHubRepositoryContributorsInfo(@Language("JSON") String json) {
        return GSON.fromJson(json, new TypeToken<>() { });
    }

    @SneakyThrows
    public static List<GitHubContributor> deserializerGitHubRepositoryContributorsInfo(Path jsonFile) {
        @Language("JSON") var json = readString(normalizePath(jsonFile), UTF_8);
        return deserializerGitHubRepositoryContributorsInfo(json);
    }

    public static List<GitHubContributor> deserializerGitHubRepositoryContributorsInfo(File jsonFile) {
        return deserializerGitHubRepositoryContributorsInfo(jsonFile.toPath());
    }

    public static List<GitHubContributor> deserializerGitHubRepositoryContributorsInfo(RegularFile jsonFile) {
        return deserializerGitHubRepositoryContributorsInfo(jsonFile.getAsFile());
    }


    public static Map<String, Integer> deserializerGitHubRepositoryLanguagesInfo(@Language("JSON") String json) {
        return GSON.fromJson(json, new TypeToken<>() { });
    }

    @SneakyThrows
    public static Map<String, Integer> deserializerGitHubRepositoryLanguagesInfo(Path jsonFile) {
        @Language("JSON") var json = readString(normalizePath(jsonFile), UTF_8);
        return deserializerGitHubRepositoryLanguagesInfo(json);
    }

    public static Map<String, Integer> deserializerGitHubRepositoryLanguagesInfo(File jsonFile) {
        return deserializerGitHubRepositoryLanguagesInfo(jsonFile.toPath());
    }

    public static Map<String, Integer> deserializerGitHubRepositoryLanguagesInfo(RegularFile jsonFile) {
        return deserializerGitHubRepositoryLanguagesInfo(jsonFile.getAsFile());
    }

}
