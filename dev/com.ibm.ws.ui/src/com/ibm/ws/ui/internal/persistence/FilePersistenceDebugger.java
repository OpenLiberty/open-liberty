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
package com.ibm.ws.ui.internal.persistence;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.ui.persistence.IPersistenceDebugger;

/**
 */
public class FilePersistenceDebugger implements IPersistenceDebugger {
    private static final TraceComponent tc = Tr.register(FilePersistenceDebugger.class);

    /**
     * Try to close the Closeable object. This should never fail. If it does,
     * let it FFDC incase it matters down stream.
     * 
     * @param c
     */
    private void tryClose(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "IOException while trying to close Closeable. Ignoring but here's the details.", c, e);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getFileContents(final File file) {
        return new PrivilegedAction<String>() {
            @FFDCIgnore(IOException.class)
            @Override
            public String run() {
                StringBuilder sb = new StringBuilder();
                FileInputStream fis = null;
                InputStreamReader isr = null;
                BufferedReader br = null;
                try {
                    fis = new FileInputStream(file);
                    isr = new InputStreamReader(fis, "UTF-8");
                    br = new BufferedReader(isr);

                    String line = br.readLine();
                    while (line != null) {
                        sb.append(line);
                        sb.append("\n");
                        line = br.readLine();
                    }
                } catch (IOException e) {
                    // Let this FFDC because we should never get here
                    sb.append("Unable to load file contents: ");
                    sb.append(e.getMessage());
                } finally {
                    tryClose(br);
                    tryClose(isr);
                    tryClose(fis);
                }
                return sb.toString();
            }
        }.run();
    }

}
