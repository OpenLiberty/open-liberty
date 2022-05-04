/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.examplera;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

public class ResourceAdapterExampleTest {
    private final String className = "ResourceAdapterExampleTest";
    private static SharedOutputManager outputMgr;
    private static final String PORT = System.getProperty("HTTP_default", "8000");
    private final long TIMEOUT = 60000; // 60 seconds - Sun jdk build exceeded 30 seconds

    /**
     * Utility method to copy a file.
     *
     * @param currentLocation location of the file to copy
     * @param targetLocation  location to which to copy the file
     * @throws IOException if an error occurs
     */
    private static void copy(String currentLocation, String targetLocation) throws IOException {
        FileInputStream input = null;
        FileOutputStream output = null;
        try {
            input = new FileInputStream(currentLocation);
            output = new FileOutputStream(targetLocation);
            FileChannel inputChannel = input.getChannel();
            inputChannel.transferTo(0, inputChannel.size(), output.getChannel());

        } finally {
            if (input != null)
                input.close();
            if (output != null)
                output.close();
        }
    }

    /**
     * Utility method to scan the messages.log file
     *
     * @param pattern pattern for which to search
     * @return lines that contain the substring
     * @throws IOException if an error occurs
     */
    private static List<String> findInMessages(String pattern) throws IOException {
        List<String> found = new ArrayList<String>();
        BufferedReader log = null;
        try {
            log = new BufferedReader(new FileReader(System.getProperty("server.root") + "/logs/messages.log"));
            for (String line = log.readLine(); line != null; line = log.readLine())
                if (line.matches(pattern))
                    found.add(line);
        } finally {
            if (log != null)
                log.close();
        }

        return found;
    }

    /**
     * @param initialCount of matches previously found in the log file.
     * @param regexp       to match at least once in the log file.
     * @throws IOException
     * @throws Exception
     * @throws InterruptedException
     */
    private void waitForInMessages(int initialCount, String regexp) throws IOException, Exception, InterruptedException {
        final String methodName = "waitForInMessages";
        // Wait for the updates
        long start = System.currentTimeMillis();
        for (int i = 0; i < initialCount + 1; i = findInMessages(regexp).size()) {
            if (System.currentTimeMillis() - start > TIMEOUT)
                throw new Exception("[Exc0208] " + className + " : " + methodName + "(): Timed out exceeding the " + TIMEOUT
                                    + " Milliseconds limit while waiting for the message pattern(" + regexp + ").");
            Thread.sleep(500);
        }
    }

    /**
     * Utility method to run a test on RAExampleServlet.
     *
     * @param query query string for the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    private StringBuilder runInServlet(String query) throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/ExampleApp?" + query);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            if (lines.indexOf("ERROR:") >= 0)
                fail("Error in servlet output: " + lines);

            return lines;
        } finally {
            con.disconnect();
        }
    }

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // There are variations of this constructor:
        // e.g. to specify a log location or an enabled trace spec. Ctrl-Space for suggestions
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    @Test
    public void testAddAndFind() throws Exception {
        StringBuilder output;

        // attempt find for an entry that isn't in the table
        output = runInServlet("functionName=FIND&capital=Saint%20Paul");
        if (output.indexOf("Did not FIND any entries") < 0)
            throw new Exception("Entry should not have been found. Output: " + output);

        // add
        output = runInServlet("functionName=ADD&state=Iowa&population=30741869&area=56272&capital=Des%20Moines");
        output = runInServlet("functionName=ADD&state=Minnesota&population=5379139&area=86939&capital=Saint%20Paul");

        // find
        output = runInServlet("functionName=FIND&capital=Saint%20Paul");
        if (output.indexOf("Successfully performed FIND with output: {area=86939, capital=Saint Paul, population=5379139, state=Minnesota}") < 0)
            throw new Exception("Did not find entry. Output: " + output);
    }

    @Test
    public void testAddAndRemove() throws Exception {
        StringBuilder output;

        // add
        output = runInServlet("functionName=ADD&city=Rochester&state=Minnesota&population=106769");
        output = runInServlet("functionName=ADD&city=Stewartville&state=Minnesota&population=5916");
        output = runInServlet("functionName=ADD&city=Byron&state=Minnesota&population=4914");

        // remove
        output = runInServlet("functionName=REMOVE&city=Stewartville");
        if (output.indexOf("Successfully performed REMOVE with output: {city=Stewartville, population=5916, state=Minnesota}") < 0)
            throw new Exception("Did not report entry removed. Output: " + output);

        // attempt removal of something that doesn't exist
        output = runInServlet("functionName=REMOVE&city=Stewartville");
        if (output.indexOf("Successfully performed REMOVE") >= 0)
            throw new Exception("Entry should not have been present to remove. Output: " + output);
    }

    @Test
    public void testMessageDrivenBean() throws Exception {
        final String methodName = "testMessageDrivenBean";
        int initialCountCWWKZ0003I = findInMessages(".*CWWKZ0003I.* ExampleApp .*").size();
        int initialCountJ2CA8801I = findInMessages(".*J2CA8801I.* ExampleApp.*").size();
        int initialCountJ2CA7001I = findInMessages(".*J2CA7001I.* ExampleRA .*").size();

        // Update server.xml to enable mdb-3.1 and define an activationSpec
        copy(System.getProperty("server.root") + "/server-with-mdb.txt",
             System.getProperty("server.root") + "/server.xml");

        // Example: CWWKZ0003I: The application ExampleApp updated in 2.270 seconds.
        waitForInMessages(initialCountCWWKZ0003I, ".*CWWKZ0003I.* ExampleApp .*");
        // Example: J2CA8801I: The message endpoint for activation specification ExampleApp/ExampleWeb/ExampleMessageDrivenBean and message driven bean application ExampleApp#ExampleWeb.war#ExampleMessageDrivenBean is activated.
        waitForInMessages(initialCountJ2CA8801I, ".*J2CA8801I.* ExampleApp.*");
        // Example: J2CA7001I: Resource adapter ExampleRA installed in 0.901 seconds.
        waitForInMessages(initialCountJ2CA7001I, ".*J2CA7001I.* ExampleRA .*");

        StringBuilder output = runInServlet("functionName=ADD&county=Olmsted&state=Minnesota&population=147066&area=654.5");
        if (output.indexOf("Successfully performed ADD with output: {area=654.5, county=Olmsted, population=147066, state=Minnesota}") < 0)
            throw new Exception("[Exc0215] " + className + " : " + methodName + "(): Did not report entry added. Output: " + output);

        // search messages log for MDB output
        boolean found = false;
        BufferedReader log = new BufferedReader(new FileReader(System.getProperty("server.root") + "/logs/messages.log"));
        try {
            for (String line = log.readLine(); line != null && !found; line = log.readLine())
                found = line.contains("ExampleMessageDrivenBean.onMessage record = {area=654.5, county=Olmsted, population=147066, state=Minnesota}");
        } finally {
            log.close();
        }

        if (!found)
            throw new Exception("[Exc0228] " + className + " : " + methodName + "(): Output from message driven bean not found in messages.log");
    }

}
