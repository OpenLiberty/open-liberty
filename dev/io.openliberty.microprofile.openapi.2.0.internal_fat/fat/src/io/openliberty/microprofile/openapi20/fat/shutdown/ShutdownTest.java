package io.openliberty.microprofile.openapi20.fat.shutdown;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestApp;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class ShutdownTest {

    @Server("OpenAPITestServer")
    public static LibertyServer server;
    
    @After
    public void cleanup() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
    }
    
    /**
     * Check that we don't process any apps on shutdown when multiple apps are deployed
     */
    @Test
    public void multiAppShutdownTest() throws Exception {
        for (int i = 0; i < 10; i++) {
            WebArchive war = ShrinkWrap.create(WebArchive.class, "test-app-" + i + ".war")
                            .addPackage(DeploymentTestApp.class.getPackage());
            ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);
        }
        
        server.startServer();
        
        List<String> appProcessedMessages = server.findStringsInLogs("CWWKO1660I");
        assertThat("Exactly one app should be processed on startup", appProcessedMessages, hasSize(1));

        server.stopServer(false); // stop server without archiving, since we want to check logs
        
        try {
            appProcessedMessages = server.findStringsInLogs("CWWKO1660I");
            assertThat("No further apps should be processed on shutdown", appProcessedMessages, hasSize(1));
        } finally {
            server.postStopServerArchive();
        }
    }
}
