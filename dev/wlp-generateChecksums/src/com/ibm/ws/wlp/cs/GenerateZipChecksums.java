/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wlp.cs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * GenerateChecksums
 */
public class GenerateZipChecksums extends Task {
    //ant task attributes
    private File installRoot;
    private final String checksumsDirName = "checksums";
    private final boolean ignoreBinFiles = false;

    //the ext name of all the checksum files
    private static String MD5_FILE_EXT = "md5";
    private static String SHA2_FILE_EXT = "sha2";

    @Override
    public void execute() {

        try {
            // generate dist zips checksums
            generateZipChecksums(installRoot, new File(installRoot, checksumsDirName));

        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /**
     * @param dir
     * @param checksumsDir
     * @throws IOException
     */
    private void generateZipChecksums(File dir, File checksumsDir) throws IOException {
        checksumsDir.mkdirs();
        File[] zipFiles = dir.listFiles();
        for (File zipFile : zipFiles) {
            if (!zipFile.getName().endsWith(".zip")) {
                continue;
            }
            //generate the cs file from each packaged zip
            String md5Checksum = MD5Utils.getFileMD5String(zipFile);
            String sha2Checksum = SHA2Utils.getFileSHA2String(zipFile);
            String fileName = zipFile.getName();
            //Create MD5 file
            File md5File = new File(checksumsDir, fileName + "." + MD5_FILE_EXT);
            OutputStream out = null;
            try {
                out = new FileOutputStream(md5File, false);
                byte checksumbytes[] = md5Checksum.getBytes();
                out.write(checksumbytes);
            } finally {
                FileUtils.tryToClose(out);
            }
            //Create SHA2 file
            File sha2File = new File(checksumsDir, fileName + "." + SHA2_FILE_EXT);
            try {
                out = new FileOutputStream(sha2File, false);
                byte checksumbytes[] = sha2Checksum.getBytes();
                out.write(checksumbytes);
            } finally {
                FileUtils.tryToClose(out);
            }
        }
    }

    // getters and setters
    public File getInstallRoot() {
        return installRoot;
    }

    public void setInstallRoot(File installRoot) {
        this.installRoot = installRoot;
    }
}
