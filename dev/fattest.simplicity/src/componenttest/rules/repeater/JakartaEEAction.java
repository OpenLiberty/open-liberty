/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.Fileset;
import com.ibm.websphere.simplicity.config.JMSActivationSpec;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.ServerConfigurationFactory;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.config.WasJmsProperties;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.SharedServer;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatActions.EEVersion;
import componenttest.rules.repeater.RepeatActions.SEVersion;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FileUtils;

/**
 *
 */
public abstract class JakartaEEAction extends FeatureReplacementAction {

    private static final Class<?> c = JakartaEEAction.class;

    public static final String EE9_ACTION_ID = "EE9_FEATURES";
    public static final String EE10_ACTION_ID = "EE10_FEATURES";
    public static final String EE11_ACTION_ID = "EE11_FEATURES";

    static final String TRANSFORMER_RULES_APPEND_ROOT = System.getProperty("user.dir") + "/publish/rules/";
    static final String TRANSFORMER_RULES_ROOT = System.getProperty("user.dir") + "/autoFVT-templates/";

    private boolean skipTransformation;

    public boolean isSkipTransformation() {
        return skipTransformation;
    }

    public JakartaEEAction setSkipTransformation(boolean skip) {
        skipTransformation = skip;
        return this;
    }

    public static final boolean isEE9Active() {
        return RepeatTestFilter.isRepeatActionActive(EE9_ACTION_ID);
    }

    public static final boolean isEE10Active() {
        return RepeatTestFilter.isRepeatActionActive(EE10_ACTION_ID);
    }

    public static final boolean isEE11Active() {
        return RepeatTestFilter.isRepeatActionActive(EE11_ACTION_ID);
    }

    public static final boolean isEE9OrLaterActive() {
        return RepeatTestFilter.isAnyRepeatActionActive(EE9_ACTION_ID, EE10_ACTION_ID, EE11_ACTION_ID);
    }

    public static final boolean isEE10OrLaterActive() {
        return RepeatTestFilter.isAnyRepeatActionActive(EE10_ACTION_ID, EE11_ACTION_ID);
    }

    public static final boolean isEE11OrLaterActive() {
        return RepeatTestFilter.isAnyRepeatActionActive(EE11_ACTION_ID);
    }

    protected JakartaEEAction(Set<String> addFeatures) {
        super(addFeatures);
    }

    @Override
    public JakartaEEAction addFeature(String addFeature) {
        return (JakartaEEAction) super.addFeature(addFeature);
    }

    @Override
    public JakartaEEAction conditionalFullFATOnly(Predicate<FeatureReplacementAction> conditional) {
        return (JakartaEEAction) super.conditionalFullFATOnly(conditional);
    }

    @Override
    public JakartaEEAction fullFATOnly() {
        return (JakartaEEAction) super.fullFATOnly();
    }

    @Override
    public JakartaEEAction liteFATOnly() {
        return (JakartaEEAction) super.liteFATOnly();
    }

    @Override
    public JakartaEEAction withTestMode(TestMode mode) {
        return (JakartaEEAction) super.withTestMode(mode);
    }

    @Override
    public JakartaEEAction addFeatures(Set<String> addFeatures) {
        return (JakartaEEAction) super.addFeatures(addFeatures);
    }

    @Override
    public JakartaEEAction removeFeature(String removeFeature) {
        return (JakartaEEAction) super.removeFeature(removeFeature);
    }

    @Override
    public JakartaEEAction removeFeatures(Set<String> removeFeatures) {
        return (JakartaEEAction) super.removeFeatures(removeFeatures);
    }

    @Override
    public JakartaEEAction withMinJavaLevel(SEVersion javaLevel) {
        return (JakartaEEAction) super.withMinJavaLevel(javaLevel);
    }

    @Override
    public JakartaEEAction withID(String id) {
        return (JakartaEEAction) super.withID(id);
    }

    @Override
    public JakartaEEAction forServers(String... serverNames) {
        return (JakartaEEAction) super.forServers(serverNames);
    }

    @Override
    public JakartaEEAction forClients(String... clientNames) {
        return (JakartaEEAction) super.forClients(clientNames);
    }

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional package transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    public abstract JakartaEEAction withLocalPackageTransformAppend(String fileName);

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional selection transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    public abstract JakartaEEAction withLocalSelectionTransformAppend(String fileName);

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional version transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    public abstract JakartaEEAction withLocalVersionTransformAppend(String fileName);

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional bundle transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    public abstract JakartaEEAction withLocalBundleTransformAppend(String fileName);

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional string transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    public abstract JakartaEEAction withLocalStringTransformAppend(String fileName);

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional xml transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    public abstract JakartaEEAction withLocalXMLTransformAppend(String fileName);

    /**
     * The widen option in the transformer enables the transformer to handle things like jars
     * inside of other jars or zips inside of other zips. These are not the usual setup of
     * of bundles and applications, so it is only enabled by an argument to the transformer.
     */
    public abstract JakartaEEAction withWiden();

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
        transformApp(appPath, (Path) null);
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
    public static void transformApp(Path appPath, EEVersion eeVersion) {
        transformApp(appPath, null, eeVersion);
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
    public static void transformApp(Path appPath, Path newAppPath, EEVersion eeVersion) {
        switch (eeVersion) {
            case EE9:
                JakartaEE9Action.staticTransformApplication(appPath, newAppPath, null);
                break;
            case EE10:
                JakartaEE10Action.staticTransformApplication(appPath, newAppPath, null);
                break;
            case EE11:
                JakartaEE11Action.staticTransformApplication(appPath, newAppPath, null);
                break;
            default:
                // do nothing
        }
    }

    /**
     * Invoke the Jakarta transformer on an application with added transformation rules.
     *
     * @param appPath                   The application path to be transformed to Jakarta
     * @param newAppPath                The application path of the transformed file (or <code>null<code>)
     * @param transformationRulesAppend The map with the additional transformation rules to add
     */
    public static void transformApp(Path appPath, Path newAppPath, Map<String, String> transformationRulesAppend) {
        List<RepeatTestAction> actions = RepeatTestFilter.getRepeatActions();

        //If you are running a repeat test inside a repeat test
        //E.G. a microprofile repeat action inside an EE repeat action
        //the JakartaEEAction might not be at the top of the stack
        actions.stream()
                        .filter(a -> a instanceof JakartaEEAction)
                        .map(a -> (JakartaEEAction) a)
                        .findFirst()
                        .ifPresent(a -> a.transformApplication(appPath, newAppPath, transformationRulesAppend));
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
        List<RepeatTestAction> actions = RepeatTestFilter.getRepeatActions();

        //If you are running a repeat test inside a repeat test
        //E.G. a microprofile repeat action inside an EE repeat action
        //the JakartaEEAction might not be at the top of the stack
        Optional<JakartaEEAction> jakartaAction = actions.stream()
                        .filter(a -> a instanceof JakartaEEAction)
                        .map(a -> (JakartaEEAction) a)
                        .findFirst();
        if (jakartaAction.isPresent()) {
            JakartaEEAction action = jakartaAction.get();
            if (!action.isSkipTransformation()) {
                action.transformApplication(appPath, newAppPath, null);
            }
        }
    }

    abstract void transformApplication(Path appPath, Path newAppPath, Map<String, String> transformationRulesAppend);

    protected static void transformApp(Path appPath, Path newAppPath, Map<String, String> defaultTransformationRules,
                                       Map<String, String> transformationRulesAppend, boolean widen) {
        final String m = "transformApp";
        Log.info(c, m, "Transforming app: " + appPath);

        // Capture stdout/stderr streams
        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;
        // Setup file output stream and only keep if we fail
        PrintStream ps = null;
        File outputLog = new File("results/transformer_" + appPath.getFileName() + ".log");
        try {
            FileOutputStream fos = new FileOutputStream(outputLog);
            ps = new PrintStream(fos);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

        if (ps != null) {
            System.setOut(ps);
            System.setErr(ps);
        }

        Throwable classLoadError = null;
        try {
            Class.forName("org.eclipse.transformer.cli.JakartaTransformerCLI");
        } catch (Throwable e) {
            classLoadError = e;
        }
        if (classLoadError != null || !new File(TRANSFORMER_RULES_ROOT).exists()) {
            String mesg = "Unable to find the transformer rules OR failed to load the org.eclipse.transformer.cli.JakartaTransformerCLI class. \n" +
                          "Did you include 'addRequiredLibraries.dependsOn addJakartaTransformer' in the FAT's build.gradle file?\n" +
                          "If applications are already Jakarta-based, update to call setSkipTransformation(true) on your JakartaEEAction repeat action(s).";
            Log.error(c, m, classLoadError, mesg);
            throw new RuntimeException(mesg, classLoadError);
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
            String[] args = new String[(widen ? 15 : 14) + transformationRulesAppend.size() * 2];

            args[0] = appPath.toAbsolutePath().toString(); // input
            args[1] = outputPath.toAbsolutePath().toString(); // output

            // override jakarta default properties, which are
            // packaged in the transformer jar
            args[2] = "-tr"; // package-renames
            args[3] = defaultTransformationRules.get("-tr");
            args[4] = "-ts"; // file selections and omissions
            args[5] = defaultTransformationRules.get("-ts");
            args[6] = "-tv"; // package version updates
            args[7] = defaultTransformationRules.get("-tv");
            args[8] = "-tb"; // bundle identity updates
            args[9] = defaultTransformationRules.get("-tb");
            args[10] = "-td"; // exact java string constant updates
            args[11] = defaultTransformationRules.get("-td");
            args[12] = "-tf"; // text updates
            args[13] = defaultTransformationRules.get("-tf");
            // The widen option handles jars inside of jars.
            if (widen) {
                args[14] = "-w";
            }

            // Go through the additions
            if (transformationRulesAppend.size() > 0) {
                String[] additions = new String[transformationRulesAppend.size() * 2];
                int index = 0;
                for (Entry<String, String> addition : transformationRulesAppend.entrySet()) {
                    additions[index++] = addition.getKey();
                    additions[index++] = addition.getValue();
                }
                System.arraycopy(additions, 0, args, widen ? 15 : 14, transformationRulesAppend.size() * 2);
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
            if (ps != null) {
                System.setOut(originalOut);
                System.setErr(originalErr);
                ps.close();
            }
            Log.info(c, m, "Transforming complete app: " + outputPath);
        }
    }

    // uses the initialisation-on-demand holder idiom to provide safe and fast lazy loading
    private static class TransformerHolder {
        public static final TransformSubAction _INSTANCE = new componenttest.rules.repeater.TransformSubActionImpl();
    }

    /**
     * Recursively copy a directory to a new location and transform all the jars in the copy
     *
     * @param  src         the source directory
     * @param  dst         the destination directory
     * @throws IOException if there's a problem copying files
     */
    public static void copyAndTransformLibrary(Path src, Path dst) throws IOException {
        Log.info(c, "copyAndTransformLibrary", "Copy library from " + src + " to " + dst);

        if (dst.getParent() != null) {
            Files.createDirectories(dst.getParent());
        }

        Files.walk(src).forEach(path -> {
            Path target = dst.resolve(src.relativize(path));

            // Check and warn for files which would make a straight copy fail
            // For some reason some library directories on test machines include broken symlinks
            if (Files.notExists(path)) {
                Log.warning(c, path + " does not exist");
                return;
            }
            if (!Files.exists(path)) {
                Log.warning(c, path + " can't be checked for existence");
                return;
            }
            if (!Files.isDirectory(path) && !Files.isReadable(path)) {
                Log.warning(c, path + " is not readable");
                return;
            }
            if (Files.isDirectory(path) && Files.isDirectory(target)) {
                Log.debug(c, "Directory " + target + " already exists, no need to create it");
                return;
            }

            try {
                Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Failed to copy " + path, e);
            }
        });

        // Find and transform jars
        Log.info(c, "copyAndTransformLibrary", "Transform jars under " + dst);
        try (Stream<Path> dstFiles = Files.walk(dst)) {
            dstFiles.filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().endsWith(".jar"))
                            .forEach(JakartaEEAction::transformApp);
        }
    }

    /**
     * Replace a prefix of all {@code dir} attributes of all filesets under top-level {@code <library>} elements.
     * <p>
     * For example, if called with {@code originalPrefix = "/old"} and {@code newPrefix = "/new"} then
     * <p>
     *
     * <pre>
     * {@code
     * <library id="myLibrary>
     *   <fileset dir="/old/jars" include="*.jar"/>
     * </library>
     * }
     * </pre>
     *
     * is replaced with
     *
     * <pre>
     * {@code
     * <library id="myLibrary>
     *   <fileset dir="/new/jars" include="*.jar"/>
     * </library>
     * }
     * </pre>
     *
     * @param serverXml      the serverXml file to update
     * @param originalPrefix the original directory prefix to replace
     * @param newPrefix      the new string to replace {@code originalPrefix} with
     */
    public static void updateLibraryPrefix(File serverXml, String originalPrefix, String newPrefix) {
        ServerConfiguration config;
        try {
            config = ServerConfigurationFactory.fromFile(serverXml);
        } catch (Exception e) {
            Log.warning(c, serverXml + " does not appear to be a valid server.xml file");
            return;
        }

        boolean madeChanges = false;
        for (Library lib : config.getLibraries()) {
            for (Fileset fileset : lib.getFilesets()) {
                if (fileset.getDir().startsWith(originalPrefix)) {
                    String dirWithoutPrefix = fileset.getDir().substring(originalPrefix.length());
                    fileset.setDir(newPrefix + dirWithoutPrefix);
                    madeChanges = true;
                }
            }
        }

        if (madeChanges) {
            try {
                ServerConfigurationFactory.toFile(serverXml, config);
            } catch (Exception e) {
                Log.error(c, "updateLibraryPrefix", e, "Unable to write updated configuration to " + serverXml);
                throw new RuntimeException("Unable to write updated configuration to " + serverXml, e);
            }
        }
    }

    /**
     * Calls {@link #updateLibraryPrefix(File, String, String)} on each server.xml in the publish servers directory and in LibertyFATTestFiles
     *
     * @param  originalPrefix the original directory prefix to replace
     * @param  newPrefix      the new string to replace {@code originalPrefix} with
     * @throws IOException    if there's an error finding the xml files
     */
    public static void updateLibraryPrefix(String originalPrefix, String newPrefix) throws IOException {
        Path pathToAutoFVTTestFiles = Paths.get("lib/LibertyFATTestFiles");
        Path pathToAutoFVTTestServers = Paths.get("publish/servers");

        try (Stream<Path> testServerWalk = Files.walk(pathToAutoFVTTestServers);
                        Stream<Path> testFileWalk = Files.walk(pathToAutoFVTTestFiles)) {
            Stream.concat(testServerWalk, testFileWalk)
                            .filter(Files::isRegularFile)
                            .map(Path::toFile)
                            .filter(f -> f.getName().endsWith(".xml"))
                            .forEach(f -> updateLibraryPrefix(f, originalPrefix, newPrefix));
        }
    }

    @Override
    protected void updateServerConfig(ServerConfiguration serverConfig) {
        // Update JMS queue types
        for (JMSActivationSpec spec : serverConfig.getJMSActivationSpecs()) {
            for (WasJmsProperties properties : spec.getWasJmsProperties()) {
                if ("javax.jms.Queue".equals(properties.getDestinationType())) {
                    properties.setDestinationType("jakarta.jms.Queue");
                } else if ("javax.jms.Topic".equals(properties.getDestinationType())) {
                    properties.setDestinationType("jakarta.jms.Topic");
                }
            }
        }
        ConfigElementList<Variable> variables = serverConfig.getVariables();
        for (Variable variable : variables) {
            if (variable.getName().equalsIgnoreCase("wmqJmsClient.rar.location")) {
                String value = variable.getValue();
                value = value.replace("wmq.wlp.rar", "wmq.jakarta.jmsra.rar");
                value = value.replace("wmq.jmsra.rar", "wmq.jakarta.jmsra.rar");
                value = value.replace("wmq.jmsra.v7.rar", "wmq.jakarta.jmsra.rar");
                variable.setValue(value);
            }
        }
    }
}
