/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health40.internal;

import java.io.File;
import java.io.IOException;

import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;

/**
 *
 */
public class FileUtils {

    static boolean canWriteToDirectory(File parentDir) {
        Object token = ThreadIdentityManager.runAsServer();
        try {

            File tempFile = File.createTempFile("test", null);
            if (!tempFile.canWrite()) {
                return false;
            }

            if (!tempFile.delete()) {
                //TODO: debug warning, couldn't delete testFile
            }

            return true;
        } catch (SecurityException | IOException exception) {
            //TODO: Warning about creation? or let caller handle that
            return false;
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    static boolean createFile(File file) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            file.deleteOnExit();
            return file.createNewFile();
        } catch (IOException ioe) {
            //TODO: Warning about creation.
            return false;
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    static boolean createDirectory(File file) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            file.deleteOnExit();
            return file.mkdir();
        } catch (SecurityException se) {
            //TODO: Warning about creation.
            return false;
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    static boolean deleteFiles(File file) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return file.delete();
        } catch (SecurityException e) {
            //TODO: issue waring
            return false;
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    static boolean setLastModified(File file) {
        Object token = ThreadIdentityManager.runAsServer();
        try {

            boolean ret = file.setLastModified(System.currentTimeMillis());
            if (!ret) {
                //TODO: failed to touch. Warning.
            }
            return ret;
        } catch (Exception e) {
            //TODO: failed, warning.
            return false;
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }
}
