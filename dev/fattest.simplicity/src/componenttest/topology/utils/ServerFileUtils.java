/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils;

import java.io.File;
import java.nio.file.Files;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

/**
 * Expands imported files within a Liberty server xml.
 * The server configs (that include "imports" are assumed to be found in:
 * publish/servers/<server>/configs/
 * These config files will have imports located in
 * publish/shared/config/ of a FAT project
 *
 * @author chrisc
 *
 */
public class ServerFileUtils {

    protected static Class<?> thisClass = ServerFileUtils.class;
    private final String serverConfigDir = "configs";
    private final String sharedConfigDir = "config";
    private final String serverBackupDir = "serverConfigBackups";

    public String expandAndBackupCfgFile(LibertyServer server, String cfgFile) throws Exception {
        return expandAndBackupCfgFile(server, cfgFile, null);
    }

    public String expandAndBackupCfgFile(LibertyServer server, String cfgFile, String inTestName) throws Exception {

        String fixedCfgFileName = cfgFile;
        if (cfgFile == null) {
            throw new Exception("Requested configuration file can not be null");
        }

        if (!(cfgFile.startsWith("/" + serverConfigDir + "/") || cfgFile.startsWith(serverConfigDir + "/"))) {
            fixedCfgFileName = serverConfigDir + "/" + cfgFile;
        }
        String newCfgFile = expandCfgFile(server, fixedCfgFileName);
        backupCfgFile(server, newCfgFile, inTestName);
        return newCfgFile;

    }

    public String expandCfgFile(LibertyServer server, String cfgFile) throws Exception {

        String newCfgFile = cfgFile;
        CommonMergeTools merge = new CommonMergeTools();
        if (merge.mergeFile(server.getServerRoot() + "/" + cfgFile, server.getServerSharedPath() + sharedConfigDir, server.getServerRoot() + "/")) {
            newCfgFile = cfgFile.replace(".xml", "_Merged.xml");
        }
        return server.getServerRoot() + "/" + newCfgFile;
    }

    public void backupCfgFile(LibertyServer server, String cfgFile, String inTestName) throws Exception {

        String thisMethod = "backupCfgFile";
        String testName = inTestName;
        if (inTestName == null) {
            testName = "default";
        }

        File testServerDir = getServerBackupDir(server);
        String backupConfigName = testName + "_server.xml";
        Log.info(thisClass, thisMethod, "Backing up server.xml for test: " + testName + " to " + testServerDir.toString() + "/" + backupConfigName);
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), testServerDir.toString(), backupConfigName, cfgFile);
    }

    /**
     * Builds the fully qualified runtime server path.
     */
    public String getServerFileLoc(LibertyServer server) throws Exception {
        try {
            return (new File(server.getServerConfigurationPath().replace('\\', '/'))).getParent();
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
    }

    /**
     * Builds the fully qualified path of the {@code <Install>/build.image/wlp/usr/servers/<serverName>/serverConfigBackups}
     * directory
     */
    public File getServerBackupDir(LibertyServer server) throws Exception {
        try {
            File testServerDir = new File(getServerFileLoc(server) + "/" + serverBackupDir);
            if (!Files.isDirectory(testServerDir.toPath())) {
                testServerDir.mkdir();
            }
            Log.info(thisClass, "getServerBackupDir", "serverBackupDir: " + testServerDir.toString());
            return testServerDir;
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
    }

}
