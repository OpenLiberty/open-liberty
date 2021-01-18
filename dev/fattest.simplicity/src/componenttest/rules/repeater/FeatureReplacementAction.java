/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.config.ClientConfiguration;
import com.ibm.websphere.simplicity.config.ClientConfigurationFactory;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.ServerConfigurationFactory;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test repeat action that removes and adds features during setup.
 * 
 * Used for updating server features when repeating tests using
 * different Java EE levels.
 * 
 * The repeat action may be set to run in particular Java versions
 * and in particular framework test modes.
 * 
 * See {@link #withMinJavaLevel(int)}.
 *
 * See also {@link #fullFATOnly} and {@link #withTestMode(TestMode).
 */
public class FeatureReplacementAction implements RepeatTestAction {
    private static final Class<? extends FeatureReplacementAction> c =
        FeatureReplacementAction.class;

    /**
     * Feature replacement action factory method: Create a feature
     * replacement action which replaces JavaEE 8 features with the
     * JavaEE 7 equivalent features.
     * 
     * See {@link EE7FeatureReplacementAction}.
     * 
     * @return A feature replacement action which replaces JavaEE 8
     *     features with JavaEE 7 features.
     */
    public static FeatureReplacementAction EE7_FEATURES() {
        return new EE7FeatureReplacementAction();
    }

    /**
     * Feature replacement action factory method: Create a feature
     * replacement action which replaces JavaEE 7 features with the
     * JavaEE 8 equivalent features.
     * 
     * See {@link EE8FeatureReplacementAction}.
     * 
     * @return A feature replacement action which replaces JavaEE 7
     *     features with JavaEE 8 features.
     */
    public static FeatureReplacementAction EE8_FEATURES() {
        return new EE8FeatureReplacementAction();
    }

    /**
     * Feature replacement action factory method: Create a feature
     * replacement action which replaces JavaEE 7 and JavaEE 8 features
     * with the JavaEE 8 equivalent features.
     * 
     * See {@link EE9FeatureReplacementAction}.
     * 
     * @return A feature replacement action which replaces JavaEE 7
     *     and JavaEE 8 features with JavaEE 9 features. 
     */
    public static FeatureReplacementAction EE9_FEATURES() {
        return new JakartaEE9Action();
    }

    /**
     * Feature replacement action factory method: Create a feature
     * replacement action which performs no replacements.
     * 
     * See {@link EmptyAction}.
     *
     * @return A feature replacement action which performs no replacements.
     */
    public static EmptyAction NO_REPLACEMENT() {
        return new EmptyAction();
    }
    
    public FeatureReplacementAction() {
        // Empty
    }

    @Override
    public String toString() {
        boolean doRemove = ( (removeFeatures != null) && !removeFeatures.isEmpty() );
        boolean doAdd = ( (addFeatures != null) && !addFeatures.isEmpty() );

        String printString = getClass().getSimpleName();

        if ( doRemove ) {
            if ( doAdd ) {
                printString += "(remove " + removeFeatures + ", add " + addFeatures + ")";                                
            } else {
                printString += "(remove " + removeFeatures + ")";                
            }
        } else if ( doAdd ) {
            printString += "(add " + addFeatures + ")";
        } else {
            printString += "() [No replacements]";
        }

        return printString;
    }

    @Override
    public boolean isEnabled() {
        String methodName = "isEnabled";

        int majorVersion = JavaInfo.forCurrentVM().majorVersion();
        
        if ( majorVersion < minJavaLevel ) {
            Log.info(c, methodName,
                "Skipping action " + toString() + "." +
                "  The current java version " + majorVersion +
                " is less than the repeat action version " + minJavaLevel + ".");
            return false;
        }

        if (TestModeFilter.FRAMEWORK_TEST_MODE.compareTo(testRunMode) < 0) {
            Log.info(c, methodName,
                "Skipping action " + toString() + "." +
                "  The test mode " + testRunMode + " is not valid" +
                " for framework test mode " + TestModeFilter.FRAMEWORK_TEST_MODE + ".");
            return false;
        }

        return true;
    }
    
    //

    private String currentID;    

    public FeatureReplacementAction withID(String id) {
        currentID = id;
        return this;
    }

    @Override
    public String getID() {
        if ( currentID != null ) {
            return currentID;
        } else {
            return toString();
        }
    }

    //

    // These must maintain the addition order:
    //
    // Multiple matching replacements can be present in the
    // feature lists.  The matching rule is that the last
    // match is taken.  That requires that the feature lists
    // be kept in order.
    //
    // TFB: TODO: The effect of ordering 'removeFeatures' is not clear.

    private final Set<String> removeFeatures = new LinkedHashSet<>();
    private final Set<String> addFeatures = new LinkedHashSet<>();

    private final Set<String> alwaysAddFeatures = new HashSet<>();
    private final Set<String> serverConfigPaths = new HashSet<>();

    private boolean forceAddFeatures = true;

    /**
     * Set the feature addition mode.
     * 
     * When true (the default), all features which are specified to be
     * added regardless of the presence of a corresponding feature in
     * the feature configuration and in the remove features list.
     * 
     * When false, feature addition is smarter, and will only add a feature
     * if the feature is present in the server configuration and present
     * in the remove features list.
     */
    public FeatureReplacementAction forceAddFeatures(boolean force) {
        forceAddFeatures = force;
        return this;
    }

    /**
     * Create a feature replacement action which removes one feature
     * and add one feature.
     *
     * The action will add the feature even if the feature was not original
     * present.
     *
     * @param removeFeature The feature which is to be removed.
     * @param addFeature The feature which is to be added.
     */
    public FeatureReplacementAction(String removeFeature, String addFeature) {
        addFeature(addFeature);
        removeFeature(removeFeature);
    }

    /**
     * Create a feature replacement action which removes features
     * and add features.
     *
     * The action will add features even if no corresponding features
     * are removed.
     *
     * @param removeFeatures The features which are to be removed.
     * @param addFeatures The feature which are to be added.
     */
    public FeatureReplacementAction(Set<String> removeFeatures, Set<String> addFeatures) {
        addFeatures(addFeatures);
        removeFeatures(removeFeatures);
    }

    /**
     * Create a feature replacement action which adds a set of features.
     *
     * @param addFeatures The features which are to be added.
     */
    public FeatureReplacementAction(Set<String> addFeatures) {
        addFeatures(addFeatures);
    }

    /**
     * Add a single feature
     *
     * @param addFeature The feature which is to be added.
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
        addFeatures.forEach( this::addFeature );
        return this;
    }

    /**
     * Add features to the set to be added - these features will always be
     * added to the server configuration, even if {@code forceAddFeatures}
     * is false.
     *
     * @param alwaysAddedFeatures the features to be added regardless of
     *     {@code forceAddFeatures}
     */
    public FeatureReplacementAction alwaysAddFeatures(Set<String> alwaysAddedFeatures) {
        alwaysAddedFeatures.forEach( this::alwaysAddFeature );
        return this;
    }

    /**
     * Add a feature to the set to be added - this features will always be
     * added to the server configuration, even if {@code forceAddFeatures}
     * is false.
     *
     * @param alwaysAddedFeature the feature to be added regardless of
     *     {@code forceAddFeatures}
     */
    public FeatureReplacementAction alwaysAddFeature(String alwaysAddedFeature) {
        alwaysAddFeatures.add(alwaysAddedFeature);
        return this;
    }

    /**
     * Add features to the set to be removed
     *
     * To be clear, this is not the opposite of {@link #addFeatures}.
     *
     * @param removeFeatures the features to be removed. Wildcards are supported.
     */
    public FeatureReplacementAction removeFeatures(Set<String> removeFeatures) {
        removeFeatures.forEach( this::removeFeature );
        return this;
    }

    /**
     * Add a feature to the set to be added.
     *
     * @param addFeature the feature to be added
     */
    public FeatureReplacementAction addFeature(String addFeature) {
        addFeatures.add(addFeature);
        return this;
    }

    /**
     * Add a feature to the set to be removed
     *
     * To be clear, this is not the opposite of {@link #addFeature}.
     *
     * @param removeFeature the feature to be removed. Wildcards are supported.
     */
    public FeatureReplacementAction removeFeature(String removeFeature) {
        removeFeatures.add( removeFeature.toLowerCase() );
        return this;
    }

    //

    private int minJavaLevel = 8;
    
    public FeatureReplacementAction withMinJavaLevel(int javaLevel) {
        this.minJavaLevel = javaLevel;
        return this;
    }

    //

    private TestMode testRunMode = TestMode.LITE;
    
    public FeatureReplacementAction fullFATOnly() {
        testRunMode = TestMode.FULL;
        return this;
    }

    public FeatureReplacementAction withTestMode(TestMode mode) {
        testRunMode = mode;
        return this;
    }

    //

    public static final String ALL_SERVERS = "ALL_SERVERS";
    public static final String ALL_CLIENTS = "ALL_CLIENTS";
    public static final String NO_SERVERS = "NO_SERVERS";
    public static final String NO_CLIENTS = "NO_CLIENTS";

    private final Set<String> servers;
    {
        servers = new HashSet<String>(1);
        servers.add(ALL_SERVERS);
    }

    private boolean calledForServers;
    private boolean calledForServerConfigPaths;

    private final Set<String> clients;
    {
        clients = new HashSet<>(1);
        clients.add(ALL_CLIENTS);
    }
    
    /**
     * Specify a list of server names which will be updated.
     * 
     * By default, all server configuration files under
     * "publish/servers/" and "publish/files/" are updated.
     *
     * Call only one of {@link #forServers(String...)} and
     * {@link #forServerConfigPaths(String...)}. An
     * {@link IllegalStateException} will be thrown if both
     * are called.
     */
    public FeatureReplacementAction forServers(String... serverNames) {
        if ( calledForServerConfigPaths ) {
            throw new IllegalStateException("Use only one of forServers(...) and forServerConfigPaths(...)");
        }

        if ( NO_SERVERS.equals( serverNames[0]) ) {
            servers.clear();

        } else {
            servers.remove(ALL_SERVERS);
            servers.addAll( Arrays.asList(serverNames) );
        }

        calledForServers = true;
        
        return this;
    }

    /**
     * Overrides the paths to search for server XML files
     * which will be altered by this feature replacement
     * action. The defaults are "publish/servers" and "publish/files".
     *
     * Call only one of {@link #forServers(String...)} and
     * {@link #forServerConfigPaths(String...)}. An
     * {@link IllegalStateException} will be thrown if both
     * are called.
     */
    public FeatureReplacementAction forServerConfigPaths(String... serverPaths) {
        if ( calledForServers ) {
            throw new IllegalStateException(
                "Use only one of forServers(...) and forServerConfigPaths(...)");
        }

        Log.info(c, "forServerConfigPaths",
            "Adding the following server configuration paths: " + Arrays.toString(serverPaths));

        servers.remove(ALL_SERVERS);

        if ( (serverPaths != null) && (serverPaths.length > 0) ) {
            for ( String path : serverPaths ) {
                if ( path.startsWith("publish/files") ) {
                    path = path.replace("publish/files", "lib/LibertyFATTestFiles");
                }

                File f = new File(path);
                if ( !f.exists() ) {
                    throw new IllegalArgumentException(
                        "The path specified in the forServerConfigPaths(...) does not exist: " +
                        f.getAbsolutePath());
                }

                serverConfigPaths.add(path);
            }

            calledForServerConfigPaths = true;
        }

        return this;
    }

    /**
     * Specify a list of client names which will be updated.
     *
     * By default, all configuration files under "publish/clients/"
     * are updated.
     */
    public FeatureReplacementAction forClients(String... clientNames) {
        if ( NO_CLIENTS.equals( clientNames[0] ) ) {
            clients.clear();
        } else {
            clients.remove(ALL_CLIENTS);
            clients.addAll( Arrays.asList(clientNames) );
        }
        return this;
    }

    //

    private static final String testFilesPath = "lib/LibertyFATTestFiles/";
    private static final File testFilesFolder = new File(testFilesPath);

    private static final String serversPath = "publish/servers/";
    private static final File serversFolder = new File(serversPath);

    private static final String clientsPath = "publish/clients/";
    private static final File clientsFolder = new File(clientsPath);

    @Override
    public void setup() throws Exception {
        String methodName = "setup";

        assertFalse("No features were set to be added or removed",
                    (addFeatures.isEmpty() && removeFeatures.isEmpty()) );

        // First, locate server configurations ...
        
        Set<File> serverConfigs = new HashSet<>();
        Set<File> locationsChecked = new HashSet<>();

        if ( !calledForServerConfigPaths ) {
            Log.info(c, methodName, "Locating server configuration files: " + servers);

            locationsChecked.add(testFilesFolder);
            locationsChecked.add(serversFolder);

            if ( servers.contains(ALL_SERVERS) ) {
                servers.remove(ALL_SERVERS);

                serverConfigs.addAll( findFile(testFilesFolder, ".xml") );

                if ( serversFolder.exists() ) {
                    for ( File serverFolder : serversFolder.listFiles()) {
                        if ( serverFolder.isDirectory() ) {
                            servers.add( serverFolder.getName() );
                        }
                    }
                }
            }

            for ( String serverName : servers ) {
                serverConfigs.addAll( findFile(new File(serversFolder, serverName), ".xml") );
            }

        } else {
            Log.info(c, methodName, "Locating server configuration files: " + serverConfigPaths);

            for ( String path : serverConfigPaths ) {
                File f = new File(path);
                if ( !f.exists() ) {
                    throw new IllegalStateException(
                            "Server configuration path no longer exists: " + f.getAbsolutePath());
                }
                
                if ( f.isDirectory() ) {
                    locationsChecked.add(f); // Only need to add and not the flat file.
                    serverConfigs.addAll( findFile(f, ".xml") );
                } else {
                    serverConfigs.add(f);
                }
            }
        }

        // Next, locate client configurations.

        Set<File> clientConfigs = new HashSet<>();

        if ( clients.contains(ALL_CLIENTS) ) {
            clients.remove(ALL_CLIENTS);
            
            locationsChecked.add(clientsFolder);

            clientConfigs.addAll(findFile(clientsFolder, ".xml"));
            if ( clientsFolder.exists() ) {
                for ( File f : clientsFolder.listFiles() ) {
                    if ( f.isDirectory() ) {
                        clients.add(f.getName());
                    }
                }
            }
        }

        for ( String clientName : clients ) {
            // TFB: TODO: Should this be 'serversFolder' or 'clientsFolder' ???
            clientConfigs.addAll( findFile(new File(serversFolder, clientName), ".xml") );
        }

        Log.info(c, methodName,
            "Replacing features in server configurations: " + serverConfigs +
            "  and  " + clientConfigs);

        assertTrue("No configurations were located.",
                   ( !serverConfigs.isEmpty() || !clientConfigs.isEmpty() ));

        // Put all of the located configurations into a single collection.

        Set<File> configurations = new HashSet<>();
        configurations.addAll(clientConfigs);
        configurations.addAll(serverConfigs);

        // Validate that each exists, can be read, and can be written.
        
        for ( File config : configurations ) {
            String configPath = config.getAbsolutePath();
            boolean skip = false;
            if ( !config.exists() ) {
                Log.info(c, methodName, "Configuration does not exist; ignoring: " + configPath);
                skip = true;
            }
            if ( !config.canRead() ) {
                Log.info(c, methodName, "Configuration cannot be read; ignoring: " + configPath);
                skip = true;
            }
            if ( !config.canWrite() ) {
                Log.info(c, methodName, "Configuration cannot be read; ignoring: " + configPath);
                skip = true;
            }
            if ( skip ) {
                continue;
            }

            boolean isServerConfig = false;
            boolean isClientConfig = false;
            try ( Scanner s = new Scanner(config) ) {
                while ( !isServerConfig && s.hasNextLine() ) {
                    String line = s.nextLine();
                    if ( line.contains("<server") ) {
                        isServerConfig = true;
                    } else if ( line.contains("<client") ) {
                        isClientConfig = true;
                    }
                }
            }
            if ( !isServerConfig && !isClientConfig ) {
                Log.info(c, methodName,
                    "Configuration contains neither nor <server> or <client>; ignoring: " + configPath);
                continue;
            }

            // Load the configuration and extract its features.
            //
            // 'initialFeatures' holds a reference to the actual features
            // collection of the configuration.  Updates are made back
            // to the configuration by updating this collection.

            ServerConfiguration serverConfig;
            ClientConfiguration clientConfig;
            Set<String> liveConfigFeatures;
            if ( isServerConfig ) {
                serverConfig = ServerConfigurationFactory.fromFile(config);
                clientConfig = null;
                liveConfigFeatures = serverConfig.getFeatureManager().getFeatures();                
            } else { // ( isClientConfig )
                serverConfig = null;
                clientConfig = ClientConfigurationFactory.fromFile(config);
                liveConfigFeatures = clientConfig.getFeatureManager().getFeatures();
            }

            List<String> replacementFeatures = new ArrayList<String>( liveConfigFeatures.size() );
            
            // Do all feature work using the lower case feature names.

            for ( String feature : liveConfigFeatures ) {
                replacementFeatures.add( feature.toLowerCase() );
            }
            replacementFeatures.sort( String::compareTo );
            Log.info(c, methodName, "Initial features: " + replacementFeatures);

            // Remove then add features.
            //
            // Feature removal happens unconditionally, and has two steps:
            // Direct feature removal, and wildcard feature removal.
            //
            // Feature addition occurs depending on the 'forceAddFeatures'
            // setting.  When forced additions are enabled, feature addition
            // happens regardless of whether a corresponding feature was
            // removed.  When forced additions are disabled, feature addition
            // happens only for features which were removed.  A feature is
            // not added unless a corresponding feature was removed.

            if ( forceAddFeatures ) {
                replacementFeatures.removeAll(removeFeatures);

                for ( String removeFeature : removeFeatures ) {
                    if ( removeFeature.endsWith("*") ) {
                        removeWildcardFeature(replacementFeatures, removeFeature);
                    }
                }

                replacementFeatures.addAll(addFeatures);

            } else {
                for ( String removeFeature : removeFeatures ) {
                    boolean removed = false;
                    if ( removeFeature.endsWith("*") ) {
                        removed = removeWildcardFeature(replacementFeatures, removeFeature);
                    } else if ( replacementFeatures.remove(removeFeature) ) {
                        removed = true;
                    } else {
                        // The feature was not removed
                    }

                    // Only add a replacement feature for features which were removed.

                    if ( removed ) {
                        // Logging in 'getReplacementFeature'.
                        String replacement = getReplacementFeature(removeFeature, addFeatures);
                        if ( replacement != null ) {
                            replacementFeatures.add(replacement);
                        }
                    }
                }
            }

            // Finally, a number of features may be always added.  The
            // 'forceAddFeatures' setting does not matter.

            replacementFeatures.addAll(alwaysAddFeatures);

            // Update the live features ...
            //
            // This is an ugly way to do the update, but no better API
            // is available.
            //
            // The live features collection is a set: Duplicates are automatically
            // removed.

            liveConfigFeatures.clear();
            liveConfigFeatures.addAll(replacementFeatures);
            Log.info(c, methodName, "Updated features: " + liveConfigFeatures);

            if ( isServerConfig ) {
                Log.info(c, methodName, "Server configuration: " + serverConfig);
                ServerConfigurationFactory.toFile(config, serverConfig);
            } else {
                Log.info(c, methodName, "Client configuration: " + clientConfig);                
                ClientConfigurationFactory.toFile(config, clientConfig);
            }
        }

        // Make sure the updated configurations are pushed to
        // the liberty installation.

        for ( String serverName : servers ) {
            LibertyServerFactory.getLibertyServer(serverName);
        }

        for ( String clientName : clients ) {
            LibertyClientFactory.getLibertyClient(clientName);
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
     * @param originalFeature The feature name which is to be replaced.
     * @param replacementFeatures Table of replacement features.
     *
     * @return The replacement feature name. Null if no replacement is available.
     */
    private static String getReplacementFeature(
        String originalFeature, Set<String> replacementFeatures) {

        String methodName = "getReplacementFeature";

        // Example: servlet-3.1 --> servlet-4.0

        int dashOffset = originalFeature.indexOf('-');
        if ( dashOffset == -1 ) {
            throw new IllegalArgumentException(
                "Feature " + originalFeature + " is not valid: No '-' was found.");
        }

        // "servlet-3.1" ==> "servlet-"
        String baseFeature = originalFeature.substring(0, dashOffset + 1);
        
        // Keep the base feature all lower case.  This avoids a lot
        // of 'toLowerCase' calls.
        baseFeature = baseFeature.toLowerCase();

        // "servlet-4.0".startsWith("servlet-")
        
        // If multiple versions of a feature listed in the replacement list, it will take the last
        // version by looping through all of the features instead of stopping on the first that matches.
        // The Set is a LinkedHashSet so it will iterate over them in the order that add was called.        

        String replacementFeature = null;

        for ( String candidateFeature : replacementFeatures ) {
            if ( candidateFeature.toLowerCase().startsWith(baseFeature) ) {
                replacementFeature = candidateFeature;
            }
        }

        if ( replacementFeature != null ) {
            Log.info(c, methodName,
                    "Replace feature " + originalFeature + " with " + replacementFeature);
            return replacementFeature;
        }

        // We need to check that the feature passed is an EE7/EE8 feature which
        // could have a name change on EE9 that doesn't match the original feature
        // name it replaces. We also check vice versa if the feature is an EE9
        // feature that involves a name change to update from EE9 to EE7/EE8.

        Log.info(c, methodName,
            "No feature replacement found for " + originalFeature + "." +
            " Testing if the feature name was changed in EE9.");

        // Remove the '-' from the base feature.

        // "servlet-" ==> "servlet"        
        baseFeature = baseFeature.substring(0, dashOffset);

        String featureReplacement = null;
        for ( Map.Entry<String, String> nameChangeEntry : JakartaEE9Action.getFeaturesWithNameChange().entrySet() ) {
            if ( nameChangeEntry.getValue().equalsIgnoreCase(baseFeature) ) {
                featureReplacement = nameChangeEntry.getKey();
                Log.info(c, methodName,
                    "Replace base EE9 feature " + baseFeature + " with base feature " + featureReplacement);
                baseFeature = featureReplacement.toLowerCase(); // Kkeep the base feature lower case!
                break;
            }

            if ( nameChangeEntry.getKey().equalsIgnoreCase(baseFeature) ) {
                featureReplacement = nameChangeEntry.getValue();
                Log.info(c, methodName,
                    "Replace base feature " + baseFeature + " with base EE9 feature " + featureReplacement);
                baseFeature = featureReplacement.toLowerCase(); // Keep the base feature lower case!
                break;
            }
        }

        if ( featureReplacement == null ) {
            Log.info(c, methodName,
                "Remove feature " + originalFeature + ": No replacement is available");
            return null;
        }

        baseFeature += "-";

        // Re-check the features with the name changes
        for ( String candidateFeature : replacementFeatures ) {
            if ( candidateFeature.toLowerCase().contains(baseFeature) ) {
                Log.info(c, methodName,
                    "Replace feature " + originalFeature + " with " + candidateFeature);
                return candidateFeature;
            }
        }

        // We may need to remove a feature without adding any replacement
        // (e.g. jsonb-1.0 is EE8 only) so in this case return null
        Log.info(c, methodName,
            "Remove feature " + originalFeature + ": No replacement is available");
        return null;
    }

    /**
     * Remove features which match a specified feature.
     * 
     * The specified feature must end with an asterisk '*'.
     * Features which match the specified feature up to but
     * not including the asterisk are removed.
     * 
     * Removal is performed directly on the argument features
     * list.
     * 
     * @param features The features from which to remove matches.
     * @param removeFeature The feature to remove.
     *
     * @return True or false telling if any features were removed.
     */
    private boolean removeWildcardFeature(List<String> features, String removeFeature) {
        // Don't match the '*'!
        int matchLength = removeFeature.length() - 1;

        boolean removed = false;

        Iterator<String> iterator = features.iterator();
        while ( iterator.hasNext() ) {
            String feature = iterator.next();
            if ( feature.regionMatches(0, removeFeature, 0, matchLength) ) {
                iterator.remove();
                removed = true;
            }
        }

        return removed;
    }

    /**
     * Recursively select files having a specified suffix.
     * 
     * Descend into directories, but do not select them.
     *
     * A selected file will have its paths set by appending
     * its relative path to the path of the selection directory. 
     *
     * @param dir The root selection directory.
     * @param suffix The suffix to select.
     *
     * @return The simple files within the directory and its
     *     subdirectories which have the specified suffix.
     */
    private Set<File> findFile(File dir, String suffix) {
        HashSet<File> selected = new HashSet<File>();
        File[] list = dir.listFiles();
        if ( list != null ) {
            for ( File file : list ) {
                if ( file.isDirectory() ) {
                    selected.addAll( findFile(file, suffix) );
                } else if ( file.getName().endsWith(suffix) ) {
                    selected.add(file);
                }
            }
        }
        return selected;
    }

    /**
     * Evaluate the statement in the context of this repeat action.
     * 
     * Do activate the repeat action, and do particular setup for this
     * action.
     *
     * @param statement The statement which is to be evaluated.
     * 
     * @throws Throwable Thrown by the statement.
     */
    @Override
    public void evaluate(Statement statement) throws Throwable {
        RepeatTestFilter.activateRepeatAction( getID() );                        

        try {
            setup();
            statement.evaluate(); // throws Throwable

        } finally {
            RepeatTestFilter.deactivateRepeatAction();
        }
    }    
}
