/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jakartaee.transform.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileFilter;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

/**
 * Validates that the import values for the transform rules are consistent between the different files
 */
public class RulesValidationTest {

    @Test
    public void validateVersionRules() throws Exception {
        StringBuilder errorMessage = new StringBuilder();
        File rulesDir = new File("rules/");
        File[] versionRuleFiles = rulesDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.getName().startsWith("jakarta-versions");
            }
        });
        for (File versionRuleFile : versionRuleFiles) {
            Properties rules = new Properties();
            try (FileInputStream fis = new FileInputStream(versionRuleFile)) {
                rules.load(fis);
            }

            for (String key : rules.stringPropertyNames()) {
                String value = rules.getProperty(key);
                if (!value.startsWith("[")) {
                    errorMessage.append("Version value is expected to start with a [ for pacakge ").append(key)
                            .append('\n');
                }
                int index = value.indexOf(';');
                if (index != -1) {
                    value = value.substring(0, index);
                }
                if (!value.endsWith(")")) {
                    errorMessage.append("Version value is expected to end with a ) for pacakge ").append(key)
                            .append(" in file ").append(versionRuleFile.getName()).append('\n');
                }
            }
        }
        if (errorMessage.length() != 0) {
            Assert.fail("Found transform rules with incorrectly formated values.\n" + errorMessage.toString());
        }
    }

    @Test
    public void validateEE9Rules() throws Exception {
        StringBuilder errorMessage = new StringBuilder();
        validateSingleVersionRules(errorMessage, "rules/jakarta-versions-ee9.properties",
                "rules/jakarta-versions-ee10.properties", "rules/jakarta-versions-ee11.properties");

        if (errorMessage.length() != 0) {
            Assert.fail("Found transform rules with incorrect values.\n" + errorMessage.toString());
        }
    }

    @Test
    public void validateEE10Rules() throws Exception {
        StringBuilder errorMessage = new StringBuilder();
        validateSingleVersionRules(errorMessage, "rules/jakarta-versions-ee10.properties",
                "rules/jakarta-versions-ee11.properties");

        if (errorMessage.length() != 0) {
            Assert.fail("Found transform rules with incorrect values.\n" + errorMessage.toString());
        }
    }

    @Test
    public void validateEE9PlusRules() throws Exception {
        StringBuilder errorMessage = new StringBuilder();
        validateComboRules(errorMessage, "rules/jakarta-versions.properties", "rules/jakarta-versions-ee9.properties",
                "rules/jakarta-versions-ee11.properties");

        if (errorMessage.length() != 0) {
            Assert.fail(
                    "Found transform rules with incorrect values.\nError could be in jakarta-versions.properties, ee9, and/or ee11 files\n"
                            + errorMessage.toString());
        }
    }

    @Test
    public void validateEE10PlusRules() throws Exception {
        StringBuilder errorMessage = new StringBuilder();
        validateComboRules(errorMessage, "rules/jakarta-versions-ee10plus.properties",
                "rules/jakarta-versions-ee10.properties", "rules/jakarta-versions-ee11.properties");
        if (errorMessage.length() != 0) {
            Assert.fail(
                    "Found transform rules with incorrect values.\nError could be in ee10plus, ee10, and/or ee11 files\n"
                            + errorMessage.toString());
        }
    }

    @Test
    public void validateEE9And10Rules() throws Exception {
        StringBuilder errorMessage = new StringBuilder();
        validateComboRules(errorMessage, "rules/jakarta-versions-ee9and10.properties",
                "rules/jakarta-versions-ee9.properties", "rules/jakarta-versions-ee10.properties");
        if (errorMessage.length() != 0) {
            Assert.fail(
                    "Found transform rules with incorrect values.\nError could be in ee9and10, ee9, and/or ee10 files\n"
                            + errorMessage.toString());
        }
    }

    /**
     * Determines if the ending import version for package is correct by examining later EE versions of the version rules. 
     * @param errorMessage
     * @param rulesFile
     * @param laterVersionFiles - the order needs to be in version order in order for the processing to be correct.
     * @throws Exception
     */
    private void validateSingleVersionRules(StringBuilder errorMessage, String rulesFile, String... laterVersionFiles)
            throws Exception {
        Properties rulesProps = new Properties();
        try (FileInputStream fis = new FileInputStream(rulesFile)) {
            rulesProps.load(fis);
        }

        Properties[] laterVersionRulesProps = new Properties[laterVersionFiles.length];

        int i = 0;
        for (String laterVersionFile : laterVersionFiles) {
            laterVersionRulesProps[i] = new Properties();
            try (FileInputStream fis = new FileInputStream(laterVersionFile)) {
                laterVersionRulesProps[i].load(fis);
            }
            i++;
        }
        
        for (String key : rulesProps.stringPropertyNames()) {
            String value = rulesProps.getProperty(key);
            int index = value.indexOf(";Export");
            if (index > -1) {
                value = value.substring(0, index);
            }

            for (Properties laterVersionRules : laterVersionRulesProps) {
                String nextVersionValue = laterVersionRules.getProperty(key);
                index = nextVersionValue.indexOf(";Export");
                if (index > -1) {
                    nextVersionValue = nextVersionValue.substring(0, index);
                }
                if (value.equals(nextVersionValue)) {
                    continue;
                }

                String nextVersionMinImport = nextVersionValue.substring(1, nextVersionValue.indexOf(','));
                String expectedEnding = ',' + nextVersionMinImport + ')';
                boolean includeAlt = false;
                if (!value.endsWith(expectedEnding)) {
                    if (nextVersionMinImport.endsWith(".0")) {
                        expectedEnding = ',' + nextVersionMinImport.substring(0, nextVersionMinImport.length()-2) + ')';
                        if (value.endsWith(expectedEnding)) {
                            break;
                        }
                        includeAlt = true;
                    }
                    errorMessage.append("Ending import range was expected to be ").append(nextVersionMinImport);
                    if (includeAlt) {
                        errorMessage.append(" or ").append(nextVersionMinImport, 0, nextVersionMinImport.length() - 2);
                    }
                    errorMessage.append(" for package ").append(key).append(", but the version range was ").append(value).append('\n');
                }
                break;
            }
        }
    }

    private void validateComboRules(StringBuilder errorMessage, String rulesFile, String minimumRulesFile,
            String maximumRulesFile) throws Exception {
        Properties rulesProps = new Properties();
        Properties minRulesProps = new Properties();
        Properties maxRulesProps = new Properties();
        try (FileInputStream fis = new FileInputStream(rulesFile)) {
            rulesProps.load(fis);
        }

        try (FileInputStream fis = new FileInputStream(minimumRulesFile)) {
            minRulesProps.load(fis);
        }

        try (FileInputStream fis = new FileInputStream(maximumRulesFile)) {
            maxRulesProps.load(fis);
        }

        for (String key : rulesProps.stringPropertyNames()) {
            String value = rulesProps.getProperty(key);
            int index = value.indexOf(";Export");
            if (index > -1) {
                value = value.substring(0, index);
            }

            String minValue = minRulesProps.getProperty(key);
            index = minValue.indexOf(";Export");
            if (index > -1) {
                minValue = minValue.substring(0, index);
            }

            String maxValue = maxRulesProps.getProperty(key);
            index = maxValue.indexOf(";Export");
            if (index > -1) {
                maxValue = maxValue.substring(0, index);
            }

            int minIndex = minValue.indexOf(',');
            int maxIndex = maxValue.indexOf(',');
            String expectedValue = minValue.substring(0, minIndex) + maxValue.substring(maxIndex);
            if (!value.equals(expectedValue)) {
                errorMessage.append("Expected import rules to be ").append(expectedValue).append(" for package ")
                        .append(key).append(", but was ").append(value).append('\n');
            }
        }
    }
}
