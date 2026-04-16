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
#     IBM Corporation - JaCoCo coverage report generation script
###############################################################################

# Script to generate JaCoCo coverage reports from existing test execution data
# Usage: ./generate-coverage-report.sh [options]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
MODULE=""
AGGREGATE=false
OPEN_REPORT=false
FORMAT="html"
OUTPUT_DIR=""

# Function to print usage
print_usage() {
    echo -e "${BLUE}Usage:${NC} $0 [options]"
    echo ""
    echo "Options:"
    echo "  -m, --module <name>     Generate report for a specific module"
    echo "  -a, --aggregate         Generate aggregate report for all modules"
    echo "  -f, --format <type>     Report format: html, xml, csv, or all (default: html)"
    echo "  -o, --open              Open HTML report after generation"
    echo "  -d, --output-dir <dir>  Custom output directory for reports"
    echo "  -h, --help              Display this help message"
    echo ""
    echo "Examples:"
    echo "  $0 -a -o                              # Generate aggregate HTML report and open"
    echo "  $0 -m com.ibm.ws.anno -o             # Generate report for specific module"
    echo "  $0 -a -f all                         # Generate all report formats"
    echo "  $0 -a -f xml -d /tmp/coverage        # Generate XML report to custom directory"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -m|--module)
            MODULE="$2"
            shift 2
            ;;
        -a|--aggregate)
            AGGREGATE=true
            shift
            ;;
        -f|--format)
            FORMAT="$2"
            shift 2
            ;;
        -o|--open)
            OPEN_REPORT=true
            shift
            ;;
        -d|--output-dir)
            OUTPUT_DIR="$2"
            shift 2
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

# Validate format
if [[ ! "$FORMAT" =~ ^(html|xml|csv|all)$ ]]; then
    echo -e "${RED}Error: Invalid format '$FORMAT'. Must be html, xml, csv, or all${NC}"
    exit 1
fi

# Print header
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}JaCoCo Coverage Report Generator${NC}"
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

# Function to check if coverage data exists
check_coverage_data() {
    local module=$1
    local exec_file="${module}/build/jacoco/test.exec"
    
    if [ ! -f "$exec_file" ]; then
        echo -e "${YELLOW}⚠ Warning: No coverage data found for ${module}${NC}"
        echo -e "  Expected file: ${exec_file}"
        echo -e "  Run tests first with: ./run-tests-with-coverage.sh"
        return 1
    fi
    return 0
}

# Function to open report
open_report_file() {
    local report_path=$1
    
    if [ -f "$report_path" ]; then
        echo -e "${BLUE}Opening coverage report...${NC}"
        if [[ "$OSTYPE" == "darwin"* ]]; then
            open "$report_path"
        elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
            xdg-open "$report_path" 2>/dev/null || echo -e "${YELLOW}Please open manually: $report_path${NC}"
        else
            echo -e "${YELLOW}Please open manually: $report_path${NC}"
        fi
    else
        echo -e "${YELLOW}⚠ HTML report not found at: $report_path${NC}"
    fi
}

# Generate report based on options
if [ -n "$MODULE" ]; then
    # Generate report for specific module
    echo -e "${YELLOW}Generating coverage report for module: ${MODULE}${NC}"
    
    if ! check_coverage_data "$MODULE"; then
        exit 1
    fi
    
    ./gradlew :${MODULE}:jacocoTestReport
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✓ Coverage report generated for ${MODULE}${NC}"
        
        REPORT_BASE="${MODULE}/build/reports/jacoco/test"
        echo -e "${BLUE}Report locations:${NC}"
        
        if [[ "$FORMAT" == "html" || "$FORMAT" == "all" ]]; then
            echo -e "  HTML: ${REPORT_BASE}/html/index.html"
            if [ "$OPEN_REPORT" = true ]; then
                open_report_file "${REPORT_BASE}/html/index.html"
            fi
        fi
        
        if [[ "$FORMAT" == "xml" || "$FORMAT" == "all" ]]; then
            echo -e "  XML:  ${REPORT_BASE}/jacocoTestReport.xml"
        fi
        
        if [[ "$FORMAT" == "csv" || "$FORMAT" == "all" ]]; then
            echo -e "  CSV:  ${REPORT_BASE}/jacocoTestReport.csv"
        fi
    else
        echo -e "${RED}✗ Failed to generate coverage report${NC}"
        exit 1
    fi
    
elif [ "$AGGREGATE" = true ]; then
    # Generate aggregate report
    echo -e "${YELLOW}Generating aggregate coverage report for all modules...${NC}"
    
    # Check if any coverage data exists
    COVERAGE_DATA_FOUND=false
    for exec_file in */build/jacoco/test.exec; do
        if [ -f "$exec_file" ]; then
            COVERAGE_DATA_FOUND=true
            break
        fi
    done
    
    if [ "$COVERAGE_DATA_FOUND" = false ]; then
        echo -e "${RED}Error: No coverage data found for any module${NC}"
        echo -e "Run tests first with: ./run-tests-with-coverage.sh -a"
        exit 1
    fi
    
    ./gradlew jacocoRootReport
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✓ Aggregate coverage report generated${NC}"
        
        REPORT_BASE="build/reports/jacoco/aggregate"
        echo -e "${BLUE}Report locations:${NC}"
        
        if [[ "$FORMAT" == "html" || "$FORMAT" == "all" ]]; then
            echo -e "  HTML: ${REPORT_BASE}/html/index.html"
            if [ "$OPEN_REPORT" = true ]; then
                open_report_file "${REPORT_BASE}/html/index.html"
            fi
        fi
        
        if [[ "$FORMAT" == "xml" || "$FORMAT" == "all" ]]; then
            echo -e "  XML:  ${REPORT_BASE}/jacocoAggregateReport.xml"
        fi
        
        if [[ "$FORMAT" == "csv" || "$FORMAT" == "all" ]]; then
            echo -e "  CSV:  ${REPORT_BASE}/jacocoAggregateReport.csv"
        fi
        
        # Display coverage summary if available
        if [ -f "${REPORT_BASE}/jacocoAggregateReport.csv" ]; then
            echo ""
            echo -e "${BLUE}Coverage Summary:${NC}"
            echo -e "${BLUE}----------------------------------------${NC}"
            
            # Parse CSV for summary (skip header, sum up totals)
            awk -F',' 'NR>1 {
                missed_instructions += $4
                covered_instructions += $5
                missed_branches += $6
                covered_branches += $7
            }
            END {
                total_instructions = missed_instructions + covered_instructions
                total_branches = missed_branches + covered_branches
                
                if (total_instructions > 0) {
                    instruction_coverage = (covered_instructions / total_instructions) * 100
                    printf "  Instruction Coverage: %.2f%% (%d/%d)\n", instruction_coverage, covered_instructions, total_instructions
                }
                
                if (total_branches > 0) {
                    branch_coverage = (covered_branches / total_branches) * 100
                    printf "  Branch Coverage:      %.2f%% (%d/%d)\n", branch_coverage, covered_branches, total_branches
                }
            }' "${REPORT_BASE}/jacocoAggregateReport.csv"
            
            echo -e "${BLUE}----------------------------------------${NC}"
        fi
    else
        echo -e "${RED}✗ Failed to generate aggregate coverage report${NC}"
        exit 1
    fi
    
else
    echo -e "${RED}Error: Must specify either -m <module> or -a (aggregate)${NC}"
    print_usage
    exit 1
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Report generation completed${NC}"
echo -e "${BLUE}========================================${NC}"
