package io.openliberty.jpa.ejbpassivation;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jpa.fat.web.JpaPassivationServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.jpa.JPAFATServletClient;

@RunWith(FATRunner.class)
public class JPAPassivationTest extends JPAFATServletClient {
    private static final String CLASS_NAME = JPAPassivationTest.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static final String APP_NAME = "ejbpassivation";
    private static final String APP_RES_ROOT = "test-applications/ejbpassivation/ear/";
    private static final String contextRoot = "ejbpassivation";
    private static final String servletName = "JpaPassivationServlet";
    private static final String appPath = contextRoot + "/" + servletName;
    private static final String EOLN = String.format("%n");

    @Server("com.ibm.ws.jpa.fat.ejbpassivation")
    @TestServlets({
                    @TestServlet(servlet = JpaPassivationServlet.class, path = appPath)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        WebArchive webapp = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war").addPackage("com.ibm.ws.jpa.fat.web");
        ShrinkHelper.addDirectory(webapp, APP_RES_ROOT + APP_NAME + ".war");

        final JavaArchive ejbapp = ShrinkWrap.create(JavaArchive.class, APP_NAME + ".jar");
        ejbapp.addPackage("com.ibm.ws.jpa.fat.data");
        ejbapp.addPackage("com.ibm.ws.jpa.fat.ejb");
        ShrinkHelper.addDirectory(ejbapp, APP_RES_ROOT + APP_NAME + ".jar");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        app.addAsModule(ejbapp);
        app.addAsModule(webapp);
        ShrinkHelper.addDirectory(app, APP_RES_ROOT, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }

        });

        ShrinkHelper.exportToServer(server, "apps", app);

        server.addInstalledAppForValidation("ejbpassivation");
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CWWJP9991W");
    }

}
