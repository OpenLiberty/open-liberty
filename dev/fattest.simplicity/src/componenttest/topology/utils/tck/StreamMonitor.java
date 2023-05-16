/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.utils.tck;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Reads from a reader into a set of lines, optionally logging each line as its read from the reader
 * <p>
 * Starts a separate thread to read, but then uses blocking read operations so it is trusting that the reader will eventually end
 */
public class StreamMonitor {
    private static final Class<?> c = StreamMonitor.class;

    private final BufferedReader reader;
    private final List<String> lines = new ArrayList<>();
    private String logName = null;
    private File logFile;

    /**
     * Create a StreamMonitor which reads from the given reader
     *
     * @param reader the Reader to read from
     */
    public StreamMonitor(Reader reader) {
        this.reader = new BufferedReader(reader);
    }

    /**
     * Start running this StreamMonitor asynchronously
     *
     * @return an AutoClosable which will ensure the task stops and reports any exceptions
     */
    public AutoCloseable start() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Void> future = executor.submit(this::run);
        return () -> {
            try {
                // Give it a chance to finish reading and check for any errors
                // Note that we never expect exceptions here, even if we're monitoring the output of a process which failed or had to be terminated
                future.get(10, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                throw new Exception("Error reading output stream. Output may be incomplete.", e.getCause());
            } catch (TimeoutException e) {
                throw new Exception("Reading stream output did not complete", e);
            } finally {
                // Stop the task if it's still running
                future.cancel(true);
                executor.shutdown();
            }
        };
    }

    /**
     * Read from the reader until the end of the stream is reached
     *
     * @throws IOException          if there's an error reading from the reader or writing to the log
     * @throws InterruptedException if the thread is interrupted
     */
    public Void run() throws IOException, InterruptedException {
        // Handle opening and closing the reader and the log writer
        try (BufferedReader reader = this.reader) {
            if (logFile != null) {
                try (Writer logWriter = new BufferedWriter(new FileWriter(logFile))) {
                    doRun(reader, logWriter);
                }
            } else {
                doRun(reader, null);
            }
        }
        return null;
    }

    private void doRun(BufferedReader reader, Writer logWriter) throws IOException, InterruptedException {
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
            if (logName != null) {
                Log.info(c, logName, line);
            }
            if (logWriter != null) {
                logWriter.write(line);
                logWriter.write("\n");
            }
        }
    }

    /**
     * Log lines as they're read from the reader with the given name
     *
     * @param logName the name to use when logging, or {@code null} to disable logging
     */
    public void logAs(String logName) {
        this.logName = logName;
    }

    /**
     * Get the lines read from the stream
     *
     * @return the lines read from the stream
     */
    public List<String> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /**
     * Log lines as they're read from the reader to a file with the given name
     *
     * @param logFile the file to write to, or {@code null} to not log to a file
     */
    public void logToFile(File logFile) {
        this.logFile = logFile;
    }

}
