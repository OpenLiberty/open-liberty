package com.ibm.ws.messaging.comms.fat;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import componenttest.topology.impl.LibertyServer;
import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.OVERWRITE;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public abstract class AbstractSuite {
    static void setupShrinkWrap(LibertyServer... servers) throws Exception {
        final JavaArchive utilsJar = ShrinkWrap.create(JavaArchive.class, "utilLib.jar")
                .addPackages(true, "test.util");

        final Archive testWar = ShrinkWrap.create(WebArchive.class, "CommsLP.war")
                .addClass("web.CommsLPServlet")
                .add(new FileAsset(new File("test-applications/CommsLP.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
                .add(new FileAsset(new File("test-applications/CommsLP.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml")
                .addAsLibrary(utilsJar);

        for (LibertyServer server: servers) ShrinkHelper.exportDropinAppToServer(server, testWar, OVERWRITE);
    }

    protected static StringBuilder runInServlet(LibertyServer server, String test) throws IOException {
        final URL url = new URL(String.format("http://%s:%d/CommsLP?test=%s",
                server.getHostname(), server.getHttpDefaultPort(), test));
        System.out.println("URL is : " + url.toString());
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

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0)
                fail("Missing success message in output. " + lines);

            return lines;
        } finally {
            con.disconnect();
        }
    }

    static void copyFiles(LibertyServer server, String destination, String... sources) throws Exception {
        for (String source: sources) server.copyFileToLibertyServerRoot(destination, source);
    }

    static void waitForServerStart(LibertyServer server, boolean isJmsServer) throws Exception {
        final RemoteFile traceFile = server.getDefaultTraceFile();
        String traceLine = server.waitForStringInLog("CWWKF0011I", traceFile);
        assertThat("Server should have started", traceLine, not(nullValue()));
        if (!isJmsServer) return;
        traceLine = server.waitForStringInLog("CWWKO0219I:.*InboundJmsCommsEndpoint", traceFile);
        assertThat("Server should have InboundJmsCommsEndpoint started", traceLine, not(nullValue()));
        traceLine = server.waitForStringInLog("CWWKO0219I:.*InboundJmsCommsEndpoint-ssl", traceFile);
        assertThat("Server should have InboundJmsCommsEndpoint-ssl started", traceLine, not(nullValue()));
    }
}
