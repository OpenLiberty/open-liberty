/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.ibm.websphere.simplicity.log.Log;

/**
 *
 */
public class Utils {
    private static final Class<?> c = Utils.class;

    /**
     * Make a copy of a file in the same directory. Add the suffix "_backup"
     * to the destination file name.
     *
     * @param fileName
     */
    public static void backupFile(String fileName) {
        final String METHOD_NAME = "backupFile";
        File originalFile = new File(fileName);

        if (!originalFile.exists()) {
            Log.info(c, METHOD_NAME, "The file [" + fileName + "] does not exist. ");
            return;
        }

        String parentDirectory = originalFile.getParent();
        String originalFileName = originalFile.getName();
        String backupFileName = originalFileName + "_backup";

        File backupFile = new File(parentDirectory, backupFileName);

        try {
            Files.copy(originalFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Log.info(c, METHOD_NAME, "Backup file created: " + backupFile.getAbsolutePath());
        } catch (IOException e) {
            Log.info(c, METHOD_NAME, "Failed to create the backup file: " + e.getMessage());
        }
    }

    /**
     * Given a file named '<someFileName.ext>', if a file in the same directory
     * with name <someFileName.ext>_backup exists, copy the backup file to the
     * to the original file name.
     *
     * @param fileName
     */
    public static void restoreFileFromBackup(String fileName) {
        final String METHOD_NAME = "restoreFileFromBackup";
        File backupFile = new File(fileName + "_backup");

        if (!backupFile.exists()) {
            Log.info(c, METHOD_NAME, "The backup [" + backupFile.getName() + "] file does not exist");
            return;
        }

        File originalFile = new File(fileName);

        try {
            Files.copy(backupFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Log.info(c, METHOD_NAME, "Backup file restored to: " + originalFile.getAbsolutePath());
        } catch (IOException e) {
            Log.info(c, METHOD_NAME, "Failed to restore the backup file: " + e.getMessage());
        }
    }

    public static void copyFile(File sourceFile, String destinationFilePath) throws IOException {
        final String METHOD_NAME = "copyFile";
        Log.info(c, METHOD_NAME, "ENTER \n[" + sourceFile.getAbsolutePath() + "]\n[" + destinationFilePath + "]");
        try (
                        FileInputStream inputStream = new FileInputStream(sourceFile);
                        FileOutputStream outputStream = new FileOutputStream(destinationFilePath)) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     *
     * @param filePath     - absolute path of file to create
     * @param fileContents - contents to put in the newly created server.env file
     * @return
     */
    public static File createFile(String filePath, String fileContents) {
        final String METHOD_NAME = "createFile";
        Log.info(c, METHOD_NAME, "ENTER, file to create [{0}]\n", filePath);

        File fileToCreate = new File(filePath);
        File parentDir = fileToCreate.getParentFile();
        if (parentDir == null) {
            String parentDirFromPath = extractParentDirFromPath(filePath);
            parentDir = new File(parentDirFromPath);
        }
        String path = fileToCreate.getAbsolutePath();
        Log.info(c, METHOD_NAME, "Creating file [{0}] with contents \n[\n{1}\n]", new Object[] { path, fileContents });

        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileToCreate);
            fos.write(fileContents.getBytes());
        } catch (IOException ioe) {
            Log.info(c, METHOD_NAME, "Caught exception while writing the file " + path);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ioe) {
                    Log.info(c, METHOD_NAME, "Caught exception while closing the file " + path);
                }
            }
        }

        return fileToCreate;
    }

    /**
     * @param file to delete
     */
    public static void deleteFile(File file) {

        if (!file.exists()) {
            return;
        }

        try {
            file.delete();
        } catch (Exception e) {
            Log.info(c, "deleteFile", "Failed to delete : " + file.getAbsolutePath());
        }

        if (file.exists()) {
            Log.info(c, "deleteFile", "File exists after delete : " + file.getAbsolutePath());
        }
        return;
    }

    /**
     * @param full path and file name of file to delete
     */
    public static void deleteFile(String fileName) {
        deleteFile(new File(fileName));
    }

    public static String extractParentDirFromPath(String filePath) {
        int lastSlashIndex = filePath.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            return filePath.substring(0, lastSlashIndex);
        }
        return "";
    }

    /**
     * @param methodName
     * @param folder
     */
    public static void displayDirectoryContents(final String methodName, File folder) {

        File[] listOfFiles = folder.listFiles();
        StringBuffer sb = new StringBuffer();
        sb.append("Server 'logs' directory contents: \n[\n");

        if (listOfFiles != null) {
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    sb.append("File: " + listOfFiles[i].getName() + "\n");
                } else if (listOfFiles[i].isDirectory()) {
                    sb.append("Directory: " + listOfFiles[i].getName() + "\n");
                }
            }
        }
        sb.append("]\n");
        Log.info(c, methodName, sb.toString());
    }

    public static void displayFile(String filePath) {
        final String METHOD_NAME = "displayFile";
        File file = new File(filePath);
        if (!file.exists()) {
            Log.info(c, METHOD_NAME, "FILE DOES NOT EXIST: [ " + filePath + "]");
            return;
        }

        StringBuffer sb = new StringBuffer();
        sb.append("Contents of " + filePath + ": \n[\n");

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        sb.append("\n]\n");
        Log.info(c, METHOD_NAME, sb.toString());
    }
}
