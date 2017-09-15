/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.ibm.ws.kernel.boot.Debug;

/**
 * Determine if the server process is running based on whether or not the output
 * redirection still holds the file share lock on to console.log.
 */
public class FileShareLockProcessStatusImpl implements ProcessStatus {
    private final File file;

    public FileShareLockProcessStatusImpl(File file) {
        this.file = file;
    }

    @Override
    public boolean isPossiblyRunning() {
        if (file.exists()) {
            try {
                new FileOutputStream(file, true).close();
            } catch (FileNotFoundException e) {
                // "java.io.FileNotFoundException: C:\...\logs\console.log
                // (The process cannot access the file because it is being used
                // by another process.)"
                return true;
            } catch (IOException e) {
                Debug.printStackTrace(e);
            }
        }

        return false;
    }
}
