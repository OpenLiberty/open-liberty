/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.transformer.jakarta.JakartaTransformer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.SharedServer;

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

    // Point-in-time list of enabled JakartaEE9 features.
    // This list is of only the currently enabled features.
    //
    // FAT tests use a mix of enabled features and not yet enabled
    // features, which is necessary for the FATs to run.

    static final String[] EE9_FEATURES_ARRAY = {
                                                 "appClientSupport-2.0",
                                                 "jakartaee-9.0",
                                                 "webProfile-9.0",
                                                 "jakartaeeClient-9.0",
                                                 "componenttest-2.0", // replaces "componenttest-1.0"
                                                 "txtest-2.0",
                                                 "appAuthentication-2.0",
                                                 "appAuthorization-2.0",
                                                 "appSecurity-4.0",
                                                 "batch-2.0",
                                                 "batchManagement-2.0",
                                                 "beanValidation-3.0",
                                                 "cdi-3.0",
                                                 "concurrent-2.0",
                                                 "connectors-2.0",
                                                 "connectorsInboundSecurity-2.0",
                                                 "expressionLanguage-4.0",
                                                 "enterpriseBeans-4.0",
                                                 "enterpriseBeansHome-4.0",
                                                 "enterpriseBeansLite-4.0",
                                                 "enterpriseBeansPersistentTimer-4.0",
                                                 "enterpriseBeansRemote-4.0",
                                                 "enterpriseBeansTest-2.0",
                                                 "mail-2.0",
                                                 "persistence-3.0",
                                                 "persistenceContainer-3.0",
                                                 "jsonp-2.0",
                                                 "jsonb-2.0",
                                                 "jsonpContainer-2.0",
                                                 "jsonbContainer-2.0",
                                                 "faces-3.0",
                                                 "facesContainer-3.0",
                                                 "pages-3.0",
                                                 "managedBeans-2.0",
                                                 "mdb-4.0",
                                                 "messaging-3.0",
                                                 "messagingClient-3.0",
                                                 "messagingServer-3.0",
                                                 "messagingSecurity-3.0",
                                                 "restfulWS-3.0",
                                                 "restfulWSClient-3.0",
                                                 "servlet-5.0",
                                                 "websocket-2.0",
                                                 "xmlBinding-3.0",
                                                 "xmlWS-3.0"
    };

    public static final Set<String> EE9_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE9_FEATURES_ARRAY)));

    public JakartaEE9Action() {
        // Remove the EE7 and EE8 features; replace them with the EE9 features
        super(EE9_FEATURE_SET);
        removeFeatures(EE6FeatureReplacementAction.EE6_FEATURE_SET);
        removeFeatures(EE7FeatureReplacementAction.EE7_FEATURE_SET);
        removeFeatures(EE8FeatureReplacementAction.EE8_FEATURE_SET);
        forceAddFeatures(false);
        withID(ID);
    }

    @Override
    public String toString() {
        return "JakartaEE9 FAT repeat action";
    }

    //

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

    //

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
        return RepeatTestFilter.isRepeatActionActive(ID);
    }

    /**
     * Invoke the Jakarta transformer on an application (ear or war or jar).
     *
     * A backup of the original application is placed under "&lt;server&gt;/backup".
     * ".jakarta" is appended to name the initially transformed application. However,
     * that application is renamed to the initial application name.
     *
     * @param appPath The application path to be transformed to Jakarta
     */
    public static void transformApp(Path appPath) {
        transformApp(appPath, null);
    }

    /**
     * Invoke the Jakarta transformer on an application (ear or war or jar).
     * to create a new transformed copy.
     *
     * If the destination Path is null, the application is transformed into
     * the same file as the source. A backup of the original application is placed
     * under "&lt;server&gt;/backup". The extension ".jakarta" is appended to
     * name the initially transformed application. However,
     * that application is renamed to the initial application name.
     *
     * @param appPath    The application path of file to be transformed to Jakarta
     * @param newAppPath The application path of the transformed file (or <code>null<code>)
     */
    public static void transformApp(Path appPath, Path newAppPath) {
        final String m = "transformApp";
        Log.info(c, m, "Transforming app: " + appPath);

        // Setup file output stream and only keep if we fail
        FileOutputStream fos = null;
        File outputLog = new File("results/transformer_" + appPath.getFileName() + ".log");
        try {
            fos = new FileOutputStream(outputLog);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

        PrintStream ps = new PrintStream(fos);
        System.setOut(ps);
        System.setErr(ps);

        try {
            Class.forName("org.eclipse.transformer.jakarta.JakartaTransformer");
        } catch (Throwable e) {
            String mesg = "Unable to load the org.eclipse.transformer.jakarta.JakartaTransformer class. " +
                          "Did you remember to include 'addRequiredLibraries.dependsOn addJakartaTransformer' in the FATs build.gradle file?";
            Log.error(c, m, e, mesg);
            throw new RuntimeException(mesg, e);
        }

        Path outputPath;
        Path backupPath = null;
        if (newAppPath == null) {
            outputPath = appPath.resolveSibling(appPath.getFileName() + ".jakarta");

            backupPath = appPath.getParent().getParent().resolve("backup");
            try {
                if (!Files.exists(backupPath)) {
                    Files.createDirectory(backupPath); // throws IOException
                }
            } catch (IOException e) {
                Log.info(c, m, "Unable to create backup directory.");
                Log.error(c, m, e);
                throw new RuntimeException(e);
            }
        } else {
            outputPath = newAppPath;
        }

        // The rules are copied from 'open-liberty/dev/wlp-jakartaee-transform/rules' to
        // the user 'autoFVT-templates' folder.
        //
        //   jakarta-selections.properties
        //   jakarta-renames.properties
        //   jakarta-versions.properties
        //   jakarta-bundles.properties
        //   jakarta-direct.properties
        //   jakarta-xml-master.properties
        //   (other xml properties files as referenced by 'jakarta-xml-master.properties'

        String transformerRulesRoot = System.getProperty("user.dir") + "/autoFVT-templates/";
        try {
            // Invoke the jakarta transformer
            String[] args = new String[15];

            args[0] = appPath.toAbsolutePath().toString(); // input
            args[1] = outputPath.toAbsolutePath().toString(); // output

            args[2] = "-q"; // quiet output

            // override jakarta default properties, which are
            // packaged in the transformer jar
            args[3] = "-tr"; // package-renames
            args[4] = transformerRulesRoot + "jakarta-renames.properties";
            args[5] = "-ts"; // file selections and omissions
            args[6] = transformerRulesRoot + "jakarta-selections.properties";
            args[7] = "-tv"; // package version updates
            args[8] = transformerRulesRoot + "jakarta-versions.properties";
            args[9] = "-tb"; // bundle identity updates
            args[10] = transformerRulesRoot + "jakarta-bundles.properties";
            args[11] = "-td"; // exact java string constant updates
            args[12] = transformerRulesRoot + "jakarta-direct.properties";
            args[13] = "-tf"; // master xml subsitution file
            args[14] = transformerRulesRoot + "jakarta-xml-master.properties";

            // Note the use of 'com.ibm.ws.JakartaTransformer'.
            // 'org.eclipse.transformer.Transformer' might also be used instead.

            JakartaTransformer.main(args);

            if (outputPath.toFile().exists()) {
                if (backupPath != null) {
                    Path backupAppPath = backupPath.resolve(appPath.getFileName());

                    /*
                     * Move original to backup.
                     *
                     * Don't use Files.move, b/c it can lead to:
                     *
                     * java.nio.file.FileSystemException: The process cannot access the
                     * file because it is being used by another process.
                     */
                    FileUtils.copyDirectory(appPath.toFile(), backupAppPath.toFile());
                    FileUtils.recursiveDelete(appPath.toFile());

                    /*
                     * Rename jakarta app to the original filename
                     */
                    FileUtils.copyDirectory(outputPath.toFile(), appPath.toFile());
                    FileUtils.recursiveDelete(outputPath.toFile());
                }
            } else {
                throw new RuntimeException("Jakarta transformer failed for: " + appPath);
            }
            //At this point the transformer was successful, delete output
            if (outputLog.exists()) {
                outputLog.delete();
            }
        } catch (Exception e) {
            Log.info(c, m, "Unable to transform app at path: " + appPath);
            Log.error(c, m, e);
            throw new RuntimeException(e);
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
            }
            Log.info(c, m, "Transforming complete app: " + outputPath);
        }
    }
}
