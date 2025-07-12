#!/bin/bash

# Test script to verify Java 22 installation in Docker
# This script can be used to test the installation in a running container

set -euo pipefail

# Color codes for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# Test functions
test_java_version() {
    echo -e "${BLUE}Testing Java version...${NC}"
    if java -version 2>&1 | grep -q "openjdk version \"22"; then
        echo -e "${GREEN}✓ Java 22 is installed and working${NC}"
        java -version
        return 0
    else
        echo -e "${RED}✗ Java 22 is not installed or not working${NC}"
        return 1
    fi
}

test_java_home() {
    echo -e "\n${BLUE}Testing JAVA_HOME...${NC}"
    if [[ -n "${JAVA_HOME:-}" && -d "${JAVA_HOME}" ]]; then
        echo -e "${GREEN}✓ JAVA_HOME is set correctly: ${JAVA_HOME}${NC}"
        return 0
    else
        echo -e "${RED}✗ JAVA_HOME is not set or invalid${NC}"
        return 1
    fi
}

test_javac() {
    echo -e "\n${BLUE}Testing Java compiler...${NC}"
    if command -v javac >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Java compiler (javac) is available${NC}"
        javac -version
        return 0
    else
        echo -e "${YELLOW}⚠ Java compiler (javac) is not available${NC}"
        return 1
    fi
}

test_sdk_command() {
    echo -e "\n${BLUE}Testing SDKMAN...${NC}"
    if command -v sdk >/dev/null 2>&1; then
        echo -e "${GREEN}✓ SDKMAN is available${NC}"
        sdk version
        return 0
    else
        echo -e "${YELLOW}⚠ SDKMAN is not available in current shell${NC}"
        echo "  Try running: source /etc/profile.d/java22.sh"
        return 1
    fi
}

test_simple_java_program() {
    echo -e "\n${BLUE}Testing Java compilation and execution...${NC}"
    
    # Create a simple test program
    cat > /tmp/HelloWorld.java << 'EOF'
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello from Java " + System.getProperty("java.version"));
        System.out.println("Java Home: " + System.getProperty("java.home"));
    }
}
EOF
    
    # Compile and run
    if javac /tmp/HelloWorld.java -d /tmp && java -cp /tmp HelloWorld; then
        echo -e "${GREEN}✓ Java compilation and execution successful${NC}"
        rm -f /tmp/HelloWorld.java /tmp/HelloWorld.class
        return 0
    else
        echo -e "${RED}✗ Java compilation or execution failed${NC}"
        rm -f /tmp/HelloWorld.java /tmp/HelloWorld.class
        return 1
    fi
}

# Source Java environment if not already available
if ! command -v java >/dev/null 2>&1; then
    echo -e "${YELLOW}Java not found in PATH, sourcing environment...${NC}"
    if [[ -f /etc/profile.d/java22.sh ]]; then
        # shellcheck source=/dev/null
        source /etc/profile.d/java22.sh
    fi
fi

# Run all tests
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   Java 22 Installation Test Suite    ${NC}"
echo -e "${BLUE}========================================${NC}\n"

tests_passed=0
total_tests=5

# Run tests
test_java_version && ((tests_passed++))
test_java_home && ((tests_passed++))
test_javac && ((tests_passed++))
test_sdk_command && ((tests_passed++))
test_simple_java_program && ((tests_passed++))

# Summary
echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}              Test Summary              ${NC}"
echo -e "${BLUE}========================================${NC}"

if [[ $tests_passed -eq $total_tests ]]; then
    echo -e "${GREEN}✓ All tests passed ($tests_passed/$total_tests)${NC}"
    echo -e "${GREEN}Java 22 installation is working correctly!${NC}"
    exit 0
elif [[ $tests_passed -ge 3 ]]; then
    echo -e "${YELLOW}⚠ Most tests passed ($tests_passed/$total_tests)${NC}"
    echo -e "${YELLOW}Java 22 installation is mostly working${NC}"
    exit 0
else
    echo -e "${RED}✗ Many tests failed ($tests_passed/$total_tests)${NC}"
    echo -e "${RED}Java 22 installation has issues${NC}"
    exit 1
fi
