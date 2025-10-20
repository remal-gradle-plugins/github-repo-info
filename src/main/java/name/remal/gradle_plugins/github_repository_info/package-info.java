@Value.Style(
    defaults = @Value.Immutable(
    ),
    visibility = ImplementationVisibility.SAME,
    builderVisibility = BuilderVisibility.PUBLIC,
    jdkOnly = true,
    get = {"is*", "get*"},
    optionalAcceptNullable = true,
    privateNoargConstructor = true,
    typeBuilder = "*Builder",
    typeInnerBuilder = "BaseBuilder",
    allowedClasspathAnnotations = {
        org.immutables.value.Generated.class,
        Nullable.class,
        javax.annotation.Nullable.class,
        Immutable.class,
        ThreadSafe.class,
        NotThreadSafe.class,
    },
    depluralize = true
)
@NullMarked
package name.remal.gradle_plugins.github_repository_info;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.BuilderVisibility;
import org.immutables.value.Value.Style.ImplementationVisibility;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
