package com.ibm.ws.jdbc.fat.sqlserver;

import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.DatabaseCluster;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.SQLServerTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 7, runSyntheticTest = false)
public class SQLServerTest extends FATServletClient {

    public static final String APP_NAME = "sqlserverfat";
    public static final String SERVLET_NAME = "SQLServerTestServlet";

    @Server("com.ibm.ws.jdbc.fat.sqlserver")
    @TestServlet(servlet = SQLServerTestServlet.class, path = APP_NAME + '/' + SERVLET_NAME)
    public static LibertyServer server;

    public static DatabaseCluster dbCluster;

    @BeforeClass
    public static void setUp() throws Exception {
        // Pick the java 7 or java 8 JDBC driver
        String jdbcJar = "1.7".equals(System.getProperty("java.specification.version")) ? "sqljdbc41.jar" : "sqljdbc42.jar";
        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().add(new Variable("database.jdbcjar", jdbcJar));
        server.updateServerConfiguration(config);

        dbCluster = new DatabaseCluster();
        dbCluster.createDatabase();
        dbCluster.addConfigTo(server);

        // Create a normal Java EE application and export to server
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")//
                        .addPackages(true, "web");
        ShrinkHelper.exportAppToServer(server, app);

        server.addInstalledAppForValidation(APP_NAME);
        server.startServer();

        runTest(server, APP_NAME + '/' + SERVLET_NAME, "initDatabase");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (server.isStarted()) {
                ArrayList<String> expectedErrorMessages = new ArrayList<String>();
                // Some config warnings are expected if the JDBC driver version is old
                List<String> jdbcVersionLines = server.findStringsInLogs("DSRA8206I");
                if (!jdbcVersionLines.isEmpty()) {
                    // DSRA8206I: JDBC driver version  : 4.1.5605.100
                    String[] parts = jdbcVersionLines.get(0).split(" |\\x2E"); // space or .
                    if (parts.length > 4) {
                        int major = Integer.parseInt(parts[parts.length - 4]);
                        int minor = Integer.parseInt(parts[parts.length - 3]);
                        System.out.println("JDBC driver version " + major + '.' + minor);
                        if (major < 6) {
                            expectedErrorMessages.add("DSRA8020E.*serverNameAsACE");
                            expectedErrorMessages.add("DSRA8020E.*transparentNetworkIPResolution");
                        }
                    }
                }
                expectedErrorMessages.add("DSRA0304E"); // From XAException upon rollback of already timed out XAResource
                expectedErrorMessages.add("DSRA0302E.*XAER_NOTA"); // More specific message for rollback of already timed out XAResource
                expectedErrorMessages.add("J2CA0027E.*rollback"); // JCA message for rollback of already timed out XAResource
                expectedErrorMessages.add("J2CA0027E.*commit"); // JCA message for attempted commit of already timed out XAResource
                server.stopServer(expectedErrorMessages.toArray(new String[expectedErrorMessages.size()]));
            }
        } finally {
            dbCluster.dropDatabase();
        }
    }
}
