# Release and Tagging Process

This document describes the automated release process for the `sc` CLI project, which builds platform-specific installers using JPackage and publishes them via GitHub releases.

## Overview

The project uses:
- **Semver Gradle Plugin** for semantic versioning and git tag management
- **JReleaser** for automated releases and distribution
- **GitHub Actions** for CI/CD and multi-platform installer building
- **JPackage** for creating platform-specific installers (.deb, .rpm, .dmg, .pkg, .exe, .msi)

## Supported Platforms

- **Linux**: DEB and RPM packages (x86_64)
- **macOS**: DMG and PKG installers (x86_64 Intel, aarch_64 Apple Silicon)  
- **Windows**: EXE and MSI installers (x86_64)

## Release Types

Since this project is in heavy development, we use **ALPHA** releases by default:

- `v0.1.0-alpha.1` - First alpha release
- `v0.1.0-alpha.2` - Subsequent alpha versions
- `v0.1.0-beta.1` - Beta releases
- `v0.1.0-rc.1` - Release candidates
- `v0.1.0` - Final release

## Quick Start

### 1. Manual Release Creation

Use the provided Gradle tasks for local testing:

```bash
# Create an alpha release
./gradlew releaseAlpha

# Create a beta release  
./gradlew releaseBeta

# Create a release candidate
./gradlew releaseRC

# Create a final release
./gradlew releaseFinal
```

### 2. GitHub Actions Release

For automated multi-platform releases:

1. Go to **Actions** tab in GitHub
2. Select **Release** workflow
3. Click **Run workflow**
4. Choose:
   - **Stage**: `alpha`, `beta`, `rc`, or `final`
   - **Scope**: `auto`, `patch`, `minor`, or `major`

### 3. Tag-Based Release

Push a tag to trigger automatic release:

```bash
# Create and push a tag
git tag v0.1.0-alpha.1
git push origin v0.1.0-alpha.1
```

## Local Development

### Test JReleaser Configuration

```bash
# Dry-run to test configuration
./gradlew jreleaserConfig

# Test full release process (dry-run)
./gradlew jreleaserFullReleaseLocal
```

### Version Information

```bash
# Show current version
./gradlew printSemver

# Show version without building
./gradlew version --quiet
```

### Building Installers Locally

```bash
# Build JAR first
./gradlew bootJar

# Build platform-specific installer
./gradlew jreleaserAssemble --assembler=jpackage

# The installer will be in: build/jreleaser/assemble/sc/jpackage/
```

## Release Workflow Details

### 1. Build Phase
- Builds JAR files on all platforms
- Uses JPackage to create platform-specific installers
- Linux: .deb and .rpm packages
- macOS: .dmg and .pkg installers  
- Windows: .exe and .msi installers
- Caches build artifacts for faster subsequent builds

### 2. Release Phase
- Downloads installer artifacts from all platform builds
- Creates appropriate git tags using semver plugin
- Runs JReleaser to:
  - Create GitHub release
  - Upload installer packages
  - Generate changelog
  - Configure package managers (Homebrew, Scoop, Chocolatey)

## Configuration Files

### `jreleaser.yml`
Main JReleaser configuration defining:
- Project metadata
- Release settings
- Distribution artifacts
- Package manager configurations

### `build.gradle`
Contains:
- Semver plugin configuration
- JReleaser plugin setup
- Custom release tasks
- GraalVM native image settings

## Environment Variables

Required for GitHub Actions:
- `GITHUB_TOKEN` - Automatically provided by GitHub Actions
- Additional tokens may be needed for package managers

## Troubleshooting

### Common Issues

1. **Build failures on specific platforms**
   - Check the JPackage build logs
   - Verify Java version compatibility (requires JDK 16+)

2. **JReleaser configuration errors**
   - Run `./gradlew jreleaserConfig` to validate
   - Check JAR file paths match build outputs

3. **Version conflicts**
   - Ensure git tags follow semver format
   - Check for existing tags with same version

### Debugging

```bash
# Show detailed JReleaser configuration
./gradlew jreleaserConfig --full

# Show available JReleaser tasks
./gradlew tasks --group JReleaser

# Show semver information
./gradlew printSemver

# Test JPackage assembly
./gradlew jreleaserAssemble --assembler=jpackage

# Check git status and tags
git tag -l
git status
```

## Future Enhancements

- Add Linux ARM64 support when GitHub Actions runners are available
- Integrate with package managers (apt, yum, etc.)
- Add automatic dependency security scanning
- Implement signed releases with GPG

## References

- [JReleaser Documentation](https://jreleaser.org/)
- [Semver Gradle Plugin](https://semver-gradle-plugin.javiersc.com/)
- [GraalVM Native Image](https://www.graalvm.org/native-image/)
- [GitHub Actions](https://docs.github.com/en/actions)
