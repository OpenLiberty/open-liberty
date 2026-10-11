#!/bin/bash
###############################################################################
# Java Environment Setup for Open Liberty
# This script sets the correct Java versions for building and testing
###############################################################################

# Set JAVA_HOME to Java 17 (required for Gradle)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.0.13+11/Contents/Home

# Set JAVA_21_HOME (required by Open Liberty build)
export JAVA_21_HOME=/Library/Java/JavaVirtualMachines/jdk-21.0.5+11/Contents/Home

# Verify Java version
echo "JAVA_HOME set to: $JAVA_HOME"
echo "JAVA_21_HOME set to: $JAVA_21_HOME"
echo ""
echo "Current Java version:"
$JAVA_HOME/bin/java -version

echo ""
echo "✓ Java environment configured successfully!"
echo ""
echo "You can now run:"
echo "  ./run-tests-with-coverage.sh -m <module-name> -o"
echo "  ./run-tests-with-coverage.sh -a -o"
