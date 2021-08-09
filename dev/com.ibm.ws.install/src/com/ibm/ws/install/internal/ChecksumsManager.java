/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.internal;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.kernel.boot.cmdline.Utils;

/**
 * This class contains APIs for checksum management.
 */
public class ChecksumsManager {

    private static final Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);

    /**
     * Class to hold Checksum data
     */
    private static class ChecksumData {

        Properties newChecksums;
        Map<String, Set<String>> existingChecksums;

        ChecksumData() {
            newChecksums = new Properties();
            existingChecksums = new HashMap<String, Set<String>>();
        }

        void registerNewChecksums(String fileName, String checksum) {
            newChecksums.put(fileName, checksum);
        }

        void registerExistingChecksums(String symbolicName, String fileName) {
            Set<String> checksums = existingChecksums.get(symbolicName);
            if (checksums == null) {
                checksums = new HashSet<String>();
                existingChecksums.put(symbolicName, checksums);
            }
            checksums.add(fileName);
        }

        boolean isIgnoredCSFile(File csFile) {
            for (String symbolicName : existingChecksums.keySet()) {
                String csFileName = csFile.getName();
                if (csFileName.equals(symbolicName + ".cs"))
                    return true;
            }
            return false;
        }
    }

    private final Map<File, ChecksumData> checksumsMap;

    /**
     * Constructor for ChecksumsManager
     */
    public ChecksumsManager() {
        checksumsMap = new HashMap<File, ChecksumData>();
    }

    /**
     * Updates the checkSumsMap for all entries.
     */
    public void updateChecksums() {
        for (Entry<File, ChecksumData> entry : checksumsMap.entrySet()) {
            updateChecksums(entry.getKey(), entry.getValue());
        }
    }

    /**
     *
     * @param checksumDir
     * @param checksumFiles
     */
    private void getCheckSumFiles(File checksumDir, final Collection<File> checksumFiles) {
        checksumDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                boolean accepted = pathname.getName().toLowerCase().endsWith(".cs");
                if (accepted)
                    checksumFiles.add(pathname);
                return accepted;
            }
        });
    }

    /**
     * Updates the check sum for a specific feature directory.
     *
     * @param featureDir the directory of the feature
     * @param checksumData the new checksum of the feature
     */
    public void updateChecksums(File featureDir, ChecksumData checksumData) {

        Collection<File> csFiles = new ArrayList<File>();
        File featureChecksumsDir = new File(featureDir, "checksums");
        getCheckSumFiles(featureChecksumsDir, csFiles);
        File libFeaturesChecksumsDir = new File(Utils.getInstallDir(), "lib/features/checksums");
        if (libFeaturesChecksumsDir.getAbsoluteFile().equals(featureChecksumsDir.getAbsoluteFile())) {
            File libPlatformChecksumsDir = new File(Utils.getInstallDir(), "lib/platform/checksums");
            getCheckSumFiles(libPlatformChecksumsDir, csFiles);
        }

        Properties existingChecksums = initChecksumProps(checksumData.existingChecksums);

        // Update existing cs files to new checksum
        for (File csFile : csFiles) {
            Properties csprops = loadChecksumFile(csFile);
            boolean modified = false;
            for (Entry<Object, Object> csEntry : csprops.entrySet()) {
                Object k = csEntry.getKey();
                if (checksumData.newChecksums.containsKey(k)) {
                    Object newChecksum = checksumData.newChecksums.get(k);
                    if (!csEntry.getValue().equals(newChecksum)) {
                        csprops.put(k, newChecksum);
                        modified = true;
                    }
                }
                if (!checksumData.isIgnoredCSFile(csFile) && existingChecksums.containsKey(k)) {
                    existingChecksums.put(k, csEntry.getValue());
                }
            }
            if (modified) {
                saveChecksumFile(csFile, csprops, " for new checksum.");
            }
        }

        // Update the new cs files to existing checksum
        for (Entry<String, Set<String>> entry : checksumData.existingChecksums.entrySet()) {
            String f = entry.getKey();
            File csFile = new File(featureChecksumsDir, f + ".cs");
            if (csFile.exists()) {
                Properties csprops = loadChecksumFile(csFile);
                boolean modified = false;
                for (String s : entry.getValue()) {
                    if (csprops.containsKey(s) && existingChecksums.containsKey(s)) {
                        String cs = (String) existingChecksums.get(s);
                        if (cs != null && !cs.isEmpty()) {
                            csprops.put(s, cs);
                            modified = true;
                        }
                    }
                }
                if (modified) {
                    saveChecksumFile(csFile, csprops, " for exising checksum.");
                }
            }
        }
    }

    /**
     * Populates a new property file with the inputed checksum map and empty strings.
     *
     * @param useOldChecksums a map of the old checksums
     * @return a new Properties object containing the old checksums as keys and "" as value
     */
    private Properties initChecksumProps(Map<String, Set<String>> useOldChecksums) {
        Properties checksumProps = new Properties();
        for (Set<String> set : useOldChecksums.values()) {
            for (String s : set) {
                checksumProps.put(s, "");
            }
        }
        return checksumProps;
    }

    /**
     * Loads the checksum file into a properties object
     *
     * @param csFile the checksum File
     * @return a new Properties object with the checksum data
     */
    public static Properties loadChecksumFile(File csFile) {
        InputStream csis = null;
        Properties csprops = new Properties();
        try {
            csis = new FileInputStream(csFile);
            csprops.load(csis);
        } catch (IOException e) {
            logger.log(Level.FINEST, "Failed to load the checksum file: " + csFile.getAbsolutePath(), e);
        } finally {
            InstallUtils.close(csis);
        }
        return csprops;
    }

    /**
     * Saves the checksum properties to csFile.
     *
     * @param csFile Output file
     * @param csprops Checksum properties file
     * @param reason the reason for saving the checksum file
     */
    public static void saveChecksumFile(File csFile, Properties csprops, String reason) {
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(csFile);
            csprops.store(fOut, null);
            logger.log(Level.FINEST, "Successfully updated the checksum file " + csFile.getAbsolutePath() + reason);
        } catch (Exception e) {
            logger.log(Level.FINEST, "Failed to save checksum file " + csFile.getAbsolutePath() + reason, e);
        } finally {
            InstallUtils.close(fOut);
        }
    }

    /**
     * Registers a new feature directory's new checksum
     *
     * @param featureDir the feature directory
     * @param fileName the filename for the checksum
     * @param checksum the checksum as a String
     */
    public void registerNewChecksums(File featureDir, String fileName, String checksum) {
        ChecksumData checksums = checksumsMap.get(featureDir);
        if (checksums == null) {
            checksums = new ChecksumData();
            checksumsMap.put(featureDir, checksums);
        }
        checksums.registerNewChecksums(fileName, checksum);
    }

    /**
     * Registers an existing feature directory's checksum
     *
     * @param featureDir the feature directory
     * @param symbolicName the symbolic name for the file
     * @param fileName the actual file name
     */
    public void registerExistingChecksums(File featureDir, String symbolicName, String fileName) {
        ChecksumData checksums = checksumsMap.get(featureDir);
        if (checksums == null) {
            checksums = new ChecksumData();
            checksumsMap.put(featureDir, checksums);
        }
        checksums.registerExistingChecksums(symbolicName, fileName);
    }
}
