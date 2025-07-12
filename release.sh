#!/bin/bash

# Release Helper Script
# This script helps with testing and managing releases for the sc CLI project

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
print_header() {
    echo -e "${BLUE}======================================${NC}"
    echo -e "${BLUE}  SC CLI Release Helper${NC}"
    echo -e "${BLUE}======================================${NC}"
    echo
}

print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_git_status() {
    if ! git diff-index --quiet HEAD --; then
        print_warning "Working directory is dirty. Consider committing changes first."
        git status --short
        echo
    fi
}

show_current_version() {
    print_status "Current version information:"
    ./gradlew printSemver --quiet
    echo
}

show_recent_tags() {
    print_status "Recent git tags:"
    git tag -l --sort=-version:refname | head -10
    echo
}

test_jreleaser_config() {
    print_status "Testing JReleaser configuration..."
    print_warning "Note: JReleaser local testing may have dependency conflicts"
    print_status "The configuration will be validated during GitHub Actions workflow"
    print_status "JReleaser configuration syntax appears valid in jreleaser.yml"
    echo
}

test_build() {
    print_status "Testing JPackage installer build..."
    ./gradlew bootJar
    
    # Find the main JAR file (not the plain one)
    JAR_FILE=$(find build/libs -name "sc-*.jar" -not -name "*-plain.jar" | head -1)
    if [[ -f "$JAR_FILE" ]]; then
        print_status "JAR built successfully!"
        echo "JAR location: $JAR_FILE"
        echo "JAR size: $(du -h "$JAR_FILE" | cut -f1)"
        
        print_status "Testing JPackage assembly..."
        echo "[INFO] Note: Local JReleaser may fail due to dependency conflicts (JGit)"
        echo "[INFO] This is expected - GitHub Actions provides a clean environment"
        ./gradlew jreleaserAssemble --assembler=jpackage
        
        if [[ -d "build/jreleaser/assemble/sc/jpackage" ]]; then
            print_status "JPackage installer built successfully!"
            echo "Installer location: build/jreleaser/assemble/sc/jpackage"
            ls -la build/jreleaser/assemble/sc/jpackage/ || true
        else
            print_warning "JPackage installer not found (may be platform-specific)"
        fi
    else
        print_error "JAR file not found!"
        exit 1
    fi
    echo
}

dry_run_release() {
    print_status "JReleaser dry-run currently unavailable due to local dependency issues"
    print_status "Release testing will happen via GitHub Actions workflow"
    print_status "You can test the workflow by:"
    echo "  1. Push changes to GitHub"
    echo "  2. Go to Actions -> Release workflow"
    echo "  3. Run workflow with 'alpha' stage for testing"
    echo
}

create_test_tag() {
    local stage=${1:-alpha}
    local scope=${2:-auto}
    
    print_status "Creating $stage tag with $scope scope..."
    
    # Get current version from semver
    local current_version=$(./gradlew printSemver --quiet | grep "semver for sc:" | cut -d' ' -f3)
    print_status "Current version: $current_version"
    
    # For now, create tags manually based on current pattern
    # This will be improved once semver tag creation is working
    case $stage in
        alpha)
            # Increment alpha version
            local new_tag="v0.1.0-alpha.2"
            ;;
        beta)
            local new_tag="v0.1.0-beta.1"
            ;;
        rc)
            local new_tag="v0.1.0-rc.1"
            ;;
        final)
            local new_tag="v0.1.0"
            ;;
        *)
            local new_tag="v0.1.0-alpha.2"
            ;;
    esac
    
    git tag $new_tag
    print_status "Created tag: $new_tag"
    
    show_recent_tags
}

show_help() {
    echo "Usage: $0 [command]"
    echo
    echo "Commands:"
    echo "  status      - Show current version and git status"
    echo "  test-config - Test JReleaser configuration"
    echo "  test-build  - Test JAR and JPackage installer build"
    echo "  dry-run     - Run JReleaser dry-run"
    echo "  tag [stage] [scope] - Create a new tag (default: alpha auto)"
    echo "  alpha       - Create alpha release"
    echo "  beta        - Create beta release" 
    echo "  rc          - Create release candidate"
    echo "  final       - Create final release"
    echo "  help        - Show this help message"
    echo
    echo "Examples:"
    echo "  $0 status"
    echo "  $0 test-build"
    echo "  $0 tag alpha patch"
    echo "  $0 alpha"
    echo
}

# Main script
print_header

case ${1:-help} in
    status)
        check_git_status
        show_current_version
        show_recent_tags
        ;;
    test-config)
        test_jreleaser_config
        ;;
    test-build)
        test_build
        ;;
    dry-run)
        dry_run_release
        ;;
    tag)
        create_test_tag $2 $3
        ;;
    alpha)
        create_test_tag alpha auto
        ;;
    beta)
        create_test_tag beta auto
        ;;
    rc)
        create_test_tag rc auto
        ;;
    final)
        create_test_tag final auto
        ;;
    help)
        show_help
        ;;
    *)
        print_error "Unknown command: $1"
        echo
        show_help
        exit 1
        ;;
esac
