/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.cloudfoundry;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.ibm.websphere.simplicity.cloudfoundry.util.StreamGobbler;

public final class ExecuteCommandUtil {

    public static String executeCommand(String[] command) {
        File workDir = new File("");
        return executeCommand(command, workDir.getAbsolutePath());
    }

    public static String executeCommand(String[] command, String workDirPath) {
        ProcessBuilder procBuild = new ProcessBuilder(command);
        procBuild.directory(new File(workDirPath));
        String outputString = "";

        // Redirect stderr to stdout so that we don't lose stderr
        procBuild.redirectErrorStream(true);
        Process proc;

        try {
            proc = procBuild.start();

            // Capture the merged output and error streams in a separate thread, to avoid blocking
            boolean async = false;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), output, async);

            outputGobbler.start();

            // Don't return until we know if the command was successful or not
            proc.waitFor();

            // let the streams catch up (they'll only last for a second past the original process)
            outputGobbler.doJoin();

            outputString = output.toString();
        } catch (IOException e) {
            // Don't make calling code handle this exceptions, but throw something so the test fails
            throw new RuntimeException("Running the commands " + Arrays.toString(command) + " in directory "
                                       + workDirPath + " gave an exception: " + e, e);
        } catch (InterruptedException e) {
            // This is unlikely, and probably non-fatal, so a printStackTrace will suffice
            e.printStackTrace();
        }
        return outputString;
    }
}
