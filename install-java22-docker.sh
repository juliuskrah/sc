#!/bin/bash

# Java 22 Installation Script for Docker Ubuntu Environment
# This script automates the installation of Java 22 using SDKMAN in Docker containers
# Optimized for non-interactive, automated execution in sandbox environments

set -euo pipefail

# Color codes for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# Configuration
readonly JAVA_VERSION="22-graalce"
readonly SDKMAN_DIR="${HOME}/.sdkman"
readonly LOG_FILE="/tmp/java22-install.log"

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "${LOG_FILE}"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "${LOG_FILE}"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "${LOG_FILE}"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "${LOG_FILE}"
}

# Error handling
cleanup() {
    local exit_code=$?
    if [[ $exit_code -ne 0 ]]; then
        log_error "Installation failed with exit code $exit_code"
        log_error "Check log file at: ${LOG_FILE}"
    fi
    exit $exit_code
}

trap cleanup EXIT

# Check if running as root or with sudo
check_privileges() {
    if [[ $EUID -eq 0 ]]; then
        log_info "Running with root privileges"
        return 0
    elif command -v sudo >/dev/null 2>&1; then
        log_info "Running with sudo available"
        return 0
    else
        log_error "This script requires root privileges or sudo access"
        exit 1
    fi
}

# Install system dependencies
install_dependencies() {
    log_info "Installing system dependencies..."
    
    # Update package list
    if [[ $EUID -eq 0 ]]; then
        apt-get update -qq
        apt-get install -y curl zip unzip ca-certificates
    else
        sudo apt-get update -qq
        sudo apt-get install -y curl zip unzip ca-certificates
    fi
    
    log_success "System dependencies installed successfully"
}

# Check if SDKMAN is already installed
is_sdkman_installed() {
    [[ -d "${SDKMAN_DIR}" && -f "${SDKMAN_DIR}/bin/sdkman-init.sh" ]]
}

# Install SDKMAN
install_sdkman() {
    if is_sdkman_installed; then
        log_info "SDKMAN already installed at ${SDKMAN_DIR}"
        return 0
    fi
    
    log_info "Installing SDKMAN..."
    
    # Download and install SDKMAN
    curl -s "https://get.sdkman.io" | bash
    
    # Verify installation
    if [[ ! -f "${SDKMAN_DIR}/bin/sdkman-init.sh" ]]; then
        log_error "SDKMAN installation failed"
        exit 1
    fi
    
    log_success "SDKMAN installed successfully"
}

# Source SDKMAN environment
source_sdkman() {
    log_info "Sourcing SDKMAN environment..."
    
    # shellcheck source=/dev/null
    source "${SDKMAN_DIR}/bin/sdkman-init.sh"
    
    # Verify SDKMAN is available
    if ! command -v sdk >/dev/null 2>&1; then
        log_error "SDKMAN not available after sourcing"
        exit 1
    fi
    
    log_success "SDKMAN environment sourced successfully"
}

# Check if Java version is already installed
is_java_installed() {
    [[ -d "${SDKMAN_DIR}/candidates/java/${JAVA_VERSION}" ]]
}

# Install Java 22
install_java22() {
    if is_java_installed; then
        log_info "Java ${JAVA_VERSION} already installed"
        sdk default java "${JAVA_VERSION}"
        return 0
    fi
    
    log_info "Installing Java ${JAVA_VERSION}..."

    # Install Java 22 (GraalVM distribution)
    sdk install java "${JAVA_VERSION}" < /dev/null
    
    # Set as default Java version
    sdk default java "${JAVA_VERSION}"
    
    log_success "Java ${JAVA_VERSION} installed and set as default"
}

# Configure environment variables
configure_environment() {
    log_info "Configuring environment variables..."
    
    # Set JAVA_HOME
    export JAVA_HOME="${SDKMAN_DIR}/candidates/java/current"
    
    # Update PATH
    export PATH="${JAVA_HOME}/bin:${PATH}"
    
    # Create environment configuration for Docker
    cat > /etc/environment << EOF
JAVA_HOME=${JAVA_HOME}
SDKMAN_DIR=${SDKMAN_DIR}
PATH=${PATH}
EOF
    
    # Add to global bash profile for all users
    cat > /etc/profile.d/java22.sh << 'EOF'
# Java 22 Environment Configuration
export SDKMAN_DIR="/root/.sdkman"
[[ -s "$SDKMAN_DIR/bin/sdkman-init.sh" ]] && source "$SDKMAN_DIR/bin/sdkman-init.sh"
export JAVA_HOME="$SDKMAN_DIR/candidates/java/current"
export PATH="$JAVA_HOME/bin:$PATH"
EOF
    
    # Make the profile script executable
    chmod +x /etc/profile.d/java22.sh
    
    log_success "Environment variables configured"
}

# Verify Java installation
verify_installation() {
    log_info "Verifying Java installation..."
    
    # Check Java version
    if java -version 2>&1 | grep -q "openjdk version \"22"; then
        log_success "Java 22 verification successful"
        java -version 2>&1 | head -3 | while read -r line; do
            log_info "  $line"
        done
    else
        log_error "Java 22 verification failed"
        exit 1
    fi
    
    # Check JAVA_HOME
    if [[ -n "${JAVA_HOME:-}" && -d "${JAVA_HOME}" ]]; then
        log_success "JAVA_HOME is set correctly: ${JAVA_HOME}"
    else
        log_error "JAVA_HOME is not set correctly"
        exit 1
    fi
    
    # Check if javac is available
    if command -v javac >/dev/null 2>&1; then
        log_success "Java compiler (javac) is available"
    else
        log_warning "Java compiler (javac) is not available"
    fi
}

# Cleanup temporary files
cleanup_temp_files() {
    log_info "Cleaning up temporary files..."
    
    # Remove SDKMAN cache to reduce image size (optional)
    if [[ -d "${SDKMAN_DIR}/archives" ]]; then
        rm -rf "${SDKMAN_DIR}/archives"/*
        log_info "SDKMAN archives cleaned"
    fi
    
    # Clean apt cache if running as root
    if [[ $EUID -eq 0 ]]; then
        apt-get clean
        rm -rf /var/lib/apt/lists/*
        log_info "APT cache cleaned"
    fi
}

# Main installation function
main() {
    log_info "Starting Java 22 installation for Docker environment"
    log_info "Log file: ${LOG_FILE}"
    
    # Check system requirements
    check_privileges
    
    # Install dependencies
    install_dependencies
    
    # Install SDKMAN
    install_sdkman
    
    # Source SDKMAN
    source_sdkman
    
    # Install Java 22
    install_java22
    
    # Configure environment
    configure_environment
    
    # Verify installation
    verify_installation
    
    # Cleanup
    cleanup_temp_files
    
    log_success "Java 22 installation completed successfully!"
    log_info "To use Java 22 in new shells, run: source /etc/profile.d/java22.sh"
    log_info "Or restart your container/terminal session"
}

# Run main function
main "$@"
