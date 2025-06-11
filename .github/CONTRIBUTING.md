# Welcome to the contributing guide
Thank you for considering contributing to this project. We appreciate your time and effort. 
We have a few guidelines to help you get started.

WeaponMechanics leverages the following technologies:
  - [MechanicsCore](https://github.com/WeaponMechanics/MechanicsCore)
  - [Gradle](https://gradle.org/)
    - Handling modules, dependencies, and other build tasks 
  - [Kotlin](https://kotlinlang.org/) 
    - [Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html) for Gradle
    - Use `@NotNull` and `@Nullable` annotations for null safety
    - Use Kotlin for utility classes, and other appropriate classes
  - [paperweight-userdev](https://github.com/PaperMC/paperweight-test-plugin)
    - Automatically download and remaps server jars for 1.17.2+

## New contributor guide

To build the plugin for Spigot, run:
```shell
./gradlew shadowJar
```

The `.jar` file will be located in the `weaponmechanics-build/build/libs` directory. Note that
you will still need to have the `MechanicsCore` plugin installed on your server for WeaponMechanics to work.

## Commit Style
We use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) for our commit messages. Generally, this means:
- Use `fix: <description>` for bug fixes
- Use `feat: <description>` for new features

## Making a pull request

When making a pull request, GitHub Actions will automatically run the following
checks on your code:
```shell
./gradlew build
./gradlew spotlessCheck
```

If any of these checks fail, you will need to fix the issues before your pull
request can be merged. For "spotlessCheck", you can run `./gradlew spotlessApply`
to automatically address your code formatting issues.
