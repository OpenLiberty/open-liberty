package io.openliberty.security.jakartasec.fat.tests;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import declared.roles.DeclaredRolesApplication;
import declared.roles.RolesResource;
import declared.roles.RunAsBean;
import declared.roles.RunAsResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static io.openliberty.security.jakartasec.fat.utils.Jakartasec40TestConstants.*;
import static io.openliberty.security.jakartasec.fat.utils.Utils.*;
import static org.junit.Assert.*;

@RunWith(FATRunner.class)
@Mode(Mode.TestMode.LITE)
public class AppBndRolesTests {

    private final static Class<?> c = AppBndRolesTests.class;

    private final static String APP_NAME = "appPermissions";
    private final static String ROLES_RESOURCE_PATH = "/roles";
    private final static String RUNAS_RESOURCE_PATH = "/runas";

    private static String url;

    @Server("appPermissionsServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        Log.info(c, "setUp", "Starting server setup...");
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME+".war").addClass(DeclaredRolesApplication.class).addClass(RolesResource.class).addClass(RunAsResource.class).addClass(RunAsBean.class).addAsWebInfResource(new File("test-applications/declaredroles/WEB-INF/web.xml")).add(new FileAsset(new File("test-applications/declaredroles/META-INF/ibm-application-bnd.xml")),"META-INF/ibm-application-bnd.xml");
        ShrinkHelper.exportAppToServer(server, app, ShrinkHelper.DeployOptions.SERVER_ONLY);

        server.addDropinDefaultConfiguration("dropins/app1_noRoles.xml");

        server.startServer();
        server.waitForLTPAConfigReady();

        url = "http://localhost:" + server.getHttpDefaultPort() + "/" + APP_NAME;

        Log.info(c, "setUp", "Server started successfully");
    }

    @Test
    public void testAppBndRoles() throws Exception {
        Set<String> expectedResult = Collections.singleton("Role1");
        String result = executeGetRequestBasicAuth(url + ROLES_RESOURCE_PATH, USER1, USER1_PASSWORD, 200);
        Set<String> returnedRoles = convertStringToSet(result);
        assertEquals(expectedResult, returnedRoles);
        assertTrue("Expected Role Role1 in set, but got " + result, expectedResult.containsAll(returnedRoles));

        Set<String> expectedResult2 = Collections.singleton("Role2");
        String result2 = executeGetRequestBasicAuth(url + ROLES_RESOURCE_PATH, USER3, USER3_PASSWORD, 200);
        Set<String> returnedRoles2 = convertStringToSet(result2);
        assertEquals(expectedResult2, returnedRoles2);
        assertTrue("Expected Role Role3 in set, but got " + result2, expectedResult2.containsAll(returnedRoles2));

        Set<String> expectedResult3 = new HashSet<>();
        expectedResult3.add("Role1");
        expectedResult3.add("Role3");
        String result3 = executeGetRequestBasicAuth(url + ROLES_RESOURCE_PATH, USER2, USER2_PASSWORD, 200);
        Set<String> returnedRoles3 = convertStringToSet(result3);
        assertTrue("Expected Role Role3 in set, but got " + result3, expectedResult3.containsAll(returnedRoles3));
    }

    @Test
    public void testRunAsRoles() throws Exception {
        String result = executeGetRequestNoAuth(url + RUNAS_RESOURCE_PATH,200);
        Set<String> returnedRoles = convertStringToSet(result);
        assertTrue("Expected empty set, but got " + result, returnedRoles.isEmpty());

        Set<String> expectedResult2 = Collections.singleton("Role2");
        String result2 = executeGetRequestBasicAuth(url + RUNAS_RESOURCE_PATH, USER3, USER3_PASSWORD, 200);
        Set<String> returnedRoles2 = convertStringToSet(result2);
        assertEquals(expectedResult2, returnedRoles2);
        assertTrue("Expected Role Role2 in set, but got " + result2, expectedResult2.containsAll(returnedRoles2));
    }

    @AfterClass
    public static void teardwon() throws Exception {
        server.stopServer();
    }
}
