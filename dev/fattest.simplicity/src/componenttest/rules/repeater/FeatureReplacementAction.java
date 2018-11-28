/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

import com.ibm.websphere.simplicity.config.ClientConfiguration;
import com.ibm.websphere.simplicity.config.ClientConfigurationFactory;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.ServerConfigurationFactory;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test repeat action that removes and adds features during setup.
 */
public class FeatureReplacementAction implements RepeatTestAction {

    private static final Class<?> c = FeatureReplacementAction.class;

    static final String ALL_SERVERS = "ALL_SERVERS";
    static final String ALL_CLIENTS = "ALL_CLIENTS";

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

    private boolean forceAddFeatures = true;
    private int minJavaLevel = 7;
    protected String currentID = null;
    private final Set<String> servers = new HashSet<>(Arrays.asList(ALL_SERVERS));
    private final Set<String> clients = new HashSet<>(Arrays.asList(ALL_CLIENTS));
    private final Set<String> removeFeatures = new HashSet<>();
    private final Set<String> addFeatures = new HashSet<>();

    public FeatureReplacementAction() {}

    /**
     * Remove one feature and add one feature.
     *
     * By default features are added even if there was not another version already there
     *
     * @param removeFeature the feature to be removed
     * @param addFeature the feature to add
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
     * @param addFeatures the features to add
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
     * @param removeFeatures the features to remove
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
     * @return this
     */
    public FeatureReplacementAction addFeatures(Set<String> addFeatures) {
        this.addFeatures.addAll(addFeatures);
        return this;
    }

    /**
     * Add features to the set to be removed
     *
     * ...to be clear, this is not the opposite of addFeatures()
     *
     * @param removeFeatures the features to be removed. Wildcards are supported.
     * @return this
     */
    public FeatureReplacementAction removeFeatures(Set<String> removeFeatures) {
        this.removeFeatures.addAll(removeFeatures);
        return this;
    }

    /**
     * Add a feature to the set to be added
     *
     * @param addFeature the feature to be added
     * @return this
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
     * @return this
     */
    public FeatureReplacementAction removeFeature(String removeFeature) {
        this.removeFeatures.add(removeFeature);
        return this;
    }

    /**
     * Defines a minimum java level in order for this RepeatTestAction to be enabled
     */
    public FeatureReplacementAction withMinJavaLevel(int javaLevel) {
        this.minJavaLevel = javaLevel;
        return this;
    }

    /**
     * Specify a list of server names to include in the feature replace action and any server configuration
     * files under "publish/servers/SERVER_NAME/" will be scanned.
     * By default, all server config files in publish/servers/ and publish/files/ will be scanned for updates.
     */
    public FeatureReplacementAction forServers(String... serverNames) {
        servers.remove(ALL_SERVERS);
        servers.addAll(Arrays.asList(serverNames));
        return this;
    }

    /**
     * Specify a list of server names to include in the feature replace action and any server configuration
     * files under "publish/clients/CLIENT_NAME/" will be scanned.
     * By default, all server config files in publish/clients/ will be scanned for updates.
     */
    public FeatureReplacementAction forClients(String... clientNames) {
        clients.remove(ALL_CLIENTS);
        clients.addAll(Arrays.asList(clientNames));
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
        boolean enabled = JavaInfo.forCurrentVM().majorVersion() >= minJavaLevel;
        if (!enabled)
            Log.info(c, "isEnabled", "Skipping action '" + toString() + "' because the java level is too low.");
        return enabled;
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
        for (String serverName : servers)
            serverConfigs.add(new File(pathToAutoFVTTestServers + serverName + "/server.xml"));

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
        for (String clientName : clients)
            clientConfigs.add(new File(pathToAutoFVTTestClients + clientName + "/client.xml"));

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
                    }
                    else if (features.remove(removeFeature)) {
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

    private static String getReplacementFeature(String originalFeature, Set<String> featuresToAdd) {
        // Example: servlet-3.1 --> servlet-4.0
        String featureBasename = originalFeature.substring(0, originalFeature.indexOf('-') + 1); // "servlet-"
        for (String featureToAdd : featuresToAdd)
            if (featureToAdd.contains(featureBasename)) // "servlet-4.0".contains("servlet-")
                return featureToAdd;
        // We may need to remove a feature without adding any replacement
        // (e.g. jsonb-1.0 is EE8 only) so in this case return null
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
