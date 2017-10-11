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
package test.common.zos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 
 * Wrapper around process.waitFor and process.getInputStream/getErrorStream.
 * 
 * getInputStream/getErrorStream must be called from different threads than
 * the waitFor, since the Process may not exit if it's waiting for this guy
 * to read some data off the pipe that's opened up between this guy and the
 * process.
 * 
 * Usage:
 * 
 * new WaitForProcess(Process p).waitFor().getStdout();
 * 
 */
public class WaitForProcess {

    /**
     * ExecutorService for creating threads to collect the process's stdout/stderr.
     */
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * The process's stdout stream.
     */
    private Future<List<String>> stdout;

    /**
     * The process's stderr stream.
     */
    private Future<List<String>> stderr;

    /**
     * 
     * Waits for the process to complete and starts up 2 separate threads for
     * reading the input/error streams.
     * 
     * @return this
     */
    public WaitForProcess waitFor(final Process process) throws InterruptedException {

        // Start threads for reading stdout and stderr.
        // Note: this must be done on separate threads otherwise the process may block
        // waiting for this guy to read some output.
        stdout = executorService.submit(new Callable<List<String>>() {
            @Override
            public List<String> call() {
                return loadStreamUnchecked(process.getInputStream());
            }
        });

        stderr = executorService.submit(new Callable<List<String>>() {
            @Override
            public List<String> call() {
                return loadStreamUnchecked(process.getErrorStream());
            }
        });

        process.waitFor();

        return this;
    }

    /**
     * @return The stdout stream.
     */
    public List<String> getStdout() {
        try {
            return stdout.get();
        } catch (Exception ee) {
            throw new RuntimeException(ee);
        }
    }

    /**
     * @return The stderr stream.
     */
    public List<String> getStderr() {
        try {
            return stderr.get();
        } catch (Exception ee) {
            throw new RuntimeException(ee);
        }
    }

    /**
     * @return The contents of the InputStream
     * 
     * @throws RuntimeException if an IOException occurs.
     */
    protected List<String> loadStreamUnchecked(InputStream is) {
        try {
            return loadStream(is);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Read the entire contents of the given inputstream and return as a List<String>,
     * 
     * @return The contents of the InputStream as List<String>, one entry per line.
     */
    protected List<String> loadStream(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is)); // stdout
        List<String> retMe = new ArrayList<String>();
        String line;
        while ((line = br.readLine()) != null) {
            retMe.add(line);
        }
        return retMe;
    }

}
