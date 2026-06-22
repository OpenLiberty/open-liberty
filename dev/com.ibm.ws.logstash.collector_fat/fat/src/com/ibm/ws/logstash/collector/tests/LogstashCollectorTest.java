/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logstash.collector.tests;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public abstract class LogstashCollectorTest {

    private static Class<?> c = LogstashCollectorTest.class;

    // Constants
    public static final String LIBERTY_MESSAGE = "liberty_message";
    public static final String LIBERTY_TRACE = "liberty_trace";
    public static final String LIBERTY_FFDC = "liberty_ffdc";
    public static final String LIBERTY_GC = "liberty_gc";
    public static final String LIBERTY_ACCESSLOG = "liberty_accesslog";
    public static final String LIBERTY_AUDIT = "liberty_audit";
    public static final String NPE = "NullPointerException";
    public static final String AIOB = "ArrayIndexOutOfBoundsException";

    public static final String KEY_TYPE = "type";
    public static final String KEY_TAGS = "tags";
    public static final String KEY_REASON = "reason";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_OBJECTDETAILS = "objectDetails";
    public static final String KEY_URLPATH = "uriPath";
    public static final String KEY_STACKTRACE = "stackTrace";
    public static final String ENTRY = "Entry";
    public static final String EXIT = "Exit";
    public static final String MESSAGE_PREFIX = "Test Logstash Message";
    public static final String PATH_TO_AUTOFVT_TESTFILES = "lib/LibertyFATTestFiles/";
    public static final int DEFAULT_TIMEOUT = 40 * 1000; // 40 seconds
    private static File generatedPrivateKeyFile;
    private static File generatedCertificateFile;

    private static File generatedOverrideFile;

    protected abstract LibertyServer getServer();

    private static String APP_URL = null;
    private static String buffer;
    private static CopyOnWriteArrayList<String> logstashOutput = new CopyOnWriteArrayList<String>();

    protected void setConfig(String conf) throws Exception {
        Log.info(c, "setConfig entry", conf);
        getServer().setMarkToEndOfLog();
        getServer().setServerConfigurationFile(conf);
        assertNotNull("Cannot find CWWKG0016I from messages.log", getServer().waitForStringInLogUsingMark("CWWKG0016I", 60000));
        String line = getServer().waitForStringInLogUsingMark("CWWKG0017I|CWWKG0018I", 60000);
        assertNotNull("Cannot find CWWKG0017I or CWWKG0018I from messages.log", line);
        waitForStringInContainerOutput("CWWKG0017I|CWWKG0018I"); // waits for server configuration to finish updating (CWWKG0017I)
        waitForStringInContainerOutput("CWWKZ0003I"); // waits for application to finish updating (CWWKZ0003I)
        Log.info(c, "setConfig exit", conf);
    }

    protected void createMessageEvent() {
        createMessageEvent(null);
    }

    protected void createMessageEvent(String id) {
        String url = getAppUrl();
        if (id != null) {
            try {
                url = url + "?id=" + URLEncoder.encode(id, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.error(c, "createMessageEvent", e);
                e.printStackTrace();
            }
        }
        runApp(url);
    }

    protected void createMessageEventWithException(String id) {
        String url = getAppUrl() + "/ExceptionURL";
        if (id != null) {
            try {
                url = url + "?id=" + URLEncoder.encode(id, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.error(c, "createMessageEventWithException", e);
                e.printStackTrace();
            }
        }
        runApp(url);
    }

    protected void createAccessLogEvent() {
        createAccessLogEvent(null);
    }

    protected void createAccessLogEvent(String id) {
        String url = getAppUrl() + "/AccessURL";
        if (id != null) {
            try {
                url = url + "?id=" + URLEncoder.encode(id, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.error(c, "createAccessLogEvent", e);
                e.printStackTrace();
            }
        }
        runApp(url);

    }

    protected void createMessageLogEvents(String id) {
        String url = getAppUrl() + "/MessageURL";
        if (id != null) {
            try {
                url = url + "?id=" + URLEncoder.encode(id, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.error(c, "createMessageLogEvents", e);
                e.printStackTrace();
            }
        }
        runApp(url);

    }

    protected void createFFDCEvent(int i) {
        String url = getAppUrl();
        switch (i) {
            case 2:
                url = url + "?secondFFDC=true";
                break;
            case 3:
                url = url + "?thirdFFDC=true";
                break;
            default:
                url = url + "?isFFDC=true";
        }
        runApp(url);
    }

    protected void createTraceEvent() {
        createTraceEvent(null);
    }

    protected void createTraceEvent(String id) {
        String url = getAppUrl() + "/TraceURL";
        if (id != null) {
            try {
                url = url + "?id=" + URLEncoder.encode(id, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.error(c, "createTraceEvent", e);
                e.printStackTrace();
            }
        }
        runApp(url);
    }

    protected void createGCEvent() {
        String url = getAppUrl() + "?gc=true";
        runApp(url);
    }

    private static void runApp(String url) {
        String method = "runApp";
        Log.info(c, method, "---> Running the application with url : " + url);
        try {
            ValidateHelper.runGetMethod(url);
        } catch (Exception e) {
            Log.info(c, method, " ---> Exception : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getAppUrl() {
        if (APP_URL == null) {
            APP_URL = "http://" + getServer().getHostname() + ":" + getServer().getHttpDefaultPort() + "/LogstashApp";
        }
        return APP_URL;
    }

    private static final String IMAGE_NAME = DockerImageName.parse("public.ecr.aws/elastic/logstash:9.3.3") //
                    .asCompatibleSubstituteFor("logstash:9.3.3") //
                    .asCanonicalNameString();

    // This helper method is passed into `withLogConsumer()` of the container
    // It will consume all of the logs (System.out) of the container, which we will
    // use to pipe container output to our standard FAT output logs (output.txt)
    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n")) {
            msg = msg.substring(0, msg.length() - 1);
        }

        boolean isComplete = false;
        if (msg.startsWith("{") && msg.endsWith("}")) {
            buffer = msg;
            isComplete = true;
        } else if (msg.startsWith("{")) {
            buffer = msg;
        } else if (msg.endsWith("}")) {
            buffer = buffer + msg;
            isComplete = true;
        } else {
            buffer = buffer + msg;
        }
        if (isComplete) {
            logstashOutput.add(buffer);
            Log.info(c, "logstashContainer", buffer);
            buffer = null;
        }
    }

    protected static void clearContainerOutput() {
        logstashOutput.clear();
        Log.info(c, "clearContainerOutput", "cleared logstashOutput");
    }

    protected static int getContainerOutputSize() {
        return logstashOutput.size();
    }

    protected static int waitForContainerOutputSize(int size) {
        int timeout = DEFAULT_TIMEOUT;
        while (timeout > 0) {
            if (getContainerOutputSize() >= size) {
                return size;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            timeout -= 1000;
        }
        return getContainerOutputSize();
    }

    protected static String waitForStringInContainerOutput(String regex) {
        Log.info(c, "waitForStringInOutput", "looking for " + regex);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher;
        int timeout = DEFAULT_TIMEOUT;

        while (timeout > 0) {
            Iterator<String> it = logstashOutput.iterator();
            while (it.hasNext()) {
                String line = it.next();
                matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return line;
                }
            }
            timeout -= 1000;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        Log.info(c, "waitForStringInOutput", "Timed out and could not find any lines containing : " + regex);
        return null; // timed out and not found
    }

    protected static String findStringInContainerOutput(String str) {
        Iterator<String> it = logstashOutput.iterator();
        while (it.hasNext()) {
            String line = it.next();
            if (line.contains(str)) {
                return line;
            }
        }
        return null; // not found
    }

    protected static List<JSONObject> parseJsonInContainerOutput() throws JSONException {
        ArrayList<JSONObject> list = new ArrayList<JSONObject>();
        Iterator<String> it = logstashOutput.iterator();

        String partialLine = "";
        while (it.hasNext()) {
            String line = it.next();
            if (!line.endsWith("}")) {
                // Handle a split output frame
                partialLine += line;
                continue;
            }
            if (!partialLine.isEmpty()) {
                line = partialLine + line;
                partialLine = "";
            }
            try {
                JSONObject json = new JSONObject(line);
                list.add(json);
            } catch (Exception e) {
                Log.error(c, "parseJsonInContainerOutput", e, "Unable to parse JSON: " + line);
                throw e;
            }
        }
        return list;
    }

    protected static void generateTrustStoreForServer(LibertyServer server) throws Exception {
        String dockerHostName = DockerClientFactory.instance().dockerHostIpAddress();
        String securityUtility = server.getInstallRoot() + "/bin/securityUtility";
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            securityUtility = securityUtility + ".bat";
        }

        String[] cmd = new String[] {
                                      "createSSLCertificate",
                                      "--server=" + server.getServerName(),
                                      "--password=passw0rd",
                                      "--subject=CN=" + dockerHostName + ",OU=" + server.getServerName() + ",O=ibm,C=us"
        };

        Properties env = new Properties();
        ProgramOutput commandOutput = server.getMachine().execute(securityUtility, cmd, server.getInstallRoot(), env);

        Log.info(c, "generateTrustStoreForServer", "stderr:\n" + commandOutput.getStderr()
                                                   + "\nstdout:\n" + commandOutput.getStdout()
                                                   + "\nRC: " + commandOutput.getReturnCode());

        if (commandOutput.getReturnCode() != 0) {
            throw new IllegalStateException("securityUtility createSSLCertificate failed with return code " + commandOutput.getReturnCode());
        }

        /*
         * Acquire keyStore server.xml snippet to be injected into server.xml
         */

        String keyStoreSnippet = extractKeyStoreSnippet(commandOutput.getStdout());
        generatedOverrideFile = new File(server.getServerRoot() + "/configDropins/overrides/logstash-ssl-override.xml");
        writeOverrideXml(generatedOverrideFile, keyStoreSnippet);
        Log.info(c, "generateTrustStoreForServer", "Wrote SSL override to " + generatedOverrideFile.getAbsolutePath());

        File keystoreFile = new File(server.getServerRoot() + "/resources/security/key.p12");
        if (!keystoreFile.exists()) {
            throw new IllegalStateException("Expected keystore was not created: " + keystoreFile.getAbsolutePath());
        }

        /*
         * Section where we extract the keys/cert from the p12 file.
         * Store to field, which we use later to inject into logstash container
         */
        generatedPrivateKeyFile = new File(server.getServerRoot() + "/resources/security/logstash.key");
        generatedCertificateFile = new File(server.getServerRoot() + "/resources/security/logstash.crt");

        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(fis, "passw0rd".toCharArray());

            String alias = keyStore.aliases().nextElement();
            Key privateKey = keyStore.getKey(alias, "passw0rd".toCharArray());
            Certificate certificate = keyStore.getCertificate(alias);

            if (privateKey == null) {
                throw new IllegalStateException("No private key found in keystore: " + keystoreFile.getAbsolutePath());
            }
            if (certificate == null) {
                throw new IllegalStateException("No certificate found in keystore: " + keystoreFile.getAbsolutePath());
            }

            writePemFile(generatedPrivateKeyFile, "PRIVATE KEY", privateKey.getEncoded());
            writePemFile(generatedCertificateFile, "CERTIFICATE", certificate.getEncoded());

            Log.info(c, "generateTrustStoreForServer", "Extracted private key to " + generatedPrivateKeyFile.getAbsolutePath());
            Log.info(c, "generateTrustStoreForServer", "Extracted certificate to " + generatedCertificateFile.getAbsolutePath());
        }
    }

    protected static GenericContainer<?> prepareServerSSLAndConstructContainer(LibertyServer server) throws Exception {
        generateTrustStoreForServer(server);

        return new GenericContainer<>(new ImageFromDockerfile() //
                        .withDockerfileFromBuilder(builder -> builder.from(IMAGE_NAME) //
                                        .copy("/usr/share/logstash/pipeline/logstash.conf", "/usr/share/logstash/pipeline/logstash.conf") //
                                        .copy("/usr/share/logstash/config/logstash.yml", "/usr/share/logstash/config/logstash.yml") //
                                        .copy("/usr/share/logstash/config/logstash.key", "/usr/share/logstash/config/logstash.key") //
                                        .copy("/usr/share/logstash/config/logstash.crt", "/usr/share/logstash/config/logstash.crt") //
                                        .build()) //
                        .withFileFromFile("/usr/share/logstash/pipeline/logstash.conf", new File(PATH_TO_AUTOFVT_TESTFILES + "logstash.conf"), 644) //
                        .withFileFromFile("/usr/share/logstash/config/logstash.yml", new File(PATH_TO_AUTOFVT_TESTFILES + "logstash.yml"), 644) //
                        .withFileFromFile("/usr/share/logstash/config/logstash.key", generatedPrivateKeyFile, 644) //
                        .withFileFromFile("/usr/share/logstash/config/logstash.crt", generatedCertificateFile, 644)) //
                        .withExposedPorts(5043) //
                        .withStartupTimeout(Duration.ofSeconds(240)) //
                        .withLogConsumer(LogstashCollectorTest::log); //
    }

    private static void writePemFile(File outputFile, String type, byte[] encodedBytes) throws Exception {
        /*
         * Writes in PKCS#8 format. Logstash does not like PKCS#1 format if we were to use SSLUtils. So we need to use our own method here.
         */
        String pem = "-----BEGIN " + type + "-----\n"
                     + Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(encodedBytes)
                     + "\n-----END " + type + "-----\n";

        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        Files.write(outputFile.toPath(), pem.getBytes(StandardCharsets.US_ASCII));
    }

    private static String extractKeyStoreSnippet(String stdout) {
        Pattern pattern = Pattern.compile(
                                          "<keyStore\\b[^>]*\\bid=\"defaultKeyStore\"[^>]*\\bpassword=\"\\{[a-zA-Z0-9]+\\}[^\"\\s>]+\"[^>]*/>",
                                          Pattern.DOTALL);
        Matcher matcher = pattern.matcher(stdout);
        if (matcher.find()) {
            return matcher.group();
        }

        throw new IllegalStateException("Unable to find <keyStore .../> snippet in securityUtility output:\n" + stdout);
    }

    private static void writeOverrideXml(File outputFile, String xmlSnippet) throws Exception {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                     + "<server>\n"
                     + "    " + xmlSnippet + "\n"
                     + "</server>\n";

        Files.write(outputFile.toPath(), xml.getBytes(StandardCharsets.UTF_8));
    }

}
