/*******************************************************************************
 * Copyright (c) 2011,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jca.fat.regr;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.Description;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 * Common infrastructure code for JCA REGR FAT tests
 */
public abstract class JCAFATTest {

    protected final static Class<?> c = JCAFATTest.class;
    protected static LibertyServer server;

    private static boolean runPasswordCheck = false;
    private static List<String> passwords;
    private static List<String> excludeTraceList;

    private static final String fvtapp = "fvtapp";
    private static final String fvtweb = "fvtweb";

    /**
     * Print the name of test methods, timestamp, and failures to System.out so
     * that we have this information for debug purposes.
     */
    @Rule
    public final TestName testName = new TestName() {
        @Override
        public void failed(Throwable e, Description description) {
            System.out.println(timestamp() + " <--- "
                               + description.getMethodName() + " failed:");
            e.printStackTrace(System.out);
        }

        @Override
        public void starting(Description description) {
            System.out.println(timestamp() + " ---> "
                               + description.getMethodName() + " starting");
        }

        @Override
        public void succeeded(Description description) {
            System.out.println(timestamp() + " <--- "
                               + description.getMethodName() + " successful");
        }

        private final String timestamp() {
            Date date = new Date(System.currentTimeMillis());
            return '[' + date.toString() + ']';
        }
    };

    /**
     * Utility method to search for a clear text passwords
     *
     * @throws Exception
     *             if occurrences found
     */
    protected void checkForTextPasswordsInLogs() throws Exception {
        List<String> uncheckedMatches = new ArrayList<String>();
        List<String> checkedMatches = new ArrayList<String>();
        boolean accept = true;

        if (passwords == null)
            throw new Exception("List of passwords is null. activatePasswordCheck() must be called before test.");

        uncheckedMatches = server
                        .findStringsInLogsAndTraceUsingMarkMultiRegexp(passwords);

        for (String match : uncheckedMatches) {
            accept = true;
            for (String exclude : excludeTraceList) {
                if (match.contains(exclude)) {
                    accept = false;
                    break;
                }
            }
            if (accept)
                checkedMatches.add(match);
        }

        if (checkedMatches.size() > 0) {
            StringBuffer error = new StringBuffer("");
            error.append(checkedMatches.size()
                         + " password occurrence(s) found:\n");
            for (String match : checkedMatches)
                error.append(match + "\n");

            throw new Exception(error.toString());
        }
    }

    /**
     * Utility method that activates the test to search for passwords in the
     * logs and trace files. This method must be used before running
     * checkForTextPasswordsInLogs()
     *
     * @param server
     *            the server
     * @param excludeTraceNames
     *            a list of Strings that the password check will use to ignore
     *            instances of a password found if the line also contains a
     *            string in the exclude list
     * @throws Exception
     *             if occurrences found
     */
    protected static void activatePasswordCheck(List<String> excludeTraceNames) throws Exception {
        String method = "activatePasswordCheck";
        runPasswordCheck = true;
        excludeTraceList = excludeTraceNames;
        server.resetLogOffsets();
        String prefix = "password=\"";
        passwords = new ArrayList<String>();
        RemoteFile rFile = server.getFileFromLibertyServerRoot("server.xml");
        BufferedReader reader = new BufferedReader(new InputStreamReader(rFile.openForReading()));
        String line, substring;
        int startPos = -1, endPos = -1;

        try {
            while ((line = reader.readLine()) != null) {
                startPos = line.indexOf(prefix);
                while (startPos > -1) {
                    endPos = line.indexOf("\"", startPos + prefix.length());
                    substring = line.substring(startPos + prefix.length(),
                                               endPos);
                    substring = substring.replace("{", "\\{");
                    substring = substring.replace("}", "\\}");

                    if (!passwords.contains(substring)) {
                        Log.info(c, method, "Found text password => "
                                            + substring);
                        passwords.add(substring);
                    }
                    startPos = line.indexOf(prefix, endPos);
                }
            }
        } finally {
            reader.close();
        }
    }

    protected static void deactivatePasswordCheck() {
        runPasswordCheck = false;
    }

    /**
     * Utility method to run a test on a JCA FAT servlet.
     *
     * @param servlet
     *            Name of servlet to run test on
     * @param test
     *            Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException
     *             if an error occurs
     */
    protected StringBuilder runInJCAFATServlet(String warFile, String servlet,
                                               String test) throws Exception {
        String host = server.getMachine().getHostname();
        URL url = new URL("http://" + host + ":" + server.getHttpDefaultPort()
                          + "/" + warFile + "/" + servlet + "?test=" + test);
        for (int numRetries = 2;; numRetries--) {
            Log.info(c, "runInJCAFATServlet", "URL is " + url);
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

                // Send output from servlet to console output
                for (String line = br.readLine(); line != null; line = br
                                .readLine()) {
                    lines.append(line).append(sep);
                    Log.info(c, "runInJCAFATServlet", line);
                }

                // Look for success message, otherwise fail test
                if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                    Log.info(c, "runInJCAFATServlet",
                             "failed to find completed successfully message");
                    fail("Missing success message in output. " + lines);
                }

                return lines;
            } catch (FileNotFoundException x) {
                if (numRetries > 0)
                    try {
                        Log.info(getClass(), "runInJCAFATServlet", x
                                                                   + " occurred - will retry after 2 seconds");
                        Thread.sleep(2000); // down from 10s
                    } catch (InterruptedException interruption) {
                    }
                else
                    throw x;
            } finally {
                Log.info(c, "runInJDBCFATServlet", "disconnecting from servlet");
                if (runPasswordCheck)
                    checkForTextPasswordsInLogs();
                con.disconnect();
            }
        }
    }

    /**
     * Build the fvtapp.ear and place it in the server's apps directory.
     *
     * @throws Exception
     */
    protected static void buildFvtAppEar() throws Exception {
        /*
         * Build jars that will be in the RAR
         */
        JavaArchive JCAFAT1_jar = ShrinkWrap.create(JavaArchive.class, "AnnotatedInboundSecurity.jar");
        JCAFAT1_jar.addPackage("com.ibm.inout.adapter");

        JavaArchive JCAFAT2_jar = ShrinkWrap.create(JavaArchive.class, "ResourceAdapter.jar");
        JCAFAT2_jar.addPackages(true, "com.ibm.adapter", "com.ibm.ejs.ras", "com.ibm.ws.csi");

        /*
         * Build the resource adapter.
         */
        ResourceAdapterArchive JCAFAT1_rar = ShrinkWrap.create(ResourceAdapterArchive.class, "adapter_jca16_insec_AnnotatedInboundSecurity.rar");
        JCAFAT1_rar.addAsManifestResource(new File("test-resourceadapters/adapter.regr/resources/META-INF/adapter_jca16_insec_AnnotatedInboundSecurity-ra.xml"), "ra.xml");
        JCAFAT1_rar.addAsLibraries(JCAFAT1_jar, JCAFAT2_jar);

        /*
         * Build the web module and application.
         */
        WebArchive fvtweb_war = ShrinkWrap.create(WebArchive.class, fvtweb + ".war");
        fvtweb_war.addPackages(true, "ejb.inboundsec", "web.regr");
        fvtweb_war.addAsWebInfResource(new File("test-applications/fvtweb_regr/resources/WEB-INF/web.xml"));

        EnterpriseArchive fvtapp_ear = ShrinkWrap.create(EnterpriseArchive.class, fvtapp + ".ear");
        fvtapp_ear.addAsModules(fvtweb_war, JCAFAT1_rar);
        ShrinkHelper.addDirectory(fvtapp_ear, "lib/LibertyFATTestFiles/fvtapp_regr");
        ShrinkHelper.exportToServer(server, "apps", fvtapp_ear);
    }
}