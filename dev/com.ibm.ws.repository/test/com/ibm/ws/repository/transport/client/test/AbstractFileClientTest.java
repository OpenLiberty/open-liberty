/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.transport.client.test;

import java.io.File;
import java.io.IOException;

public class AbstractFileClientTest {

    public static File getTempDir() throws IOException {
        File tmpRepoRoot = File.createTempFile("tempRepoDir", null);
        tmpRepoRoot.delete();
        File tmpRepoDir = new File(tmpRepoRoot.getPath());
        if (!tmpRepoDir.mkdir()) {
            throw new IOException("Couldn't create directory for temp repo directory: " + tmpRepoDir);
        }
        tmpRepoDir.deleteOnExit();
        return tmpRepoDir;
    }

}
