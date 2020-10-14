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
package com.ibm.ws.diagnostics.zos.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.logging.IntrospectableService;

/**
 * Diagnostic handler to obtain process information from the 'ps' command.
 */
public class ProcessInformation implements IntrospectableService {

    /**
     * Declarative services activation callback.
     */
    protected void activate() {
    }

    /**
     * Declarative services deactivation callback.
     */
    protected void deactivate() {
    }

    /**
     * Get string that will serve as the basis of the file name.
     */
    @Override
    public String getName() {
        return "ProcessInformation";
    }

    /**
     * Get a simple description for this diagnostic module.
     */
    @Override
    public String getDescription() {
        return "Server process information from the 'ps' command";
    }

    /**
     * Introspect the process information and write the output.
     *
     * @param out the output stream to write to
     */
    @Override
    public void introspect(OutputStream out) throws IOException {
        PrintWriter writer = new PrintWriter(out, true);

        // Put out a header before the information
        writer.println("Server Process Information");
        writer.println("--------------------------");
        getProcessInformation(getPid(), writer);

        // Add a trailer with the status flag decoder ring from the documentation
        writer.println("\n\n-- Status Flags Legend --");
        writer.println("1:\tA single task using assembler callable services.");
        writer.println("A:\tMessage queue receive wait.");
        writer.println("B:\tMessage queue send wait.");
        writer.println("C:\tCommunication system kernel wait.");
        writer.println("D:\tSemaphore operation wait.");
        writer.println("E:\tQuiesce frozen.");
        writer.println("F:\tFile system kernel wait.");
        writer.println("G:\tMVS Pause wait.");
        writer.println("H:\tOne or more pthread created tasks (implies M as well).");
        writer.println("I:\tSwapped out.");
        writer.println("J:\tPthread created.");
        writer.println("K:\tOther kernel wait (for example, pause or sigsuspend).");
        writer.println("L:\tCanceled, parent has performed wait, and still session or process group leader.");
        writer.println("M:\tMultithread.");
        writer.println("N:\tMedium weight thread.");
        writer.println("O:\tAsynchronous thread.");
        writer.println("P:\tPtrace kernel wait.");
        writer.println("R:\tRunning (not kernel wait).");
        writer.println("S:\tSleeping.");
        writer.println("T:\tStopped.");
        writer.println("U:\tInitial process thread.");
        writer.println("V:\tThread is detached.");
        writer.println("W:\tWaiting for a child (wait or waitpid function is running).");
        writer.println("X:\tCreating a new process (fork function is running).");
        writer.println("Y:\tMVS wait.");
        writer.println("Z:\tCanceled and parent has not performed wait (Z for zombie).");
        writer.print("\n");

        writer.flush();
    }

    // TODO: Replace with native method that calls getpid from <unistd.h>

    /**
     * Get the PID for the current process. This implementation is a little
     * obtuse as it spawns a process that gets its parent's PID and should be
     * replaced with a native call to getpid.
     *
     * @return the process identifier as a string
     */
    @FFDCIgnore(InterruptedException.class)
    private String getPid() throws IOException {
        List<String> args = new ArrayList<String>();
        args.add("/bin/sh");
        args.add("-c");
        args.add("/bin/echo $PPID");

        // Create the process
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process pidProcess = processBuilder.redirectErrorStream(true).start();

        // Wrap the input stream with a reader
        InputStream pidInputStream = pidProcess.getInputStream();
        InputStreamReader pidInputStreamReader = new InputStreamReader(pidInputStream);
        BufferedReader pidReader = new BufferedReader(pidInputStreamReader);

        // The PID should be the only line of output
        String pid = pidReader.readLine();

        // Eat all output after the first line
        while (pidReader.readLine() != null);
        pidReader.close();

        // Get the return code from the process
        int returnCode = -1;
        try {
            returnCode = pidProcess.waitFor();
        } catch (InterruptedException ie) {
            throw new IOException(ie);
        } finally {
            pidProcess.getOutputStream().close();
            pidProcess.getErrorStream().close();
        }

        if (returnCode != 0 || !pid.matches("\\d+")) {
            throw new IOException("Unable to acquire process ID");
        }

        return pid;
    }

    /**
     * Invoke the system 'ps' command to get information about this server process.
     *
     * @param pid    the process identifier to pass to the command
     * @param writer the print writer to write information to
     */
    private void getProcessInformation(String pid, PrintWriter writer) throws IOException {
        List<String> args = new ArrayList<String>();
        args.add("/bin/ps");
        args.add("-m"); // Include thread info
        args.add("-o"); // Format options
        args.add("pid,xtid,user,jobname,etime,time,vsz,vsz64,vszlmt64,thdcnt,xtcbaddr,state,syscall,lsyscall,tagdata");
        args.add("-p"); // Process ID
        args.add(pid);

        // Create the process we'll use to invoke 'ps'
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process psProcess = processBuilder.redirectErrorStream(true).start();

        // Wrap the input stream with a reader
        InputStream psInputStream = psProcess.getInputStream();
        InputStreamReader psInputStreamReader = new InputStreamReader(psInputStream);
        BufferedReader psReader = new BufferedReader(psInputStreamReader);

        String line;
        while ((line = psReader.readLine()) != null) {
            writer.println(line);
        }
        psReader.close();

        int returnCode = -1;
        try {
            returnCode = psProcess.waitFor();
        } catch (InterruptedException ie) {
            throw new IOException(ie);
        } finally {
            psProcess.getOutputStream().close();
            psProcess.getErrorStream().close();
        }

        if (returnCode != 0) {
            throw new IOException("Unable to get process info");
        }
    }
}
