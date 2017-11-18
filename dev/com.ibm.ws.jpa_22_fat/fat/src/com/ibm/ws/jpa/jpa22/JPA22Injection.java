package com.ibm.ws.jpa.jpa22;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jpa22injection.web.JPAInjectionTestServlet;

/**
 * Test cases that verify \@PersistenceContext and \@PersistenceUnit are \@Repeatable.
 *
 */
@RunWith(FATRunner.class)
public class JPA22Injection extends FATServletClient {
    public static final String APP_NAME = "jpa22injection";
    public static final String SERVLET = "TestJPA22Injection";

    @Server("JPA22InjectionServer")
    @TestServlet(servlet = JPAInjectionTestServlet.class, path = APP_NAME + "/" + SERVLET)
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        final String resPath = "test-applications/jpa22/" + APP_NAME + "/resources/";

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        app.addPackage("jpa22injection.web");
        app.addPackage("jpa22injection.entity");
        ShrinkHelper.addDirectory(app, resPath);
        ShrinkHelper.exportAppToServer(server1, app);
        server1.addInstalledAppForValidation(APP_NAME);

        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer("CWWJP9991W"); // From Eclipselink drop-and-create tables option
    }
}
