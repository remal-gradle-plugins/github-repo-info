package name.remal.gradle_plugins.github_repository_info;

import static java.lang.String.join;
import static java.lang.System.identityHashCode;
import static name.remal.gradle_plugins.toolkit.CiUtils.getCiSystem;
import static name.remal.gradle_plugins.toolkit.GradleManagedObjectsUtils.copyManagedProperties;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.doNotInline;
import static name.remal.gradle_plugins.toolkit.git.GitUtils.findGitRepositoryRootFor;

import java.util.Optional;
import name.remal.gradle_plugins.toolkit.CiSystem;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public abstract class GitHubRepositoryInfoPlugin implements Plugin<Project> {

    public static final String GITHUB_REPOSITORY_INFO_EXTENSION_NAME = doNotInline("githubRepositoryInfo");

    @Override
    @SuppressWarnings("unchecked")
    public void apply(Project project) {
        var extension = project.getExtensions().create(
            GITHUB_REPOSITORY_INFO_EXTENSION_NAME,
            GitHubRepositoryInfoExtension.class
        );
        extension.getRepositoryRootDir().fileProvider(project.provider(() -> {
            var buildDir = getCiSystem()
                .flatMap(CiSystem::getBuildDir)
                .orElse(null);
            if (buildDir != null) {
                return buildDir;
            }
            return findGitRepositoryRootFor(project.getRootDir());
        })).finalizeValueOnRead();

        var downloader = project.getGradle().getSharedServices().registerIfAbsent(
            join(
                "|",
                Downloader.class.getName(),
                String.valueOf(identityHashCode(Downloader.class)),
                Optional.ofNullable(Downloader.class.getClassLoader())
                    .map(System::identityHashCode)
                    .map(Object::toString)
                    .orElse("")
            ),
            Downloader.class,
            __ -> { }
        );
        extension.getDownloader().set(downloader);

        project.getTasks().withType(AbstractRetrieveGitHubRepositoryInfoTask.class).configureEach(task -> {
            copyManagedProperties(extension, task);

            task.getDownloader().set(downloader);
            task.usesService(downloader);
        });
    }

}
