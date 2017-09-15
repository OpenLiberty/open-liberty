/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils;

import java.io.File;
import java.io.IOException;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Utilities for backing up and restoring SSH keys before and after running FAT tests
 */
public class SSHKeysUtil {

    /**
     * Creates a temporary backup of the authorized_keys file. If the file doesn't exist, the backup file
     * is not created.
     * 
     * @param c The calling class
     * @return True if the backup was successful.
     *         False if the backup was unsuccessful.
     * @throws IOException
     */
    public static boolean backupAuthorizedKeys(Class<?> c) throws IOException {
        return backupAuthorizedKeys(c, null);
    }

    /**
     * Creates a temporary backup of the authorized_keys file. If the file doesn't exist, the backup file
     * is not created.
     * 
     * @param c The calling class
     * @param userHome The user's home directory which contains the .ssh directory.
     *            If it is null, userHome is set to the value of system property "user.home".
     * @return True if the backup was successful.
     *         False if the backup was unsuccessful.
     * @throws IOException
     */
    public static boolean backupAuthorizedKeys(Class<?> c, String userHome) throws IOException {
        boolean result = false;

        //Paths to the source file and backup file
        if (userHome == null) {
            userHome = System.getProperty("user.home");
        }
        String sourcePath = userHome + "/.ssh/authorized_keys";
        String destPath = userHome + "/.ssh/tempAuth_keys";

        File sourceFile = new File(sourcePath);
        File destFile = new File(destPath);

        if (!sourceFile.exists()) {
            Log.info(c, "backupAuthorizedKeys", "File: " + sourcePath + " doesn't exist. Exiting backup.");
            return result;
        }

        try {
            Log.info(c, "backupAuthorizedKeys", "Creating: " + sourcePath + " backup.");
            FileUtils.copyFile(sourceFile, destFile);
            result = true;
            Log.info(c, "backupAuthorizedKeys", "Successfully created backup of authorized_keys file to: " + destPath);
        } catch (IOException e) {
            Log.info(c, "backupAuthorizedKeys", "Failed to create authorized_keys backup due to: " + e.getMessage());
            throw e;
        } finally {
            //Delete the backup file if the backup is unsuccessful
            if (!result) {
                destFile.delete();
                Log.info(c, "backupAuthorizedKeys", "Successfully deleted backup file: " + destPath);
            }
        }

        return result;
    }

    /**
     * Restores the authorized_keys file and deletes the temporary file created. If the temporary backup file
     * doesn't exist, it means the user didn't have an authorized_keys file before the test ran, therefore
     * the one left by the tests will be deleted.
     * 
     * @param c The calling class
     * @return True if the restore was successful.
     *         False if the restore was unsuccessful.
     * @throws IOException
     */
    public static boolean restoreAuthorizedKeys(Class<?> c) throws IOException {
        return restoreAuthorizedKeys(c, null);
    }

    /**
     * Restores the authorized_keys file and deletes the temporary file created. If the temporary backup file
     * doesn't exist, it means the user didn't have an authorized_keys file before the test ran, therefore
     * the one left by the tests will be deleted.
     * 
     * @param c The calling class
     * @param userHome The user's home directory which contains the .ssh directory.
     *            If it is null, userHome is set to the value of system property "user.home".
     * @return True if the restore was successful.
     *         False if the restore was unsuccessful.
     * @throws IOException
     */
    public static boolean restoreAuthorizedKeys(Class<?> c, String userHome) throws IOException {
        boolean result = false;

        //Paths to the source file and backup file
        if (userHome == null) {
            userHome = System.getProperty("user.home");
        }
        String sourcePath = userHome + "/.ssh/tempAuth_keys";
        String destPath = userHome + "/.ssh/authorized_keys";

        File sourceFile = new File(sourcePath);
        File destFile = new File(destPath);

        if (!sourceFile.exists()) {
            Log.info(c, "restoreAuthorizedKeys", "File: " + sourcePath + " doesn't exist. Exiting restore.");
            Log.info(c, "restoreAuthorizedKeys", "Deleting: " + destFile);
            destFile.delete();
            return result;
        }

        try {
            Log.info(c, "backupAuthorizedKeys", "Restoring from: " + sourcePath);
            FileUtils.copyFile(sourceFile, destFile);
            result = true;
            Log.info(c, "restoreAuthorizedKeys", "Successfully restored authorized_keys file.");
        } catch (IOException e) {
            Log.info(c, "restoreAuthorizedKeys", "Failed to restore authorized_keys from backup due to: " + e.getMessage());
            Log.info(c, "restoreAuthorizedKeys", "Backup file located at: " + sourcePath);
            throw e;
        } finally {
            // Keep the backup file if the restore is not successful
            if (result) {
                sourceFile.delete();
                Log.info(c, "restoreAuthorizedKeys", "Successfully deleted backup file: " + sourcePath);
            }
        }

        return result;
    }

}
