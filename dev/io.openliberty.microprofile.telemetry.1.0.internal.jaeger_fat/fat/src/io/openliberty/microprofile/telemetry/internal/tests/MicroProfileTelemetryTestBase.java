/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.tests;

import static org.junit.Assert.assertNotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.ClassRule;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public abstract class MicroProfileTelemetryTestBase {

    private static Class<?> c = MicroProfileTelemetryTestBase.class;

    // Constants

    public static final String PATH_TO_AUTOFVT_TESTFILES = "lib/LibertyFATTestFiles/";
    public static final int DEFAULT_TIMEOUT = 40 * 1000; // 40 seconds
    public static final String JAEGER_IMAGE = "jaegertracing/all-in-one:1.37";
    public static final int JAEGER_UI_PORT = 16686;
    public static final int JAEGER_GRPC_PORT = 14250;
    public static final Duration CONTAINER_STARTUP_TIMEOUT = Duration.ofSeconds(240);

    public static final String ENV_OTEL_SERVICE_NAME = "OTEL_SERVICE_NAME";
    public static final String OTEL_SERVICE_NAME_SYSTEM = "system";
    public static final String ENV_OTEL_TRACES_EXPORTER = "OTEL_TRACES_EXPORTER";
    public static final String OTEL_TRACES_EXPORTER_JAEGER = "jaeger";
    public static final String ENV_OTEL_EXPORTER_JAEGER_ENDPOINT = "OTEL_EXPORTER_JAEGER_ENDPOINT";

    public static final String JAEGER_QUERY_URL = "http://%s:%s/api/traces";
    public static final String JAEGER_QUERY_PARAMS = "?start=%d&limit=%d&service=%s";
    public static final int JAEGER_QUERY_LIMIT = 20;

    protected abstract LibertyServer getServer();

    private static String APP_URL = null;
    private static String buffer;
    private static CopyOnWriteArrayList<String> containerOutput = new CopyOnWriteArrayList<String>();

    protected void setConfig(String conf) throws Exception {
        Log.info(c, "setConfig entry", conf);
        getServer().setMarkToEndOfLog();
        getServer().setServerConfigurationFile(conf);
        assertNotNull("Cannot find CWWKG0016I from messages.log", getServer().waitForStringInLogUsingMark("CWWKG0016I", 10000));
        String line = getServer().waitForStringInLogUsingMark("CWWKG0017I|CWWKG0018I", 10000);
        assertNotNull("Cannot find CWWKG0017I or CWWKG0018I from messages.log", line);
        waitForStringInContainerOutput("CWWKG0017I|CWWKG0018I");
        Log.info(c, "setConfig exit", conf);
    }

    protected String runApp(String url) {
        String method = "runApp";
        Log.info(c, method, "---> Running the application with url : " + url);
        try {
            return ValidateHelper.runGetMethod(url);
        } catch (Exception e) {
            Log.info(c, method, " ---> Exception : " + e.getMessage());
            return null;
        }
    }

    protected String getAppUrl(String appName) {
        if (APP_URL == null) {
            APP_URL = "http://" + getServer().getHostname() + ":" + getServer().getHttpDefaultPort() + "/" + appName;
        }
        return APP_URL;
    }

    protected String queryJaeger() {
        String method = "getSpans";
        String url = String.format(JAEGER_QUERY_URL, jaegerContainer.getHost(), String.valueOf(jaegerContainer.getMappedPort(JAEGER_UI_PORT)));
        String queryParams = String.format(JAEGER_QUERY_PARAMS, System.currentTimeMillis(), JAEGER_QUERY_LIMIT, OTEL_SERVICE_NAME_SYSTEM);
        Log.info(c, method, "---> Querying Jaeger with url : " + url + queryParams);
        try {
            return ValidateHelper.runGetMethod(url + queryParams);
        } catch (Exception e) {
            Log.info(c, method, " ---> Exception : " + e.getMessage());
            return null;
        }
    }

    private static final String IMAGE_NAME = ImageNameSubstitutor.instance() //
                    .apply(DockerImageName.parse(JAEGER_IMAGE)).asCanonicalNameString();

    // Can be added to the FATSuite to make the resource lifecycle bound to the entire
    // FAT bucket. Or, you can add this to any JUnit test class and the container will
    // be started just before the @BeforeClass and stopped after the @AfterClass
    @ClassRule
    public static GenericContainer<?> jaegerContainer = new GenericContainer<>(new ImageFromDockerfile() //
                    .withDockerfileFromBuilder(builder -> builder.from(IMAGE_NAME) //
                                    .build())) //
                    .withExposedPorts(JAEGER_UI_PORT, JAEGER_GRPC_PORT) //
                    .withStartupTimeout(CONTAINER_STARTUP_TIMEOUT) //
                    .withLogConsumer(MicroProfileTelemetryTestBase::log); //

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
            containerOutput.add(buffer);
            Log.info(c, "jaegerContainer", buffer);
            buffer = null;
        }
    }

    protected static void clearContainerOutput() {
        containerOutput.clear();
        Log.info(c, "clearContainerOutput", "cleared container output");
    }

    protected static int getContainerOutputSize() {
        return containerOutput.size();
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
        Log.info(c, "waitForStringInContainerOutput", "looking for " + regex);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher;
        int timeout = DEFAULT_TIMEOUT;

        while (timeout > 0) {
            Iterator<String> it = containerOutput.iterator();
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
        return null; // timed out and not found
    }

    protected static String findStringInContainerOutput(String str) {
        Iterator<String> it = containerOutput.iterator();
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
        Iterator<String> it = containerOutput.iterator();

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
}
