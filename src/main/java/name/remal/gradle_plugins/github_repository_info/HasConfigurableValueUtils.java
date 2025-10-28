package name.remal.gradle_plugins.github_repository_info;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;
import org.gradle.api.provider.HasConfigurableValue;

@NoArgsConstructor(access = PRIVATE)
abstract class HasConfigurableValueUtils {

    public static void makePropertyLazyReadOnly(HasConfigurableValue property) {
        property.disallowChanges();
        property.finalizeValueOnRead();
    }

}
