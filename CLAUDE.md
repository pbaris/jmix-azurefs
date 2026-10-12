# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A Jmix add-on (`gr.netmechanics.jmix:azurefs-starter`) that provides Azure Blob Storage as a Jmix `FileStorage` implementation. Each add-on version tracks a Jmix platform version (see the compatibility table in `README.md`; update it and `CHANGELOG.md` when bumping `version` in `gradle.properties` and the Jmix BOM/plugin version in `build.gradle`).

## Commands

Gradle wrapper, Java 21 (matches CI):

```bash
./gradlew build                    # compile + test both modules
./gradlew :azurefs:test            # tests (JUnit Platform; the module currently has no test sources)
./gradlew :azurefs:test --tests 'gr.netmechanics.jmix.azurefs.SomeTest.method'   # single test
./gradlew publish                  # needs nexus_username/nexus_password props, else GitHub Packages env vars
```

Releases are published by `.github/workflows/release.yml` on GitHub release creation, passing the tag as `-Pversion`.

## Architecture

Two Gradle modules (build files are named `<module>.gradle`, set in `settings.gradle`; shared config lives in the root `build.gradle` `subprojects {}` block, including the Jmix Gradle plugin and BOM version):

- `azurefs` — the actual add-on (package `gr.netmechanics.jmix.azurefs`):
  - `AzureFileStorageConfiguration` is the `@JmixModule(dependsOn = CoreConfiguration.class)` entry point; it component-scans the package and enables `@ConfigurationPropertiesScan`.
  - `AzureFileStorage` (`@Component("azurefs_FileStorage")`) implements `io.jmix.core.FileStorage`. It holds the `BlobContainerClient` in an `AtomicReference`, built on `ApplicationStartedEvent` (creates the container, tolerating "already exists"). Blob keys are `yyyy/MM/dd/<uuid>[.ext]`. Two modes: default constructor reads `AzureFileStorageProperties` (`jmix.azurefs.*`) on every refresh; the 5-arg constructor pins explicit settings and ignores properties (used to register additional named storages).
  - `AzureFileStorageManagementFacade` is a JMX MBean (`jmix.azurefs:type=AzureFileStorage`) that triggers `refreshBlobContainerClient()` at runtime, optionally overriding connection settings per storage name.
- `azurefs-starter` — only a Spring Boot `@AutoConfiguration` that `@Import`s `AzureFileStorageConfiguration`, registered via `META-INF/spring`. Consumers depend on this module; it exposes `azurefs` as `api`.

`azurefs/src/main/resources/.../module.properties` is the Jmix module properties file (currently carries the Jmix 3.1 migration flag).

## OpenWolf

This project uses OpenWolf for context management. The always-on rules live in `.claude/rules/openwolf.md`; the hooks handle bookkeeping (anatomy index, memory log, read tracking) automatically.

For the full operating protocol (session handoff, memory discipline, bug logging), load the `openwolf` skill, or read `.wolf/OPENWOLF.md`. Regenerate the session handoff with `/handoff`.

## Git Conventions
- NEVER run `git commit` or `git push` without an explicit instruction from the user.
- NEVER add "Co-Authored-By" lines or attribution text to commits.
- Keep commit message lines under 90 characters

<!-- BEGIN jmix-agent-toolkit -->
## Jmix

This is a Jmix 3 application. Before writing or changing ANY file in it, read
the `jmix` skill and follow it. It maps the task to artifacts, routes each
artifact to the skill that governs it, and names the checks that close a task.
Your Jmix/Vaadin priors are not reliable here; the skills are.

Managed by the Jmix Agent Toolkit. Content between these markers is replaced on
re-install — put your own instructions outside them.
<!-- END jmix-agent-toolkit -->
