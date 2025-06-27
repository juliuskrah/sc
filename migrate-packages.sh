#!/bin/bash

# Package Migration Script for sc CLI
# This script helps migrate the package structure from org.simplecommerce.ai.commerce to org.sc.ai.cli

set -e

echo "🚀 sc CLI Package Migration"
echo "============================================"
echo ""

# Function to check if git working directory is clean
check_git_status() {
    if ! git diff-index --quiet HEAD --; then
        echo "⚠️  Warning: You have uncommitted changes in your git working directory."
        read -p "Do you want to continue anyway? (y/N): " -n 1 -r
        echo ""
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "❌ Migration cancelled. Please commit or stash your changes first."
            exit 1
        fi
    fi
}

# Function to run preview
preview_migration() {
    echo "📋 Previewing migration changes..."
    echo ""
    ./gradlew previewPackageMigration
    echo ""
    read -p "Do you want to proceed with the migration? (y/N): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Migration cancelled."
        exit 0
    fi
}

# Function to run the migration
run_migration() {
    echo "🔄 Running package migration..."
    echo ""
    ./gradlew migratePackages
    echo ""
    echo "✅ Migration completed!"
}

# Function to validate the migration
validate_migration() {
    echo "🔍 Validating migration..."
    echo ""
    
    echo "📦 Cleaning and building project..."
    if ./gradlew clean build; then
        echo "✅ Build successful!"
    else
        echo "❌ Build failed! Please check the errors above."
        return 1
    fi
    
    echo ""
    echo "🧪 Running tests..."
    if ./gradlew test; then
        echo "✅ Tests passed!"
    else
        echo "❌ Tests failed! Please check the errors above."
        return 1
    fi
    
    echo ""
    echo "📚 Generating documentation..."
    if ./gradlew generateDocs; then
        echo "✅ Documentation generation successful!"
    else
        echo "⚠️  Documentation generation failed, but this might be expected."
    fi
}

# Function to show post-migration steps
show_post_migration_steps() {
    echo ""
    echo "🎉 Migration completed successfully!"
    echo ""
    echo "📝 Next steps:"
    echo "1. Review the changes: git diff"
    echo "2. Test your application: ./gradlew bootRun"
    echo "3. Update any documentation that references old package names"
    echo "4. Update external references (CI/CD, Docker files, etc.)"
    echo "5. Commit the changes: git add . && git commit -m 'Migrate package structure to org.sc.ai.cli'"
    echo ""
    echo "📖 For more information, see PACKAGE_MIGRATION.md"
}

# Main execution
main() {
    echo "Checking prerequisites..."
    
    # Check if we're in a git repository
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        echo "❌ This script should be run from within a git repository."
        exit 1
    fi
    
    # Check git status
    check_git_status
    
    # Check if gradlew exists
    if [[ ! -f "./gradlew" ]]; then
        echo "❌ gradlew not found. Please run this script from the project root."
        exit 1
    fi
    
    # Check if rewrite.yml exists
    if [[ ! -f "rewrite.yml" ]]; then
        echo "❌ rewrite.yml not found. Please ensure the OpenRewrite recipe is configured."
        exit 1
    fi
    
    echo "✅ Prerequisites check passed!"
    echo ""
    
    # Run preview
    preview_migration
    
    # Run migration
    run_migration
    
    # Validate migration
    if validate_migration; then
        show_post_migration_steps
    else
        echo ""
        echo "⚠️  Migration completed but validation failed."
        echo "Please review the errors above and fix any issues manually."
        echo "You can re-run validation with: ./gradlew clean build test"
    fi
}

# Run main function
main "$@"
