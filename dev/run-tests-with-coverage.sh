#!/bin/bash
###############################################################################
# Copyright (c) 2026 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     IBM Corporation - JaCoCo code coverage test execution script
###############################################################################

# Script to run Open Liberty tests with JaCoCo code coverage
# Usage: ./run-tests-with-coverage.sh [options]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
MODULE=""
CLEAN=false
OPEN_REPORT=false
AGGREGATE=false
PARALLEL=true

# Function to print usage
print_usage() {
    echo -e "${BLUE}Usage:${NC} $0 [options]"
    echo ""
    echo "Options:"
    echo "  -m, --module <name>     Run tests for a specific module only"
    echo "  -c, --clean             Clean build before running tests"
    echo "  -o, --open              Open HTML coverage report after generation"
    echo "  -a, --aggregate         Generate aggregate coverage report for all modules"
    echo "  -s, --sequential        Run tests sequentially (disable parallel execution)"
    echo "  -h, --help              Display this help message"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Run all tests with coverage"
    echo "  $0 -m com.ibm.ws.anno                # Run tests for specific module"
    echo "  $0 -c -a -o                          # Clean, run all tests, aggregate report, and open"
    echo "  $0 -m com.ibm.ws.anno -o             # Run module tests and open report"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -m|--module)
            MODULE="$2"
            shift 2
            ;;
        -c|--clean)
            CLEAN=true
            shift
            ;;
        -o|--open)
            OPEN_REPORT=true
            shift
            ;;
        -a|--aggregate)
            AGGREGATE=true
            shift
            ;;
        -s|--sequential)
            PARALLEL=false
            shift
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        *)
            echo -e "${RED}Error: Unknown option $1${NC}"
            print_usage
            exit 1
            ;;
    esac
done

# Print header
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Open Liberty Test Coverage Runner${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo -e "${RED}Error: gradlew not found in current directory${NC}"
    echo "Please run this script from the dev directory"
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

# Clean if requested
if [ "$CLEAN" = true ]; then
    echo -e "${YELLOW}Cleaning build...${NC}"
    ./gradlew clean
    echo -e "${GREEN}✓ Clean completed${NC}"
    echo ""
fi

# Set parallel flag
PARALLEL_FLAG=""
if [ "$PARALLEL" = false ]; then
    PARALLEL_FLAG="--no-parallel"
fi

# Run tests based on options
if [ -n "$MODULE" ]; then
    # Run tests for specific module
    echo -e "${YELLOW}Running tests with coverage for module: ${MODULE}${NC}"
    ./gradlew :${MODULE}:test :${MODULE}:jacocoTestReport $PARALLEL_FLAG
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✓ Tests completed successfully for ${MODULE}${NC}"
        
        # Check if report exists
        REPORT_PATH="${MODULE}/build/reports/jacoco/test/html/index.html"
        if [ -f "$REPORT_PATH" ]; then
            echo -e "${GREEN}✓ Coverage report generated${NC}"
            echo -e "  Report location: ${REPORT_PATH}"
            
            # Open report if requested
            if [ "$OPEN_REPORT" = true ]; then
                echo -e "${BLUE}Opening coverage report...${NC}"
                if [[ "$OSTYPE" == "darwin"* ]]; then
                    open "$REPORT_PATH"
                elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
                    xdg-open "$REPORT_PATH" 2>/dev/null || echo -e "${YELLOW}Please open manually: $REPORT_PATH${NC}"
                else
                    echo -e "${YELLOW}Please open manually: $REPORT_PATH${NC}"
                fi
            fi
        else
            echo -e "${YELLOW}⚠ Coverage report not found at expected location${NC}"
        fi
    else
        echo -e "${RED}✗ Tests failed for ${MODULE}${NC}"
        exit 1
    fi
    
elif [ "$AGGREGATE" = true ]; then
    # Run all tests and generate aggregate report
    echo -e "${YELLOW}Running all tests with coverage and generating aggregate report...${NC}"
    ./gradlew testWithCoverage $PARALLEL_FLAG
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✓ All tests completed successfully${NC}"
        echo -e "${GREEN}✓ Aggregate coverage report generated${NC}"
        
        REPORT_PATH="build/reports/jacoco/aggregate/html/index.html"
        echo -e "  Report location: ${REPORT_PATH}"
        
        # Open report if requested
        if [ "$OPEN_REPORT" = true ]; then
            echo -e "${BLUE}Opening aggregate coverage report...${NC}"
            if [[ "$OSTYPE" == "darwin"* ]]; then
                open "$REPORT_PATH"
            elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
                xdg-open "$REPORT_PATH" 2>/dev/null || echo -e "${YELLOW}Please open manually: $REPORT_PATH${NC}"
            else
                echo -e "${YELLOW}Please open manually: $REPORT_PATH${NC}"
            fi
        fi
    else
        echo -e "${RED}✗ Some tests failed${NC}"
        exit 1
    fi
    
else
    # Run all tests without aggregate report
    echo -e "${YELLOW}Running all tests with coverage...${NC}"
    ./gradlew test jacocoTestReport $PARALLEL_FLAG
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✓ All tests completed successfully${NC}"
        echo -e "${GREEN}✓ Individual coverage reports generated for each module${NC}"
        echo ""
        echo -e "${BLUE}To view coverage reports:${NC}"
        echo "  - Individual module reports: <module>/build/reports/jacoco/test/html/index.html"
        echo "  - Run with -a flag to generate aggregate report"
    else
        echo -e "${RED}✗ Some tests failed${NC}"
        exit 1
    fi
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Test execution completed${NC}"
echo -e "${BLUE}========================================${NC}"