**Tested on Java LTS versions from <!--property:java-runtime.min-version-->11<!--/property--> to <!--property:java-runtime.max-version-->25<!--/property-->.**

**Tested on Gradle versions from <!--property:gradle-api.min-version-->8.0<!--/property--> to <!--property:gradle-api.max-version-->9.2.0-rc-2<!--/property-->.**

# `name.remal.github-repository-info` plugin

[![configuration cache: supported](https://img.shields.io/static/v1?label=configuration%20cache&message=supported&color=success)](https://docs.gradle.org/current/userguide/configuration_cache.html)

<!--plugin-usage:name.remal.github-repository-info-->

```groovy
plugins {
  id 'name.remal.github-repository-info' version '1-SNAPSHOT'
}
```

<!--/plugin-usage-->

&nbsp;

This Gradle plugin that provides functionality to access GitHub repository information
directly from the build environment.

It can be useful for projects that need to load some GitHub repository information dynamically.
This is how you can add Maven POM license using this plugin:

```groovy
apply plugin: 'maven-publish'

publishing.publications.withType(MavenPublication).configureEach {
  pom {
    licenses {
      license {
        name = githubRepositoryInfo.licenseFile.map { it.license.name }
        url = githubRepositoryInfo.licenseFile.map { it.url }
      }
    }
  }
}
```

# Loading data by using `githubRepositoryInfo` extension

This plugin creates `githubRepositoryInfo` extension that provides the following
[read-only](https://docs.gradle.org/current/javadoc/org/gradle/api/provider/HasConfigurableValue.html#disallowChanges())
[properties](https://docs.gradle.org/current/javadoc/org/gradle/api/provider/Property.html).

* `githubRepositoryInfo.repository` - provides information about the repository itself ([example](https://api.github.com/repos/remal-gradle-plugins/github-repository-info))
* `githubRepositoryInfo.licenseFile` - provides information about the repository license file ([example](https://api.github.com/repos/remal-gradle-plugins/github-repository-info/license))
* `githubRepositoryInfo.contributors` - provides a list of repository contributors ([example](https://api.github.com/repos/remal-gradle-plugins/github-repository-info/contributors))
* `githubRepositoryInfo.languages` - provides a map of programming languages used in the repository with their byte size ([example](https://api.github.com/repos/remal-gradle-plugins/github-repository-info/languages))

All these properties load data lazily.

# Configuration

For public GitHub repositories, the plugin should work without any additional configuration.

However, for private repositories or to increase the rate limits, you need to provide a GitHub token.

## GitHub token configuration for GitHub Actions

Configure `GITHUB_TOKEN` environment variable for your GitHub Actions job:

```yaml
- name: Run Gradle build
  env:
    GITHUB_TOKEN: ${{secrets.GITHUB_TOKEN}}
  run: ./gradlew build
```

`GITHUB_ACTIONS_TOKEN` environment variable can be used instead of `GITHUB_TOKEN`.

## GitHub token configuration for local development

Add `name.remal.github-repository-info.api.token` property to your `~/.gradle/gradle.properties` file:

```properties
name.remal.github-repository-info.api.token = <your github token here>
```

This file is in [the Gradle User Home directory](https://docs.gradle.org/current/userguide/directory_layout.html#dir:gradle_user_home),
so it won't be committed.
