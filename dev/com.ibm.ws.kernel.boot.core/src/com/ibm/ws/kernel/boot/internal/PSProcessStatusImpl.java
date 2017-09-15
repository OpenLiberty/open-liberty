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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import com.ibm.ws.kernel.boot.Debug;
import com.ibm.ws.kernel.boot.cmdline.Utils;

/**
 * Determine if the process is running using "ps -p".
 */
public class PSProcessStatusImpl implements ProcessStatus {
    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows");

    private final String pid;

    /**
     * @param pid the pid, or the empty string if {@link #isPossiblyRunning} should
     *            always return true
     */
    public PSProcessStatusImpl(String pid) {
        this.pid = pid;
    }

    @Override
    public boolean isPossiblyRunning() {
        if (pid.isEmpty()) {
            // OS/400 shell does not support $!, so an empty PID is always
            // passed (unless native integration is enabled).  Return true since
            // the process might be running.
            return true;
        }

        ProcessBuilder pb = new ProcessBuilder();

        if (WINDOWS) {
            // java.exe is a non-Cygwin process, so we need to pass -W.
            pb.command("ps", "-W", "-p", pid);
        } else {
            pb.command("ps", "-p", pid);
        }
        Debug.println(pb.command());

        pb.redirectErrorStream(true);
        InputStream in = null;
        BufferedReader reader = null;

        try {
            Process p = pb.start();
            in = p.getInputStream();

            reader = new BufferedReader(new InputStreamReader(in));
            for (String line; (line = reader.readLine()) != null;) {
                Debug.println(line);
            }

            p.waitFor();
            return p.exitValue() == 0;
        } catch (IOException e) {
            Debug.printStackTrace(e);
        } catch (InterruptedException e) {
            Debug.printStackTrace(e);
        } finally {
            Utils.tryToClose(in);
            Utils.tryToClose(reader);
        }

        return true;
    }
}
