# Install Java

Use the `install-java22-docker.sh` script to install Java

```bash
# Make the script executable
chmod +x ./install-java22-docker.sh

# Run the installation
./install-java22-docker.sh

# Test the installation (optional)
./test-java22-installation.sh
```

## Manual Installation (Alternative)

If you prefer manual installation using SDKMAN:

```bash
# Install SDKMAN (if not already installed)
curl -s "https://get.sdkman.io" | bash
source ~/.sdkman/bin/sdkman-init.sh

# Install Java 22
sdk install java 22-graalce  

# Set as default
sdk default java 22-graalce

# Verify installation
java -version
```

## Verify java installation

After installation, verify Java 22 is properly configured:

```bash
java -version
echo $JAVA_HOME
```

The output should show Java version 22.x.x.

# Install dependencies

```bash
./gradlew dependencies
```

# General Instructions

Use the instructions from [copilot-instructions](./.github/copilot-instructions.md) to set up your environment.
