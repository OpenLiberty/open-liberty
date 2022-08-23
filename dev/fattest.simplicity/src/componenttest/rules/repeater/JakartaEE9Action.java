/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

    private static final String TRANSFORMER_RULES_APPEND_ROOT = System.getProperty("user.dir") + "/publish/rules/";
    static final String TRANSFORMER_RULES_ROOT = System.getProperty("user.dir") + "/autoFVT-templates/";
    private static final Map<String, String> DEFAULT_TRANSFORMATION_RULES = new HashMap();
    private static final Map<String, String> TRANSFORMATION_RULES_APPEND = new HashMap();
    private static boolean WIDEN = false;

    static {
        // Fill the default transformation rules for the transformer
        // The rules are copied from 'open-liberty/dev/wlp-jakartaee-transform/rules' to
        // the user 'autoFVT-templates' folder.
        //
        //   jakarta-selections.properties
        //   jakarta-renames.properties
        //   jakarta-versions.properties
        //   jakarta-bundles.properties
        //   jakarta-direct.properties
        //   jakarta-text.properties
        //   (other xml properties files as referenced by 'jakarta-text.properties'
        DEFAULT_TRANSFORMATION_RULES.put("-tr", TRANSFORMER_RULES_ROOT + "jakarta-renames.properties"); // Package renames
        DEFAULT_TRANSFORMATION_RULES.put("-ts", TRANSFORMER_RULES_ROOT + "jakarta-selections.properties"); // File selections and omissions
        DEFAULT_TRANSFORMATION_RULES.put("-tv", TRANSFORMER_RULES_ROOT + "jakarta-versions-ee9.properties"); // Package version updates
        DEFAULT_TRANSFORMATION_RULES.put("-tb", TRANSFORMER_RULES_ROOT + "jakarta-bundles.properties"); // bundle identity updates
        DEFAULT_TRANSFORMATION_RULES.put("-td", TRANSFORMER_RULES_ROOT + "jakarta-direct.properties"); // exact java string constant updates
        DEFAULT_TRANSFORMATION_RULES.put("-tf", TRANSFORMER_RULES_ROOT + "jakarta-text.properties"); // text updates
    }

    // Point-in-time list of enabled JakartaEE9 features.
    // This list is of only the currently enabled features.
    //
    // FAT tests use a mix of enabled features and not yet enabled
    // features, which is necessary for the FATs to run.

    static final String[] EE9_FEATURES_ARRAY = {
                                                 "appClientSupport-2.0",
                                                 "jakartaee-9.1",
                                                 "webProfile-9.1",
                                                 "jakartaeeClient-9.1",
                                                 "componenttest-2.0", // replaces "componenttest-1.0"
                                                 "txtest-2.0",
                                                 "appAuthentication-2.0",
                                                 "appAuthorization-2.0",
                                                 "appSecurity-4.0",
                                                 "batch-2.0",
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
        removeFeatures(JakartaEE10Action.EE10_FEATURE_SET);
        forceAddFeatures(false);
        withID(ID);
    }

    @Override
    public String toString() {
        return "JakartaEE9 FAT repeat action (" + getID() + ")";
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
    public JakartaEE9Action liteFATOnly() {
        return (JakartaEE9Action) super.liteFATOnly();
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

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional package transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    public JakartaEE9Action withLocalPackageTransformAppend(String fileName) {
        TRANSFORMATION_RULES_APPEND.put("-tr", TRANSFORMER_RULES_APPEND_ROOT + fileName);
        return this;
    }

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional selection transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    public JakartaEE9Action withLocalSelectionTransformAppend(String fileName) {
        TRANSFORMATION_RULES_APPEND.put("-ts", TRANSFORMER_RULES_APPEND_ROOT + fileName);
        return this;
    }

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional version transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    public JakartaEE9Action withLocalVersionTransformAppend(String fileName) {
        TRANSFORMATION_RULES_APPEND.put("-tv", TRANSFORMER_RULES_APPEND_ROOT + fileName);
        return this;
    }

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional bundle transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    public JakartaEE9Action withLocalBundleTransformAppend(String fileName) {
        TRANSFORMATION_RULES_APPEND.put("-tb", TRANSFORMER_RULES_APPEND_ROOT + fileName);
        return this;
    }

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional string transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    public JakartaEE9Action withLocalStringTransformAppend(String fileName) {
        TRANSFORMATION_RULES_APPEND.put("-td", TRANSFORMER_RULES_APPEND_ROOT + fileName);
        return this;
    }

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional xml transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    public JakartaEE9Action withLocalXMLTransformAppend(String fileName) {
        TRANSFORMATION_RULES_APPEND.put("-tf", TRANSFORMER_RULES_APPEND_ROOT + fileName);
        return this;
    }

    /**
     * The widen option in the transformer enables the transformer to handle things like jars
     * inside of other jars or zips inside of other zips. These are not the usual setup of
     * of bundles and applications, so it is only enabled by an argument to the transformer.
     */
    public JakartaEE9Action withWiden() {
        WIDEN = true;
        return this;
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
     * Invoke the Jakarta transformer on an application with added transformation rules.
     *
     * @param appPath                   The application path to be transformed to Jakarta
     * @param newAppPath                The application path of the transformed file (or <code>null<code>)
     * @param transformationRulesAppend The map with the additional transformation rules to add
     */
    public static void transformApp(Path appPath, Path newAppPath, Map<String, String> transformationRulesAppend) {
        TRANSFORMATION_RULES_APPEND.putAll(transformationRulesAppend);
        transformApp(appPath, newAppPath);
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
        transformApp(appPath, newAppPath, DEFAULT_TRANSFORMATION_RULES, TRANSFORMATION_RULES_APPEND, WIDEN);
    }

    protected static void transformApp(Path appPath, Path newAppPath, Map<String, String> defaultTransformationRules,
                                       Map<String, String> transformationRulesAppend, boolean widen) {
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
            Class.forName("org.eclipse.transformer.cli.JakartaTransformerCLI");
        } catch (Throwable e) {
            String mesg = "Unable to load the org.eclipse.transformer.cli.JakartaTransformerCLI class. " +
                          "Did you include 'addRequiredLibraries.dependsOn addJakartaTransformer' in the FAT's build.gradle file?";
            Log.error(c, m, e, mesg);
            throw new RuntimeException(mesg, e);
        }

        Path outputPath;
        Path backupPath = null;
        if (newAppPath == null) {
            outputPath = appPath.resolveSibling(appPath.getFileName() + ".jakarta");
            Path parent1 = appPath.toAbsolutePath().getParent();
            if (parent1 != null) {
                Path parent2 = parent1.getParent();
                if (parent2 != null) {
                    backupPath = parent2.resolve("backup");
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
                    Log.info(c, m, "Unable to create backup directory.");
                    FileNotFoundException fnfe = new FileNotFoundException("Parent path not found: " + parent1.toAbsolutePath().toString());
                    Log.error(c, m, fnfe);
                }
            } else {
                Log.info(c, m, "Unable to create backup directory.");
                FileNotFoundException fnfe = new FileNotFoundException("Parent path not found: " + appPath.toAbsolutePath().toString());
                Log.error(c, m, fnfe);
            }
        } else {
            outputPath = newAppPath;
        }

        //Ensure previous transformed apps are cleared out
        if (outputPath.toFile().exists()) {
            if (outputPath.toFile().delete()) {
                Log.info(c, m, "Removed existing transformed application: " + outputPath.toString());
            } else {
                Log.warning(c, "Tried to remove existing transformed application, but failed: " + outputPath.toString());
            }
        }

        try {
            // Invoke the jakarta transformer
            String[] args = new String[(widen ? 16 : 15) + transformationRulesAppend.size() * 2];

            args[0] = appPath.toAbsolutePath().toString(); // input
            args[1] = outputPath.toAbsolutePath().toString(); // output

            args[2] = "-q"; // quiet output

            // override jakarta default properties, which are
            // packaged in the transformer jar
            args[3] = "-tr"; // package-renames
            args[4] = defaultTransformationRules.get("-tr");
            args[5] = "-ts"; // file selections and omissions
            args[6] = defaultTransformationRules.get("-ts");
            args[7] = "-tv"; // package version updates
            args[8] = defaultTransformationRules.get("-tv");
            args[9] = "-tb"; // bundle identity updates
            args[10] = defaultTransformationRules.get("-tb");
            args[11] = "-td"; // exact java string constant updates
            args[12] = defaultTransformationRules.get("-td");
            args[13] = "-tf"; // text updates
            args[14] = defaultTransformationRules.get("-tf");
            // The widen option handles jars inside of jars.
            if (widen) {
                args[15] = "-w";
            }

            // Go through the additions
            if (transformationRulesAppend.size() > 0) {
                String[] additions = new String[transformationRulesAppend.size() * 2];
                int index = 0;
                for (Entry<String, String> addition : transformationRulesAppend.entrySet()) {
                    additions[index++] = addition.getKey();
                    additions[index++] = addition.getValue();
                }
                System.arraycopy(additions, 0, args, widen ? 16 : 15, transformationRulesAppend.size() * 2);
            }

            Log.info(c, m, "Initializing transformer with args: " + Arrays.toString(args));

            TransformerHolder._INSTANCE.transform(args);

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

    // uses the initialisation-on-demand holder idiom to provide safe and fast lazy loading
    private static class TransformerHolder {
        public static final TransformSubAction _INSTANCE = new componenttest.rules.repeater.TransformSubActionImpl();
    }
}
