/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import com.ibm.websphere.simplicity.config.ClientConfiguration;
import com.ibm.websphere.simplicity.config.ClientConfigurationFactory;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.ServerConfigurationFactory;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.RepeatActions.SEVersion;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FileUtils;

/**
 * Test repeat action that removes and adds features during setup.
 */
public class FeatureReplacementAction implements RepeatTestAction {

    private static final Class<?> c = FeatureReplacementAction.class;

    public static final String ALL_SERVERS = "ALL_SERVERS";
    public static final String ALL_CLIENTS = "ALL_CLIENTS";
    public static final String NO_SERVERS = "NO_SERVERS";
    public static final String NO_CLIENTS = "NO_CLIENTS";

    private static final Map<String, String> featuresWithNameChangeOnEE9;

    static {
        Map<String, String> featureNameMapping = new HashMap<String, String>(20);
        featureNameMapping.put("ejb", "enterpriseBeans");
        featureNameMapping.put("ejbHome", "enterpriseBeansHome");
        featureNameMapping.put("ejbLite", "enterpriseBeansLite");
        featureNameMapping.put("ejbPersistentTimer", "enterpriseBeansPersistentTimer");
        featureNameMapping.put("ejbRemote", "enterpriseBeansRemote");
        featureNameMapping.put("ejbTest", "enterpriseBeansTest");
        featureNameMapping.put("jacc", "appAuthorization");
        featureNameMapping.put("jaspic", "appAuthentication");
        featureNameMapping.put("javaee", "jakartaee");
        featureNameMapping.put("javaeeClient", "jakartaeeClient");
        featureNameMapping.put("javaMail", "mail");
        featureNameMapping.put("jaxrs", "restfulWS");
        featureNameMapping.put("jaxb", "xmlBinding");
        featureNameMapping.put("jaxrsClient", "restfulWSClient");
        featureNameMapping.put("jaxws", "xmlWS");
        featureNameMapping.put("jaxwsTest", "xmlwsTest");
        featureNameMapping.put("jca", "connectors");
        featureNameMapping.put("jcaInboundSecurity", "connectorsInboundSecurity");
        featureNameMapping.put("jpa", "persistence");
        featureNameMapping.put("jpaContainer", "persistenceContainer");
        featureNameMapping.put("jmsMdb", "mdb");
        featureNameMapping.put("jms", "messaging");
        featureNameMapping.put("wasJmsClient", "messagingClient");
        featureNameMapping.put("wasJmsServer", "messagingServer");
        featureNameMapping.put("wasJmsSecurity", "messagingSecurity");
        featureNameMapping.put("jsf", "faces");
        featureNameMapping.put("jsfContainer", "facesContainer");
        featureNameMapping.put("jsp", "pages");
        featureNameMapping.put("el", "expressionLanguage");
        featuresWithNameChangeOnEE9 = Collections.unmodifiableMap(featureNameMapping);
    }

    public static final Predicate<FeatureReplacementAction> GREATER_THAN_OR_EQUAL_JAVA_11 = (action) -> JavaInfo.JAVA_VERSION >= 11;
    public static final Predicate<FeatureReplacementAction> GREATER_THAN_OR_EQUAL_JAVA_17 = (action) -> JavaInfo.JAVA_VERSION >= 17;
    public static final Predicate<FeatureReplacementAction> GREATER_THAN_OR_EQUAL_JAVA_21 = (action) -> JavaInfo.JAVA_VERSION >= 21;

    public static EmptyAction NO_REPLACEMENT() {
        return new EmptyAction();
    }

    /**
     * Replaces any Java EE 8 features with the Java EE 7 equivalent feature.
     */
    public static FeatureReplacementAction EE7_FEATURES() {
        return new EE7FeatureReplacementAction();
    }

    /**
     * Replaces any Java EE 7 features with the Java EE 8 equivalent feature.
     * Will automatically skip if running below Java 8.
     */
    public static FeatureReplacementAction EE8_FEATURES() {
        return new EE8FeatureReplacementAction();
    }

    /**
     * Remove the EE7 and EE8 features; replace them with the EE9 features
     */
    public static JakartaEEAction EE9_FEATURES() {
        return new JakartaEE9Action();
    }

    /**
     * Remove the EE7, EE8, and EE9 features; replace them with the EE10 features
     */
    public static JakartaEEAction EE10_FEATURES() {
        return new JakartaEE10Action();
    }

    /**
     * Remove the EE7, EE8, EE9, and EE10 features; replace them with the EE11 features
     */
    public static JakartaEEAction EE11_FEATURES() {
        return new JakartaEE11Action();
    }

    /**
     * Adds beta option to all the servers and clients
     */
    public static FeatureReplacementAction BETA_OPTION() {
        return new FeatureReplacementAction().withID(BETA_ID).withOptions(BETA_EDITION_TRUE);
    }

    private boolean forceAddFeatures = true;
    private SEVersion minJavaLevel = SEVersion.JAVA8;
    protected String currentID = null;
    private final Set<String> optionsToAdd = new HashSet<String>();
    private final Set<File> optionFilesCreated = new HashSet<File>();
    private final Map<File, File> optionsFileBackupMapping = new HashMap<File, File>();
    private final Set<String> servers = new HashSet<>(Arrays.asList(ALL_SERVERS));
    private final Set<String> clients = new HashSet<>(Arrays.asList(ALL_CLIENTS));
    private final Set<String> removeFeatures = new LinkedHashSet<>();
    private final Set<String> addFeatures = new LinkedHashSet<>();
    private final Set<String> alwaysAddFeatures = new HashSet<>();
    private TestMode testRunMode = TestMode.LITE;
    private boolean liteFATOnly = false;
    private final Set<String> serverConfigPaths = new HashSet<String>();
    private boolean calledForServers = false;
    private boolean calledForServerConfigPaths = false;

    private static final String pathToAutoFVTTestFiles = "lib/LibertyFATTestFiles/";
    private static final String pathToAutoFVTTestServers = "publish/servers/";
    private static final String pathToAutoFVTTestClients = "publish/clients/";

    public static final String BETA_EDITION_TRUE = "-Dcom.ibm.ws.beta.edition=true";
    public static final String BETA_ID = "BETA_JVM_OPTIONS";

    public FeatureReplacementAction() {}

    /**
     * Remove one feature and add one feature.
     *
     * By default features are added even if there was not another version already there
     *
     * @param removeFeature the feature to be removed
     * @param addFeature    the feature to add
     */
    public FeatureReplacementAction(String removeFeature, String addFeature) {
        this();
        addFeature(addFeature);
        removeFeature(removeFeature);
    }

    /**
     * Remove a set of features and add a set of features
     *
     * By default features are added even if there was not another version already there
     *
     * @param removeFeatures the features to remove
     * @param addFeatures    the features to add
     */
    public FeatureReplacementAction(Set<String> removeFeatures, Set<String> addFeatures) {
        this();
        addFeatures(addFeatures);
        removeFeatures(removeFeatures);
    }

    /**
     * Add a set of features.
     *
     * Currently there is no constructor which allows you to just remove features. If you need to do that then
     * pass an empty set to {@link #FeatureReplacementAction(Set, Set)}
     *
     * By default features are added even if there was not another version already there
     *
     * @param addFeatures the features to add
     */
    public FeatureReplacementAction(Set<String> addFeatures) {
        this();
        addFeatures(addFeatures);
    }

    /**
     * Add a single feature
     *
     * Currently there is no constructor which allows you to just remove a single feature. If you need to do that then
     * pass an empty set to {@link #FeatureReplacementAction(Set, Set)}
     *
     * By default features are added even if there was not another version already there
     *
     * @param addFeature the feature to add.
     */
    public FeatureReplacementAction(String addFeature) {
        this();
        addFeature(addFeature);
    }

    /**
     * Add features to the set to be added
     *
     * @param addFeatures the features to be added
     */
    public FeatureReplacementAction addFeatures(Set<String> addFeatures) {
        addFeatures.forEach(this::addFeature);
        return this;
    }

    /**
     * Add features to the set to be added - these features will always be added to the server configuration, even if
     * {@code forceAddFeatures} is set to false.
     *
     * @param alwaysAddedFeatures the features to be added regardless of {@code forceAddFeatures}
     */
    public FeatureReplacementAction alwaysAddFeatures(Set<String> alwaysAddedFeatures) {
        alwaysAddedFeatures.forEach(this::alwaysAddFeature);
        return this;
    }

    /**
     * Add a feature to the set to be added - this features will always be added to the server configuration, even if
     * {@code forceAddFeatures} is set to false.
     *
     * @param alwaysAddedFeature the feature to be added regardless of {@code forceAddFeatures}
     */
    public FeatureReplacementAction alwaysAddFeature(String alwaysAddedFeature) {
        alwaysAddFeatures.add(alwaysAddedFeature);
        return this;
    }

    /**
     * Add features to the set to be removed
     *
     * ...to be clear, this is not the opposite of addFeatures()
     *
     * @param removeFeatures the features to be removed. Wildcards are supported.
     */
    public FeatureReplacementAction removeFeatures(Set<String> removeFeatures) {
        removeFeatures.forEach(this::removeFeature);
        return this;
    }

    /**
     * Add a feature to the set to be added
     *
     * @param addFeature the feature to be added
     */
    public FeatureReplacementAction addFeature(String addFeature) {
        this.addFeatures.add(addFeature);
        return this;
    }

    /**
     * Add a feature to the set to be removed
     *
     * ...to be clear, this is not the opposite of addFeature()
     *
     * @param removeFeature the feature to be removed. Wildcards are supported.
     */
    public FeatureReplacementAction removeFeature(String removeFeature) {
        this.removeFeatures.add(removeFeature.toLowerCase());
        return this;
    }

    /**
     * Defines a minimum java level in order for this RepeatTestAction to be enabled
     */
    public FeatureReplacementAction withMinJavaLevel(SEVersion javaLevel) {
        this.minJavaLevel = javaLevel;
        return this;
    }

    public SEVersion getMinJavaLevel() {
        return this.minJavaLevel;
    }

    public FeatureReplacementAction fullFATOnly() {
        this.testRunMode = TestMode.FULL;
        liteFATOnly = false;
        return this;
    }

    /**
     * Conditionally marks this repeat action as FULL FAT only mode if the Predicate that is passed to it returns true.
     *
     * This method is helpful when you have a list of repeats and you want only one of them to run in lite mode. When running
     * with Jakarta EE 10, you would want to have a previous Jakarta EE repeat to have this method call passing {@link #GREATER_THAN_OR_EQUAL_JAVA_11}
     * in order that at least one of the repeats runs in LITE FAT mode. Otherwise with Java 8 builds, no tests will run in LITE mode which
     * will produce an error. Similarly the same is true for Jakarta EE 11 repeats which require Java 17. If doing a repeat for Jakarta EE 11
     * you will want to call this method and pass {@link #GREATER_THAN_OR_EQUAL_JAVA_17} for previous Jakarta feature repeats. Usually that would be
     * a Jakarta EE 10 repeat because one of the previous Jakarta EE feature repeats would have {@link #GREATER_THAN_OR_EQUAL_JAVA_11} passed to it.
     *
     * The example below will run EE 9 in lite mode with Java 8, EE 10 in lite mode with Java 11 and EE 11 in lite mode with Java 17 and 21.
     *
     * <pre>
     * RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT().fullFATOnly())
     *                 .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly())
     *                 .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(GREATER_THAN_OR_EQUAL_JAVA_11))
     *                 .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(GREATER_THAN_OR_EQUAL_JAVA_17))
     *                 .andWith(FeatureReplacementAction.EE11_FEATURES());
     * </pre>
     *
     * @param  conditional the Predicate that if it returns true, will instruct this repeat action to be done in full mode
     * @return
     */
    public FeatureReplacementAction conditionalFullFATOnly(Predicate<FeatureReplacementAction> conditional) {
        if (conditional.test(this)) {
            this.testRunMode = TestMode.FULL;
            liteFATOnly = false;
        }
        return this;
    }

    public FeatureReplacementAction liteFATOnly() {
        this.testRunMode = TestMode.LITE;
        liteFATOnly = true;
        return this;
    }

    public FeatureReplacementAction withTestMode(TestMode mode) {
        this.testRunMode = mode;
        return this;
    }

    public FeatureReplacementAction withOptions(String... jvmOptions) {
        optionsToAdd.addAll(Arrays.asList(jvmOptions));
        return this;
    }

    /**
     * Specify a list of server names to include in the feature replace action and any server configuration
     * files under "publish/servers/SERVER_NAME/" will be scanned.
     * By default, all server config files in publish/servers/ and publish/files/ will be scanned for updates.
     *
     * Call only one of {@link #forServers(String...)} and {@link #forServerConfigPaths(String...)}. An {@link IllegalStateException} will be thrown if both are called.
     */
    public FeatureReplacementAction forServers(String... serverNames) {
        if (calledForServerConfigPaths) {
            throw new IllegalStateException("Use only one of forServers(...) and forServerConfigPaths(...)");
        }

        if (NO_SERVERS.equals(serverNames[0])) {
            servers.clear();
        } else {
            servers.remove(ALL_SERVERS);
            servers.addAll(Arrays.asList(serverNames));
        }

        calledForServers = true;
        return this;
    }

    /**
     * Specify a list of server names to include in the feature replace action and any server configuration
     * files under "publish/clients/CLIENT_NAME/" will be scanned.
     * By default, all server config files in publish/clients/ will be scanned for updates.
     */
    public FeatureReplacementAction forClients(String... clientNames) {
        if (NO_CLIENTS.equals(clientNames[0])) {
            clients.clear();
        } else {
            clients.remove(ALL_CLIENTS);
            clients.addAll(Arrays.asList(clientNames));
        }
        return this;
    }

    /**
     * Overrides the paths to search for server XML files which will be altered by this feature replacement action. The default is "publish/servers" and "publish/files".
     *
     * Call only one of {@link #forServers(String...)} and {@link #forServerConfigPaths(String...)}. An {@link IllegalStateException} will be thrown if both are called.
     *
     * @param  serverPaths The directories and / or files to search for server XML files.
     * @return             This {@link FeatureReplacementAction} instance.
     */
    public FeatureReplacementAction forServerConfigPaths(String... serverPaths) {
        if (calledForServers) {
            throw new IllegalStateException("Use only one of forServers(...) and forServerConfigPaths(...)");
        }

        Log.info(c, "forServerConfigPaths", "Adding the following server configuration paths: " + Arrays.toString(serverPaths));

        servers.remove(ALL_SERVERS);
        if (serverPaths != null && serverPaths.length > 0) {
            for (String path : serverPaths) {
                if (path.startsWith("publish/files")) {
                    path = path.replace("publish/files", "lib/LibertyFATTestFiles");
                }

                File f = new File(path);
                if (!f.exists()) {
                    throw new IllegalArgumentException("The path specified in the forServerConfigPaths(...) does not exist: "
                                                       + f.getAbsolutePath());
                }

                this.serverConfigPaths.add(path);
            }
            calledForServerConfigPaths = true;
        }

        return this;
    }

    /**
     * Set to true in order to force the addition of all features in the 'addFeature' list.
     * Set to false to "smart upgrade", where features in the 'addFeatures' list are only added if
     * a different version of the corresponding feature is present in the server configuration and the
     * 'removeFeatures' list.
     */
    public FeatureReplacementAction forceAddFeatures(boolean force) {
        this.forceAddFeatures = force;
        return this;
    }

    public FeatureReplacementAction withID(String id) {
        currentID = id;
        return this;
    }

    public FeatureReplacementAction withBeta() {
        this.withOptions(BETA_EDITION_TRUE);
        return this;
    }

    @Override
    public boolean isEnabled() {
        if (JavaInfo.forCurrentVM().majorVersion() < minJavaLevel.majorVersion()) {
            Log.info(c, "isEnabled", "Skipping action '" + toString() + "' because the java level is too low.");
            return false;
        }
        if (TestModeFilter.FRAMEWORK_TEST_MODE.compareTo(testRunMode) < 0) {
            Log.info(c, "isEnabled", "Skipping action '" + toString() + "' because the test mode " + testRunMode +
                                     " is not valid for current mode " + TestModeFilter.FRAMEWORK_TEST_MODE);
            return false;
        }
        if (liteFATOnly && TestModeFilter.FRAMEWORK_TEST_MODE.compareTo(TestMode.LITE) != 0) {
            Log.info(c, "isEnabled", "Skipping action '" + toString() + "' because the test mode " + testRunMode +
                                     " is not LITE and the test is marked to run in LITE FAT mode only.");
            return false;
        }

        return true;
    }

    @Override
    public void setup() throws Exception {

        //check that there are actually some features or options to be added or removed
        assertFalse("No features or options were set to be added or removed", addFeatures.size() == 0 && removeFeatures.size() == 0 && optionsToAdd.size() == 0);

        // Manage JVM options
        if (optionsToAdd.size() != 0)
            setJvmOptions();

        // Manage feature changes
        if (addFeatures.size() != 0 || removeFeatures.size() != 0)
            setFeatures();
    }

    /**
     * Goes through all the servers and clients making the specified feature changes
     */
    private void setFeatures() throws Exception {
        final String m = "setFeatures";
        // Find all of the server configurations to replace features in
        Set<File> serverConfigs = new HashSet<>();
        Set<File> locationsChecked = new HashSet<>(); // Directories we checked for client/server XML files.
        if (!calledForServerConfigPaths) {
            Log.info(c, m, "Checking the following servers for server configuration files: " + servers);
            File serverFolder = new File(pathToAutoFVTTestServers);
            File filesFolder = new File(pathToAutoFVTTestFiles);
            locationsChecked.add(serverFolder);
            locationsChecked.add(filesFolder);
            if (servers.contains(ALL_SERVERS)) {
                // Find all *.xml in this test project
                serverConfigs.addAll(findFile(filesFolder, ".xml"));
                servers.remove(ALL_SERVERS);
                if (serverFolder.exists()) {
                    for (File f : serverFolder.listFiles()) {
                        if (f.isDirectory()) {
                            servers.add(f.getName());
                        }
                    }
                }
            }
            for (String serverName : servers) {
                serverConfigs.addAll(findFile(new File(pathToAutoFVTTestServers + serverName), ".xml"));
            }
        } else {
            Log.info(c, m, "Checking the following paths for server configuration files: " + serverConfigPaths);
            for (String path : serverConfigPaths) {
                File f = new File(path);
                if (f.exists()) {
                    if (f.isDirectory()) {
                        locationsChecked.add(f); // Only need to add and not the flat file.
                        serverConfigs.addAll(findFile(f, ".xml"));
                    } else {
                        serverConfigs.add(f);
                    }
                } else {
                    throw new IllegalStateException("The specified server configuration path does not exist "
                                                    + "(it existed when added - possibly deleted?): "
                                                    + f.getAbsolutePath());
                }
            }
        }

        // Find all of the client configurations to replace features in
        Set<File> clientConfigs = new HashSet<>();
        File clientFolder = new File(pathToAutoFVTTestClients);
        if (clients.contains(ALL_CLIENTS)) {
            locationsChecked.add(clientFolder);
            // Find all *.xml in this test project
            clientConfigs.addAll(findFile(clientFolder, ".xml"));
            clients.remove(ALL_CLIENTS);
            if (clientFolder.exists()) {
                for (File f : clientFolder.listFiles()) {
                    if (f.isDirectory()) {
                        clients.add(f.getName());
                    }
                }
            }
        }
        for (String clientName : clients) {
            clientConfigs.addAll(findFile(new File(pathToAutoFVTTestServers + clientName), ".xml"));
        }

        // Make sure that XML file we find is a server config file, by checking if it contains the <server> tag
        Log.info(c, m, "Replacing features in files: " + serverConfigs.toString() + "  and  " + clientConfigs.toString());

        // change all the server.xml files
        assertTrue("There were no servers/clients (*.xml) in the following folders."
                   + ". To use a FeatureReplacementAction, there must be 1 or more servers/clients in any of the following locations: " + locationsChecked,
                   (serverConfigs.size() > 0 || clientConfigs.size() > 0));

        Set<File> configurations = new HashSet<>();
        configurations.addAll(clientConfigs);
        configurations.addAll(serverConfigs);
        for (File configFile : configurations) {
            if (!configFile.exists() || !configFile.canRead() || !configFile.canWrite()) {
                Log.info(c, m, "File did not exist or was not readable: " + configFile.getAbsolutePath());
                continue;
            }

            // Before we try to unmarshal the file, be sure it's a valid server/client config file
            boolean isServerConfig = false;
            boolean isClientConfig = false;
            try (Scanner s = new Scanner(configFile)) {
                while (!isServerConfig && s.hasNextLine()) {
                    String line = s.nextLine();
                    if (line.contains("<server")) {
                        isServerConfig = true;
                        break;
                    } else if (line.contains("<client")) {
                        isClientConfig = true;
                        break;
                    }
                }
            }
            if (!isServerConfig && !isClientConfig) {
                Log.info(c, m, "File did not contain <server> or <client> so assuming it is not a valid server or client config file" + configFile.getAbsolutePath());
                continue;
            }

            ServerConfiguration serverConfig = isServerConfig ? ServerConfigurationFactory.fromFile(configFile) : null;
            ClientConfiguration clientConfig = isClientConfig ? ClientConfigurationFactory.fromFile(configFile) : null;
            Set<String> features = isServerConfig ? //
                            serverConfig.getFeatureManager().getFeatures() : //
                            clientConfig.getFeatureManager().getFeatures();
            // Convert feature set to all lowercase to prevent case sensitivity issues
            Set<String> intermediateFeatures = new TreeSet<>(features);
            features.clear();
            for (String f : intermediateFeatures)
                features.add(f.toLowerCase());

            Log.info(c, m, "Original features:  " + features);
            if (forceAddFeatures) {
                features.removeAll(removeFeatures);
                //remove any wildcard features, before adding the new feature.
                for (String removeFeature : removeFeatures) {
                    if (removeFeature.endsWith("*")) {
                        removeWildcardFeature(features, removeFeature);
                    }
                }
                features.addAll(addFeatures);
            } else {
                for (String removeFeature : removeFeatures) {
                    boolean removed = false;
                    if (removeFeature.endsWith("*")) {
                        removed = removeWildcardFeature(features, removeFeature);
                    } else if (features.remove(removeFeature)) {
                        removed = true;
                    }

                    // If we found a feature to remove that is actually present in config file, then
                    // replace it with the corresponding feature
                    if (removed) {
                        String toAdd = getReplacementFeature(removeFeature, addFeatures, alwaysAddFeatures);
                        if (toAdd != null)
                            features.add(toAdd);
                    }
                }
            }
            alwaysAddFeatures.forEach(s -> features.add(s));
            Log.info(c, m, "Resulting features: " + features);

            if (isServerConfig) {
                Log.info(c, m, "Config: " + serverConfig);
                ServerConfigurationFactory.toFile(configFile, serverConfig);
            } else {
                ClientConfigurationFactory.toFile(configFile, clientConfig);
            }
        }
        // Make sure config updates are pushed to the liberty install's copy of the servers & clients
        for (String serverName : servers)
            LibertyServerFactory.getLibertyServer(serverName);
        for (String clientName : clients)
            LibertyClientFactory.getLibertyClient(clientName);
    }

    @Override
    public void cleanup() {
        if (optionsFileBackupMapping.isEmpty()) // Nothing to clean up
            return;
        // Undo changes done to jvm.options
        for (Entry<File, File> mapping : optionsFileBackupMapping.entrySet()) {
            File original = mapping.getKey();
            File backup = mapping.getValue();
            Log.info(c, "cleanup", "Restoring " + original + " from " + backup);
            try {
                FileUtils.recursiveDelete(original);
                FileUtils.copyDirectory(backup, original);
            } catch (Exception e) {
                throw new RuntimeException("Exception restoring backup for file " + original, e);
            }
        }
        optionsFileBackupMapping.clear();
        // Clean up backup folder
        Path backupsDir = Paths.get("publish/backups");
        try {
            Log.info(c, "cleanup", "Deleting backups directory.");
            FileUtils.recursiveDelete(backupsDir.toFile());
        } catch (IOException e) {
            Log.error(c, "Problems deleting backup directory", e);
        }
    }

    /**
     * Obtain the replacement for a feature for this replacement action.
     *
     * The lookup uses the base feature name, which is the feature name up to
     * the first '-' character.
     *
     * If EE9 replacement is active, allow the base feature name to be replaced
     * with an EE9 replacement.
     *
     * If no replacement is located, answer null, which indicates that the feature
     * should be removed instead of being replaced.
     *
     * Feature names are required to have a '-', for example, "servlet-3.1". Null
     * is answered for feature names which do not have a '-'.
     *
     * @param  originalFeature     The feature name which is to be replaced.
     * @param  replacementFeatures Table of replacement features.
     *
     * @return                     The replacement feature name. Null if no replacement is available.
     */
    public static String getReplacementFeature(String originalFeature, Set<String> replacementFeatures, Set<String> alwaysAddFeatures) {
        String methodName = "getReplacementFeature";
        // Example: servlet-3.1 --> servlet-4.0
        int dashOffset = originalFeature.indexOf('-');
        if (dashOffset == -1)
            throw new IllegalArgumentException("Remove feature [ " + originalFeature + " ]: No '-' was found.");
        // "servlet-3.1" ==> "servlet"
        String baseFeature = originalFeature.substring(0, dashOffset + 1);
        // "servlet-4.0".startsWith("servlet-")
        // If multiple versions of a feature listed in the replacement list, it will take the last
        // version by looping through all of the features instead of stopping on the first that matches.
        // The Set is a LinkedHashSet so it will iterate over them in the order that add was called.
        String replaceFeature = null;
        for (String replacementFeature : replacementFeatures) {
            if (replacementFeature.toLowerCase().startsWith(baseFeature.toLowerCase())) {
                replaceFeature = replacementFeature;
            }
        }
        for (String alwaysAddFeature : alwaysAddFeatures) {
            if (alwaysAddFeature.toLowerCase().startsWith(baseFeature.toLowerCase())) {
                replaceFeature = alwaysAddFeature;
            }
        }
        if (replaceFeature != null) {
            Log.info(c, methodName, "Replace feature [ " + originalFeature + " ] with [ " + replaceFeature + " ]");
            return replaceFeature;
        }
        // We need to check that the feature passed is an EE7/EE8 feature which could have a name change on EE9 that doesn't match
        // the original feature name it replaces. We also check vice versa if the feature is an EE9 feature that involves a name change
        // to update from EE9 to EE7/EE8
        Log.info(c, methodName, "No feature replacement found for [ " + originalFeature + " ]. Verifying if feature name was changed on EE9.");
        // Reset base feature to not include the "-"
        baseFeature = baseFeature.substring(0, dashOffset);
        String featureReplacement = null;
        for (Map.Entry<String, String> nameChangeEntry : featuresWithNameChangeOnEE9.entrySet()) {
            if (nameChangeEntry.getValue().equalsIgnoreCase(baseFeature)) {
                featureReplacement = nameChangeEntry.getKey();
                Log.info(c, methodName, "Replace EE9 feature [ " + baseFeature + " ] with feature [ " + featureReplacement + " ]");
                baseFeature = featureReplacement;
                break;
            }
            if (nameChangeEntry.getKey().equalsIgnoreCase(baseFeature)) {
                featureReplacement = nameChangeEntry.getValue();
                Log.info(c, methodName, "Replace base feature [ " + baseFeature + " ] with EE9 feature [ " + featureReplacement + " ]");
                baseFeature = featureReplacement;
                break;
            }
        }
        if (featureReplacement == null) {
            Log.info(c, methodName, "Remove feature [ " + originalFeature + " ]: No replacement is available");
            return null;
        }
        baseFeature += "-";
        // Re-check the features with the name changes
        for (String replacementFeature : replacementFeatures) {
            if (replacementFeature.toLowerCase().contains(baseFeature.toLowerCase())) {
                Log.info(c, methodName, "Replace feature [ " + originalFeature + " ] with [ " + replacementFeature + " ]");
                return replacementFeature;
            }
        }
        // We may need to remove a feature without adding any replacement
        // (e.g. jsonb-1.0 is EE8 only) so in this case return null
        Log.info(c, methodName, "Remove feature [ " + originalFeature + " ]: No replacement is available");
        return null;
    }

    /**
     * Goes through all the servers and clients setting the specified JVM options
     */
    private void setJvmOptions() throws IOException {
        final String m = "setJvmOptions";

        Path publishDir = Paths.get("publish");
        Path backupsDir = Paths.get("publish/backups");

        Set<File> serverOptions = new HashSet<>();
        Set<File> locationsChecked = new HashSet<>(); // Directories we checked for client/server options files.
        Set<String> servers = new HashSet<>(); // All Servers found
        Log.info(c, m, "Checking all servers for jvm.options files");
        File serverFolder = new File(pathToAutoFVTTestServers);

        // Find all of the server jvm.options to add options
        if (serverFolder.exists()) {
            for (File f : serverFolder.listFiles()) {
                if (f.isDirectory()) {
                    servers.add(f.getName());
                }
            }
        }
        locationsChecked.add(serverFolder);

        // Go through all the servers
        for (String serverName : servers) {
            Set<File> optionsFound = findFile(new File(pathToAutoFVTTestServers + serverName), "jvm.options");
            // If options file doesn't exist
            if (optionsFound.isEmpty()) {
                // Create it
                File jvmOptionsCreated = new File(pathToAutoFVTTestServers + serverName + "/jvm.options");
                if (jvmOptionsCreated.createNewFile()) {
                    Log.info(c, m, "Successfully created jvm.options in: " + serverName);
                    optionFilesCreated.add(jvmOptionsCreated);
                } else
                    Log.info(c, m, "Failed to create jvm.options in: " + serverName);
                optionsFound.add(jvmOptionsCreated);
            }
            serverOptions.addAll(optionsFound);
        }

        Set<File> clientOptions = new HashSet<>();
        Set<String> clients = new HashSet<>(); // All clients found
        File clientFolder = new File(pathToAutoFVTTestClients);
        // Find all jvm.options in the clients
        clientOptions.addAll(findFile(clientFolder, "jvm.options"));
        if (clientFolder.exists()) {
            for (File f : clientFolder.listFiles()) {
                if (f.isDirectory()) {
                    clients.add(f.getName());
                }
            }
        }

        // Go through all the clients
        for (String clientName : clients) {
            Set<File> optionsFound = findFile(new File(pathToAutoFVTTestClients + clientName), "jvm.options");
            // If options file doesn't exist
            if (optionsFound.isEmpty()) {
                // Create it
                File jvmOptionsCreated = new File(pathToAutoFVTTestClients + clientName + "/jvm.options");
                if (jvmOptionsCreated.createNewFile()) {
                    Log.info(c, m, "Successfully created jvm.options in: " + clientName);
                    optionFilesCreated.add(jvmOptionsCreated);
                } else
                    Log.info(c, m, "Failed to create jvm.options in: " + clientName);
                optionsFound.add(jvmOptionsCreated);
            }
            clientOptions.addAll(optionsFound);
        }
        locationsChecked.add(clientFolder);

        Log.info(c, m, "Adding options in files: " + serverOptions.toString() + "  and  " + clientOptions.toString());

        // change all the jvm.options files
        assertTrue("There were no servers/clients found in the following folders."
                   + ". To use a BetaOptionsAction, there must be 1 or more servers/clients in any of the following locations: " + locationsChecked,
                   (serverOptions.size() > 0 || clientOptions.size() > 0));

        Set<File> optionFilesOriginal = new HashSet<>();
        optionFilesOriginal.addAll(clientOptions);
        optionFilesOriginal.addAll(serverOptions);
        for (File optionsFile : optionFilesOriginal) {
            Log.info(c, m, "Modifying options file: " + optionsFile.getAbsolutePath());
            if (!optionsFile.exists() || !optionsFile.canRead() || !optionsFile.canWrite()) {
                Log.info(c, m, "File did not exist or was not readable: " + optionsFile.getAbsolutePath());
                continue;
            }

            try (FileWriter optionsWriter = new FileWriter(optionsFile, true);
                            BufferedWriter bw = new BufferedWriter(optionsWriter);) {
                Path backupFile = backupsDir.resolve(publishDir.relativize(optionsFile.toPath()));
                Files.createDirectories(backupFile.getParent());
                FileUtils.copyDirectory(optionsFile, backupFile.toFile());
                optionsFileBackupMapping.put(optionsFile, backupFile.toFile());

                // Add new line if file already has content
                if (optionsFile.length() != 0)
                    bw.newLine();

                for (String option : this.optionsToAdd) {
                    bw.write(option);
                    bw.newLine();
                }
            }
        }
        // Make sure options updates are pushed to the liberty install's copy of the servers & clients
        for (String serverName : servers)
            LibertyServerFactory.getLibertyServer(serverName);
        for (String clientName : clients)
            LibertyClientFactory.getLibertyClient(clientName);
    }

    private static boolean removeWildcardFeature(Set<String> features, String removeFeature) {
        String matcher = removeFeature.substring(0, removeFeature.length() - 1);
        boolean removed = false;
        Iterator<String> iterator = features.iterator();
        while (iterator.hasNext()) {
            String feature = iterator.next();
            if (feature.startsWith(matcher)) {
                iterator.remove();
                removed = true;
            }
        }
        return removed;
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
        return set;
    }

    @Override
    public String toString() {
        return getID();
    }

    @Override
    public String getID() {
        if (currentID != null) {
            return currentID;
        } else {
            return getClass().getSimpleName() + "  REMOVE " + removeFeatures + "  ADD " + addFeatures;
        }
    }
}
