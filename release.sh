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
    ./gradlew jreleaserConfig --quiet
    print_status "JReleaser configuration is valid!"
    echo
}

test_build() {
    print_status "Testing native image build..."
    ./gradlew nativeCompile
    
    if [[ -f "build/native/nativeCompile/sc" ]]; then
        print_status "Native binary built successfully!"
        echo "Binary location: build/native/nativeCompile/sc"
        echo "Binary size: $(du -h build/native/nativeCompile/sc | cut -f1)"
    elif [[ -f "build/native/nativeCompile/sc.exe" ]]; then
        print_status "Native binary built successfully!"
        echo "Binary location: build/native/nativeCompile/sc.exe"
        echo "Binary size: $(du -h build/native/nativeCompile/sc.exe | cut -f1)"
    else
        print_error "Native binary not found!"
        exit 1
    fi
    echo
}

dry_run_release() {
    print_status "Running JReleaser dry-run..."
    ./gradlew jreleaserFullRelease --dry-run
    print_status "Dry-run completed successfully!"
    echo
}

create_test_tag() {
    local stage=${1:-alpha}
    local scope=${2:-auto}
    
    print_status "Creating $stage tag with $scope scope..."
    ./gradlew createSemverTag -Psemver.stage=$stage -Psemver.scope=$scope
    
    print_status "New tag created!"
    show_recent_tags
}

show_help() {
    echo "Usage: $0 [command]"
    echo
    echo "Commands:"
    echo "  status      - Show current version and git status"
    echo "  test-config - Test JReleaser configuration"
    echo "  test-build  - Test native image build"
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
