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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.ServerConfigurationFactory;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.JavaInfo;

/**
 * Test repeat action that removes and adds features during setup.
 */
public class FeatureReplacementAction implements RepeatTestAction {

    private static final Class<?> c = FeatureReplacementAction.class;

    static final String ALL_SERVERS = "ALL_SERVERS";

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
    protected String currentID = toString();
    private final Set<String> servers = new HashSet<>(Arrays.asList(ALL_SERVERS));
    private final Set<String> removeFeatures = new HashSet<>();
    private final Set<String> addFeatures = new HashSet<>();

    public FeatureReplacementAction(String removeFeature, String addFeature) {
        this();
        addFeature(addFeature);
        removeFeature(removeFeature);
        forceAddFeatures = true;
    }

    public FeatureReplacementAction(Set<String> removeFeatures, Set<String> addFeatures) {
        this();
        addFeatures(addFeatures);
        removeFeatures(removeFeatures);
    }

    public FeatureReplacementAction() {
        //no-op ... must call add/remove methods below before use
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
     * @param removeFeatures the features to be removed
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
     * @param removeFeature the feature to be removed
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

        // Find all of the server configurations to replace features in
        Set<File> serverConfigs = new HashSet<>();
        File serverFolder = new File(pathToAutoFVTTestServers);
        File filesFolder = new File(pathToAutoFVTTestFiles);
        if (servers.contains(ALL_SERVERS)) {
            // Find all *.xml in this test project
            serverConfigs.addAll(findFile(serverFolder, ".xml"));
            serverConfigs.addAll(findFile(filesFolder, ".xml"));
        } else {
            for (String serverName : servers)
                serverConfigs.add(new File(pathToAutoFVTTestServers + serverName));
        }

        // Make sure that XML file we find is a server config file, by checking if it contains the <server> tag
        Log.info(c, m, "Replacing features in files: " + serverConfigs.toString());

        // change all the server.xml files
        assertTrue("There were no servers (*.xml) in " + serverFolder.getAbsolutePath() + " and in " + filesFolder.getAbsolutePath()
                   + ". If you see this failure, what you need to do is add a simple server to publish/servers which includes the feature you're trying to test.",
                   serverConfigs.size() > 0);

        for (File serverConfig : serverConfigs) {
            if (!serverConfig.exists() || !serverConfig.canRead() || !serverConfig.canWrite()) {
                Log.info(c, m, "File did not exist or was not readable: " + serverConfig.getAbsolutePath());
                continue;
            }

            // Before we try to unmarshal the file, be sure it's a valid <server> config file
            boolean isServerConfig = false;
            try (Scanner s = new Scanner(serverConfig)) {
                while (!isServerConfig && s.hasNextLine())
                    isServerConfig = s.nextLine().contains("<server");
            }
            if (!isServerConfig) {
                Log.info(c, m, "File did not contain <server> so assuming it is not a valid server.xml" + serverConfig.getAbsolutePath());
                continue;
            }

            ServerConfiguration config = ServerConfigurationFactory.fromFile(serverConfig);
            Set<String> features = config.getFeatureManager().getFeatures();

            Log.info(c, m, "Original features:  " + features);
            if (forceAddFeatures) {
                features.removeAll(removeFeatures);
                features.addAll(addFeatures);
            } else {
                for (String removeFeature : removeFeatures)
                    if (features.remove(removeFeature)) {
                        // If we found a feature to remove that is actually present in server.xml, then
                        // remove it and replace it with the corresponding feature
                        String toAdd = getReplacementFeature(removeFeature, addFeatures);
                        if (toAdd != null)
                            features.add(toAdd);
                    }
            }
            Log.info(c, m, "Resulting features: " + features);

            ServerConfigurationFactory.toFile(serverConfig, config);
        }
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
        return currentID;
    }
}
