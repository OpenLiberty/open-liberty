/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.ibm.websphere.simplicity.log.Log;

/**
 * A version of program output which handles asynchronous commands.
 * If something wants to know what output the program has produced,
 * the output up to that point (which might be large) will be
 * returned.
 */
public class AsyncProgramOutput extends ProgramOutput {

    private final StreamCapturingThread stdErr;
    private final StreamCapturingThread stdOut;

    public AsyncProgramOutput(String command, String[] parameters, Process process) {
        super(stringify(command, parameters), 0, null, null);
        // The javadoc for process says that on some platforms if the 
        // output isn't consumed, it will block, so consume pre-emptively.
        stdErr = new StreamCapturingThread(process.getErrorStream());
        stdOut = new StreamCapturingThread(process.getInputStream());

        stdErr.start();
        stdOut.start();
    }

    /**
     * @param command
     * @param parameters
     * @return
     */
    private static String stringify(String command, String[] parameters) {
        StringBuilder cmd = new StringBuilder(command);
        cmd.append(" ");
        for (String parameter : parameters) {
            cmd.append(parameter);
            cmd.append(" ");
        }
        return cmd.toString();
    }

    /**
     * Get the output written to stderr
     * 
     * @return stderr
     */
    @Override
    public String getStderr() {
        return stdErr.getContents();
    }

    /**
     * Get all of the output written to stdout so far.
     * 
     * @return stdout
     */
    @Override
    public String getStdout() {
        return stdOut.getContents();
    }

    private class StreamCapturingThread extends Thread {
        /**
         * We probably won't ever want to capture more than 2kb of console
         * output, since the server should be logging anyway.
         */
        private static final int BUFFER_SIZE = 2 * 1024;
        private BoundedByteArrayOutputStream bytes;
        private BufferedWriter writer;
        private final InputStream stream;

        private StreamCapturingThread(InputStream stream) {
            this.stream = stream;
            // Run as a daemon thread so we don't keep things alive
            setDaemon(true);
        }

        /**
         * @return
         */
        public String getContents() {
            if (writer != null) {
                try {
                    writer.flush();
                } catch (IOException e) {
                    Log.warning(AsyncProgramOutput.class, "Could not flush process output: " + e.toString());
                }
            }
            if (bytes != null) {
                return bytes.toString();
            } else {
                return "(no output)";
            }
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

                String line = reader.readLine();
                while (line != null) {
                    BufferedWriter writer = getWriter();
                    writer.write(line);
                    writer.newLine();
                    line = reader.readLine();
                }
            } catch (IOException e) {
                Log.warning(AsyncProgramOutput.class, "Could not read process output: " + e.toString());
            }
        }

        /**
         * @throws IOException
         */
        private BufferedWriter getWriter() throws IOException {
            if (bytes == null) {
                bytes = new BoundedByteArrayOutputStream(BUFFER_SIZE);
            }
            // If we're near the limit of our buffer, wrap around 
            if (bytes.size() > 0 && (bytes.count() + 400) >= bytes.size()) {
                int size = bytes.size();
                bytes.reset();
                writer = new BufferedWriter(new OutputStreamWriter(bytes));
                writer.write("[" + size / 1024 + " KB OF EARLIER OUTPUT TRIMMED FOR LENGTH]");
            } else if (writer == null) {
                writer = new BufferedWriter(new OutputStreamWriter(bytes));
            }

            return writer;
        }
    }

    /**
     * A byte array stream whose maximum size is (vaguely) bounded.
     */
    class BoundedByteArrayOutputStream extends ByteArrayOutputStream {
        /**
         * @param bufferSize
         */
        public BoundedByteArrayOutputStream(int bufferSize) {
            super(bufferSize);
        }

        public int count() {
            return count;
        }
    }
}
