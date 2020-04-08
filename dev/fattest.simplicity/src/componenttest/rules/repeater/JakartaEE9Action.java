/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.jakarta.transformer.JakartaTransformer;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FileUtils;

/**
 * Test repeat action that will do 2 things:
 * <ol>
 * <li>Invoke the Jakarta transformer on all war/ear files under the autoFVT/publish/ folder</li>
 * <li>Update all server.xml configs under the autoFVT/publish/ folder to use EE 9 features</li>
 * </ol>
 */
public class JakartaEE9Action extends FeatureReplacementAction {

    private static final Class<?> c = JakartaEE9Action.class;

    public static final String ID = "EE9_FEATURES";

    static final String[] EE9_FEATURES_ARRAY = { "jakartaee-9.0", "webProfile-9.0", "jakartaeeClient-9.0", "componenttest-2.0",
                                                 "beanValidation-3.0", "cdi-3.0", "concurrent-2.0",
                                                 "el-4.0", "javaMail-2.0", "jaxrs-3.0", "jaxrsClient-3.0",
                                                 "jpa-3.0", "jsonp-2.0", "jsonb-2.0",
                                                 "jsf-3.0", "jsp-3.0", "servlet-5.0"
    };

    public static final Set<String> EE9_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE9_FEATURES_ARRAY)));

//    private Function<Set<Path>, Set<Path>> selectPathsFunction;

    public JakartaEE9Action() {
        super(removeFeatures(), EE9_FEATURE_SET);
        withMinJavaLevel(8);
        forceAddFeatures(false);
        withID(ID);
    }

    @Override
    public JakartaEE9Action addFeature(String addFeature) {
        return (JakartaEE9Action) super.addFeature(addFeature);
    }

    @Override
    public JakartaEE9Action fullFATOnly() {
        return (JakartaEE9Action) super.fullFATOnly();
    }

    @Override
    public JakartaEE9Action withTestMode(TestMode mode) {
        return (JakartaEE9Action) super.withTestMode(mode);
    }

    @Override
    public JakartaEE9Action addFeatures(Set<String> addFeatures) {
        return (JakartaEE9Action) super.addFeatures(addFeatures);
    }

    @Override
    public JakartaEE9Action removeFeature(String removeFeature) {
        return (JakartaEE9Action) super.removeFeature(removeFeature);
    }

    @Override
    public JakartaEE9Action removeFeatures(Set<String> removeFeatures) {
        return (JakartaEE9Action) super.removeFeatures(removeFeatures);
    }

    @Override
    public JakartaEE9Action withMinJavaLevel(int javaLevel) {
        return (JakartaEE9Action) super.withMinJavaLevel(javaLevel);
    }

    @Override
    public JakartaEE9Action withID(String id) {
        return (JakartaEE9Action) super.withID(id);
    }

    @Override
    public JakartaEE9Action forServers(String... serverNames) {
        return (JakartaEE9Action) super.forServers(serverNames);
    }

    @Override
    public JakartaEE9Action forClients(String... clientNames) {
        return (JakartaEE9Action) super.forClients(clientNames);
    }

    @Override
    public String toString() {
        return "Set all features and applications to Jakarta EE9 compatibility";
    }

    private static Set<String> removeFeatures() {
        Set<String> removeFeatures = new HashSet<>(EE7FeatureReplacementAction.EE7_FEATURE_SET);
        removeFeatures.addAll(EE8FeatureReplacementAction.EE8_FEATURE_SET);
        removeFeatures.add("componenttest-1.0");
        return removeFeatures;
    }

    @Override
    public void setup() throws Exception {
        // Ensure all shared servers are stopped and applications are cleaned
        LibertyServerFactory.tidyAllKnownServers(SharedServer.class.getCanonicalName());
        LibertyServerFactory.recoverAllServers(SharedServer.class.getCanonicalName());
        for (LibertyServer server : LibertyServerFactory.getKnownLibertyServers(SharedServer.class.getCanonicalName())) {
            Path rootPath = Paths.get(server.getServerRoot());
            FileUtils.recursiveDelete(rootPath.toFile());
        }
        ShrinkHelper.cleanAllExportedArchives();

        // Transform server.xml's
        super.setup();
    }

    public static boolean isActive() {
        return ID.equals(RepeatTestFilter.CURRENT_REPEAT_ACTION);
    }

    /**
     * Invokes the Jakarta transformer on a given application (ear or war).
     * After completion, the transformed application will be available at the $appPath,
     * and the original application will be available at <server>/backup/
     *
     * @param appPath The application path to be transformed to Jakarta
     */
    public static void transformApp(Path appPath) {
        final String m = "transformApp";
        Log.info(c, m, "Transforming app: " + appPath);

        // Capture stdout/stderr streams
        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        System.setErr(ps);

        try {
            Class.forName("com.ibm.ws.jakarta.transformer.JakartaTransformer");
        } catch (Throwable e) {
            String mesg = "Unable to load the com.ibm.ws.jakarta.transformer.JakartaTransformer class. " +
                          "Did you remember to include 'addRequiredLibraries.dependsOn addJakartaTransformer' in the FATs build.gradle file?";
            Log.error(c, m, e, mesg);
            throw new RuntimeException(mesg, e);
        }

        Path outputPath = appPath.resolveSibling(appPath.getFileName() + ".jakarta");

        // Create backup directory
        Path serverPath = appPath.getParent().getParent();
        Path backupPath = serverPath.resolve("backup");
        try {
            if (!Files.exists(backupPath)) {
                Files.createDirectory(backupPath); // throws IOException
            }
        } catch (IOException e) {
            Log.info(c, m, "Unable to create backup directory.");
            Log.error(c, m, e);
            throw new RuntimeException(e);
        }

        String transformerRulesRoot = System.getProperty("user.dir") + "/autoFVT-templates/";
        try {
            // Invoke the jakarta transformer
            String[] args = new String[11];
            args[0] = appPath.toAbsolutePath().toString(); // input
            args[1] = outputPath.toAbsolutePath().toString(); // output
            args[2] = "-v"; // verbose
            args[3] = "-tr"; // override default properties in transformer jar
            args[4] = transformerRulesRoot + "jakarta-renames.properties";
            args[5] = "-ts";
            args[6] = transformerRulesRoot + "jakarta-selections.properties";
            args[7] = "-tv";
            args[8] = transformerRulesRoot + "jakarta-versions.properties";
            args[9] = "-tb";
            args[10] = transformerRulesRoot + "jakarta-bundles.properties";

            JakartaTransformer.main(args);

            if (outputPath.toFile().exists()) {

                Path backupAppPath = backupPath.resolve(appPath.getFileName());
                if (!Files.exists(backupAppPath)) {
                    Files.createFile(backupAppPath);
                }
                // move original to backup
                Files.move(appPath, backupAppPath, StandardCopyOption.REPLACE_EXISTING);
                // rename jakarta app to the original filename
                Files.move(outputPath, appPath);
            } else {
                throw new RuntimeException("Jakarta transformer failed for: " + appPath);
            }
        } catch (Exception e) {
            Log.info(c, m, "Unable to transform app at path: " + appPath);
            Log.error(c, m, e);
            throw new RuntimeException(e);
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
            Log.info(c, m, baos.toString());
            try {
                baos.close();
            } catch (IOException ignore) {
            }
        }
    }

}
