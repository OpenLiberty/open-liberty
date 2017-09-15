/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.custom.junit.runner;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Adding this test rule to a fat bucket will repeat running all the test classes listed in FATSuite or FATSuiteLite twice,
 * Once with the server configurations already defined in "/publish" directory and once after all the *.xml files in "/publish"
 * are updated.
 * You can replace a feature in server configuration files with one feature or a list of features when repeating.
 * Look at the "com.ibm.ws.jmx.connector.client.rest.fat.FATSuite" as an example of how to use this test rule.
 */
public class RepeatAfterFeatureSubstitutionRule extends ExternalResource {
    private final String originalFeature;
    private final List<String> substituteFeatures;

    public RepeatAfterFeatureSubstitutionRule(String originalFeature, String substituteFeature) {
        ensureArgIsValid(originalFeature, "originalFeature");
        ensureArgIsValid(substituteFeature, "substituteFeature");

        this.originalFeature = originalFeature;
        this.substituteFeatures = Arrays.asList(substituteFeature);
    }

    public RepeatAfterFeatureSubstitutionRule(String originalFeature, List<String> substituteFeatures) {
        ensureArgIsValid(originalFeature, "originalFeature");
        ensureArgIsValid(substituteFeatures, "substituteFeatures");

        this.originalFeature = originalFeature;
        this.substituteFeatures = substituteFeatures;
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return new FeatureSubstitutionStatement(originalFeature, substituteFeatures, statement);
    }

    private void ensureArgIsValid(String value, String argName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Argument '" + argName + "' is null or empty.");
        }
    }

    private void ensureArgIsValid(List<String> values, String argName) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Argument '" + argName + "' is null or empty.");
        }

        for (String value : values) {
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException("Argument '" + argName + "' contains null or empty values.");
            }
        }
    }

    private static class FeatureSubstitutionStatement extends Statement {
        private static Class<?> logClass = FeatureSubstitutionStatement.class;

        private static String pathToAutoFVTTestFiles = "lib/LibertyFATTestFiles/";
        private static String pathToAutoFVTTestServers = "publish/servers/";
        private static final String featurePattern = "<feature>%s</feature>";

        private final Statement statement;
        private final String originalFeature;
        private final List<String> substituteFeatures;

        private FeatureSubstitutionStatement(String originalFeature, List<String> substituteFeatures, Statement statement) {
            this.statement = statement;
            this.originalFeature = originalFeature;
            this.substituteFeatures = substituteFeatures;
        }

        @Override
        public void evaluate() throws Throwable {
            statement.evaluate();

            // Find all server.xml in this test project
            File serverFolder = new File(pathToAutoFVTTestServers);
            File filesFolder = new File(pathToAutoFVTTestFiles);
            Set<File> serverConfigs = findFile(serverFolder, ".xml");
            serverConfigs.addAll(findFile(filesFolder, ".xml"));

            // change all the server.xml files
            assertTrue("There were no servers (*.xml) in " + serverFolder.getAbsolutePath() + " and in " + filesFolder.getAbsolutePath()
                       + ". If you see this failure, what you need to do is add a simple server to publish/servers which includes the feature you're trying to test.",
                       serverConfigs.size() > 0);

            String originalFeatureText = String.format(featurePattern, originalFeature);
            String substituteFeaturesText = formatFeatureString(substituteFeatures);

            replaceAll(serverConfigs, originalFeatureText, substituteFeaturesText);

            statement.evaluate();

            replaceAll(serverConfigs, substituteFeaturesText, originalFeatureText);
        }

        private static Set<File> findFile(File dir, String suffix) {
            HashSet<File> set = new HashSet<File>();

            File[] list = dir.listFiles();
            if (list != null) {
                for (File file : list) {
                    if (file.isDirectory()) {
                        set.addAll(findFile(file, suffix));
                    } else if (file.getName().endsWith(suffix)) {
                        set.add(file);
                    }
                }
            }

            Log.info(logClass, "findFile", "All the files: " + set.toString());

            return set;
        }

        private static void replaceAll(Set<File> files, String from, String to) throws Exception {
            for (File file : files) {
                replaceAll(file, from, to);
            }
        }

        private static void replaceAll(File file, String from, String to) throws Exception {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = null;
            StringBuilder stringBuilder = new StringBuilder();
            String lineSeparator = System.getProperty("line.separator");

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(lineSeparator);
            }

            FileWriter fw = new FileWriter(file.getPath());
            fw.write(stringBuilder.toString().toString().replaceAll(from, to));
            fw.close();
            reader.close();
        }

        private static String formatFeatureString(List<String> features) {
            StringBuilder sb = new StringBuilder();
            for (String feature : features) {
                sb.append(String.format(featurePattern, feature));
            }

            return sb.toString();
        }
    }
}