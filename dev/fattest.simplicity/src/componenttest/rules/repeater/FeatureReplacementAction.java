/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.websphere.simplicity.config.ClientConfiguration;
import com.ibm.websphere.simplicity.config.ClientConfigurationFactory;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.ServerConfigurationFactory;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServerFactory;

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
        Map<String, String> featureNameMapping = new HashMap<String, String>(4);
        featureNameMapping.put("ejb", "enterpriseBeans");
        featureNameMapping.put("ejbHome", "enterpriseBeansHome");
        featureNameMapping.put("ejbLite", "enterpriseBeansLite");
        featureNameMapping.put("ejbPersistentTimer", "enterpriseBeansPersistentTimer");
        featureNameMapping.put("ejbRemote", "enterpriseBeansRemote");
        featureNameMapping.put("ejbTest", "enterpriseBeansTest");
        featureNameMapping.put("javaee", "jakartaee");
        featureNameMapping.put("javaeeClient", "jakartaeeClient");
        featureNameMapping.put("jaxrs", "restfulWS");
        featureNameMapping.put("jaxrsClient", "restfulWSClient");
        featureNameMapping.put("jca", "connectors");
        featureNameMapping.put("jmsMdb", "mdb");
        featureNameMapping.put("jms", "messaging");
        featureNameMapping.put("wasJmsClient", "messagingClient");
        featureNameMapping.put("wasJmsServer", "messagingServer");
        featureNameMapping.put("wasJmsSecurity", "messagingSecurity");
        featureNameMapping.put("jsf", "faces");
        featuresWithNameChangeOnEE9 = Collections.unmodifiableMap(featureNameMapping);
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
    public static FeatureReplacementAction EE9_FEATURES() {
        return new JakartaEE9Action();
    }

    private boolean forceAddFeatures = true;
    private int minJavaLevel = 8;
    protected String currentID = null;
    private final Set<String> servers = new HashSet<>(Arrays.asList(ALL_SERVERS));
    private final Set<String> clients = new HashSet<>(Arrays.asList(ALL_CLIENTS));
    private final Set<String> removeFeatures = new HashSet<>();
    private final Set<String> addFeatures = new HashSet<>();
    private TestMode testRunMode = TestMode.LITE;

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
        this(addFeature);
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
        this(addFeatures);
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
    public FeatureReplacementAction withMinJavaLevel(int javaLevel) {
        this.minJavaLevel = javaLevel;
        return this;
    }

    public FeatureReplacementAction fullFATOnly() {
        this.testRunMode = TestMode.FULL;
        return this;
    }

    public FeatureReplacementAction withTestMode(TestMode mode) {
        this.testRunMode = mode;
        return this;
    }

    /**
     * Specify a list of server names to include in the feature replace action and any server configuration
     * files under "publish/servers/SERVER_NAME/" will be scanned.
     * By default, all server config files in publish/servers/ and publish/files/ will be scanned for updates.
     */
    public FeatureReplacementAction forServers(String... serverNames) {
        if (NO_SERVERS.equals(serverNames[0])) {
            servers.clear();
        } else {
            servers.remove(ALL_SERVERS);
            servers.addAll(Arrays.asList(serverNames));
        }
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

    @Override
    public boolean isEnabled() {
        if (JavaInfo.forCurrentVM().majorVersion() < minJavaLevel) {
            Log.info(c, "isEnabled", "Skipping action '" + toString() + "' because the java level is too low.");
            return false;
        }
        if (TestModeFilter.FRAMEWORK_TEST_MODE.compareTo(testRunMode) < 0) {
            Log.info(c, "isEnabled", "Skipping action '" + toString() + "' because the test mode " + testRunMode +
                                     " is not valid for current mode " + TestModeFilter.FRAMEWORK_TEST_MODE);
            return false;
        }
        return true;
    }

    @Override
    public void setup() throws Exception {
        final String m = "setup";
        final String pathToAutoFVTTestFiles = "lib/LibertyFATTestFiles/";
        final String pathToAutoFVTTestServers = "publish/servers/";
        final String pathToAutoFVTTestClients = "publish/clients/";

        //check that there are actually some features to be added or removed
        assertFalse("No features were set to be added or removed", addFeatures.size() == 0 && removeFeatures.size() == 0);

        // Find all of the server configurations to replace features in
        Set<File> serverConfigs = new HashSet<>();
        File serverFolder = new File(pathToAutoFVTTestServers);
        File filesFolder = new File(pathToAutoFVTTestFiles);
        if (servers.contains(ALL_SERVERS)) {
            // Find all *.xml in this test project
            serverConfigs.addAll(findFile(filesFolder, ".xml"));
            servers.remove(ALL_SERVERS);
            if (serverFolder.exists())
                for (File f : serverFolder.listFiles())
                    if (f.isDirectory())
                        servers.add(f.getName());
        }
        for (String serverName : servers) {
            serverConfigs.addAll(findFile(new File(pathToAutoFVTTestServers + serverName), ".xml"));
        }

        // Find all of the client configurations to replace features in
        Set<File> clientConfigs = new HashSet<>();
        File clientFolder = new File(pathToAutoFVTTestClients);
        if (clients.contains(ALL_CLIENTS)) {
            // Find all *.xml in this test project
            clientConfigs.addAll(findFile(clientFolder, ".xml"));
            clients.remove(ALL_CLIENTS);
            if (clientFolder.exists())
                for (File f : clientFolder.listFiles())
                    if (f.isDirectory())
                        clients.add(f.getName());
        }
        for (String clientName : clients) {
            clientConfigs.addAll(findFile(new File(pathToAutoFVTTestServers + clientName), ".xml"));
        }

        // Make sure that XML file we find is a server config file, by checking if it contains the <server> tag
        Log.info(c, m, "Replacing features in files: " + serverConfigs.toString() + "  and  " + clientConfigs.toString());

        // change all the server.xml files
        assertTrue("There were no servers/clients (*.xml) in " + serverFolder.getAbsolutePath() + " or " + filesFolder.getAbsolutePath() + " or " + clientFolder.getAbsolutePath()
                   + ". To use a FeatureReplacementAction, there must be 1 or more servers/clients in any of the above locations.",
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
                        String toAdd = getReplacementFeature(removeFeature, addFeatures);
                        if (toAdd != null)
                            features.add(toAdd);
                    }
                }
            }
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
    private static String getReplacementFeature(String originalFeature, Set<String> replacementFeatures) {
        String methodName = "getReplacementFeature";
        // Example: servlet-3.1 --> servlet-4.0
        int dashOffset = originalFeature.indexOf('-');
        if (dashOffset == -1)
            throw new IllegalArgumentException("Remove feature [ " + originalFeature + " ]: No '-' was found.");
        // "servlet-3.1" ==> "servlet"
        String baseFeature = originalFeature.substring(0, dashOffset + 1);
        // "servlet-4.0".startsWith("servlet-")
        for (String replacementFeature : replacementFeatures) {
            if (replacementFeature.toLowerCase().startsWith(baseFeature.toLowerCase())) {
                Log.info(c, methodName, "Replace feature [ " + originalFeature + " ] with [ " + replacementFeature + " ]");
                return replacementFeature;
            }
        }
        // We need to check that the feature passed is an EE7/EE8 feature which could have a name change on EE9 that doesnt match
        // the original feature name it replaces. We also check viceversa if the feature is an EE9 feature that involves a name change
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
        return getClass().getSimpleName() + "  REMOVE " + removeFeatures + "  ADD " + addFeatures;
    }

    @Override
    public String getID() {
        if (currentID != null) {
            return currentID;
        } else {
            return toString();
        }
    }
}
