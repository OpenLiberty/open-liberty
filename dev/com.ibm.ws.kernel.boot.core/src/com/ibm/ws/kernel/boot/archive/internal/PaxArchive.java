/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.archive.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ibm.ws.kernel.boot.Debug;

/**
 * An implementation of {@code Archive} will start a pax command
 * in a separate process to archive all file names that it reads
 * from its standard input.
 */
public class PaxArchive extends AbstractArchive {

    /**
     * The pax file to generate or update.
     */
    private final File archiveFile;

    /**
     * The representation of the spawned pax process.
     */
    private Process paxProcess;

    /**
     * A print stream that sends data to the pax stdin descriptor.
     */
    private PrintStream paxStdin;

    /**
     * Create an archive
     * 
     * @param archiveFile the target pax file
     */
    public PaxArchive(File archiveFile) {
        this.archiveFile = archiveFile;
    }

    @Override
    public void addFileEntry(String entryPath, File source) throws IOException {
        // Cleanup an active pax process if running.
        if (paxProcess != null) {
            close();
        }

        List<String> args = preparePaxCmd(entryPath, true);

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        paxProcess = processBuilder.start();

        // Get the InputStream for stdin and output the file content
        paxStdin = new PrintStream(paxProcess.getOutputStream(), true);

        paxStdin.println(source.getAbsolutePath());
    }

    @Override
    public void addDirEntry(String entryPath, File source, List<String> dirContent) throws IOException {
        // Cleanup an active pax process if running.
        if (paxProcess != null) {
            close();
        }

        List<String> args = preparePaxCmd(entryPath, false);

        // Set the current working directory of pax to the source directory
        // and fire up the process
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.directory(source);
        paxProcess = processBuilder.start();

        // Get the InputStream for stdin
        paxStdin = new PrintStream(paxProcess.getOutputStream(), true);

        for (String relativePath : dirContent) {
            paxStdin.println(relativePath);
        }
    }

    private List<String> preparePaxCmd(String entryPath, boolean isFileName) throws IOException {
        if (paxProcess != null) {
            throw new IOException("Archive already open");
        }

        // Attempt to find the pax command
        String paxCommand = null;
        for (String candidate : Arrays.asList("/bin/pax", "/usr/bin/pax")) {
            File pax = new File(candidate);
            if (pax.exists()) {
                paxCommand = candidate;
                break;
            }
        }

        // Bail if we can't find the pax command
        if (paxCommand == null) {
            throw new IOException("pax is not in /bin or /usr/bin");
        }

        // Build the command line into an array list
        // /bin/pax -wd -x pax -s @^@entryPrefix@ -f ${archiveFile}
        List<String> args = new ArrayList<String>();
        args.add(paxCommand);
        args.add("-wd");

        // Specify the z/OS specific "pax" format if we're on z/OS
        if ("z/OS".equalsIgnoreCase(System.getProperty("os.name"))) {
            args.add("-x");
            args.add("pax");
        }

        // Use 'ed' style regex to handle the entry path
        if (entryPath != null && !entryPath.isEmpty()) {
            args.add("-s");
            if (isFileName) {
                args.add("@.*@" + entryPath + "@");
            } else { // is entry prefix
                args.add("@^@" + entryPath + "@");
            }
        }

        // If we're opening an existing file, we'll assume we're appending
        if (archiveFile.exists()) {
            args.add("-a");
        }

        // Specify the absolute path to the target pax file
        args.add("-f");
        args.add(archiveFile.getAbsolutePath());

        return args;
    }

    @Override
    public void close() throws IOException {
        if (paxStdin == null) {
            return;
        }

        paxStdin.close();
        paxStdin = null;

        processOutput(paxProcess.getErrorStream());
        processOutput(paxProcess.getInputStream());

        try {
            int returnCode = paxProcess.waitFor();
            if (returnCode != 0) {
                throw new IOException("/bin/pax returned with " + returnCode);
            }
        } catch (InterruptedException ie) {
            throw new IOException(ie);
        } finally {
            paxProcess = null;
        }
    }

    private void processOutput(InputStream inputStream) {
        InputStreamReader isr = new InputStreamReader(inputStream);
        BufferedReader br = new BufferedReader(isr);

        try {
            String line = null;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException ioe) {
            Debug.printStackTrace(ioe);
        }

        try {
            inputStream.close();
        } catch (IOException ioe) {
            Debug.printStackTrace(ioe);
        }
    }

}
