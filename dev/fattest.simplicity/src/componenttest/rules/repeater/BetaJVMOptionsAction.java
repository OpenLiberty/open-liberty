/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FileUtils;

/**
 * Test repeat action that works with JVM Options adding a beta edition option and an other defined options.
 * Goes through all servers and appends to the jvm.options already specified or creates jvm.options on them.
 *
 * Be careful using this as it doesn't clean up the jvm options after running.
 * so that it interferes with other repeats. Could look more into using a RuleChain
 * in conjunction with RepeatAction to manage things more cleanly
 */
public class BetaJVMOptionsAction extends FeatureReplacementAction {
    private static final Class<?> c = BetaJVMOptionsAction.class;

    public static final String ID = "BETA_OPTIONS";

    private final Set<String> optionsToAdd;

    private static final String BETA_EDITION_OPTION = "-Dcom.ibm.ws.beta.edition=true";

    private final Set<File> optionFilesCreated = new HashSet<File>();

    private final Map<File, File> fileBackupMapping = new HashMap<File, File>();

    private boolean needsFeatureTransformation = false;

    public BetaJVMOptionsAction() {
        optionsToAdd = new HashSet<String>();
        optionsToAdd.add(BETA_EDITION_OPTION);
        withID(ID);
    }

    @Override
    public String toString() {
        return "Beta Options FAT repeat action (" + getID() + ")";
    }

    //

    @Override
    public BetaJVMOptionsAction addFeature(String addFeature) {
        needsFeatureTransformation = true;
        return (BetaJVMOptionsAction) super.addFeature(addFeature);
    }

    @Override
    public BetaJVMOptionsAction addFeatures(Set<String> addFeatures) {
        needsFeatureTransformation = true;
        return (BetaJVMOptionsAction) super.addFeatures(addFeatures);
    }

    @Override
    public BetaJVMOptionsAction removeFeature(String removeFeature) {
        needsFeatureTransformation = true;
        return (BetaJVMOptionsAction) super.removeFeature(removeFeature);
    }

    @Override
    public BetaJVMOptionsAction removeFeatures(Set<String> removeFeatures) {
        needsFeatureTransformation = true;
        return (BetaJVMOptionsAction) super.removeFeatures(removeFeatures);
    }

    @Override
    public BetaJVMOptionsAction withID(String id) {
        // TODO Auto-generated method stub
        return (BetaJVMOptionsAction) super.withID(id);
    }

    public BetaJVMOptionsAction withOptions(String... jvmOptions) {
        optionsToAdd.addAll(Arrays.asList(jvmOptions));
        return this;
    }

    @Override
    public void setup() throws Exception {
        final String m = "setup";

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
                    Log.info(c, m, "Succesfully created jvm.options in: " + serverName);
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
            Set<File> optionsFound = findFile(new File(pathToAutoFVTTestServers + clientName), "jvm.options");
            // If options file doesn't exist
            if (optionsFound.isEmpty()) {
                // Create it
                File jvmOptionsCreated = new File(pathToAutoFVTTestServers + clientName + "/jvm.options");
                if (jvmOptionsCreated.createNewFile()) {
                    Log.info(c, m, "Succesfully created jvm.options in: " + clientName);
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
                fileBackupMapping.put(optionsFile, backupFile.toFile());

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

        // Transform server.xml's if features were actually added
        if (needsFeatureTransformation)
            super.setup();
    }

    @Override
    public void cleanup() {
        // Undo changes done to jvm.options
        for (Entry<File, File> mapping : fileBackupMapping.entrySet()) {
            File original = mapping.getKey();
            File backup = mapping.getValue();
            Log.info(c, "cleanup", "Restoring " + original + " from " + backup);
            try {
                FileUtils.recursiveDelete(original);
                FileUtils.copyDirectory(backup, original);
            } catch (Exception e) {
                throw new RuntimeException("Exception restoring backup for app " + original, e);
            }
        }
        fileBackupMapping.clear();
    }

}
