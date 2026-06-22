package io.openliberty.security.jakartasec.fat.tests;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import declared.roles.DeclaredRolesApplication;
import declared.roles.NoRoleResource;
import declared.roles.RolesResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
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
import static io.openliberty.security.jakartasec.fat.utils.Utils.convertStringToSet;
import static io.openliberty.security.jakartasec.fat.utils.Utils.executeGetRequestBasicAuth;
import static org.junit.Assert.*;

@RunWith(FATRunner.class)
@Mode(Mode.TestMode.LITE)
public class AppRolesTests {

    private final static Class<?> c = AppRolesTests.class;

    private final static String APP_NAME = "appPermissions";
    private final static String APP_NAME_2 = "appPermissions2";
    private final static String RESOURCE_PATH = "/roles";

    private static String urlApp1;
    private static String urlApp2;

    @Server("appPermissionsServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        Log.info(c, "setUp", "Starting server setup...");
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME+".war").addClass(DeclaredRolesApplication.class).addClass(RolesResource.class).addClass(NoRoleResource.class).addAsWebInfResource(new File("test-applications/declaredroles/WEB-INF/web.xml"));
        ShrinkHelper.exportAppToServer(server, app, ShrinkHelper.DeployOptions.SERVER_ONLY);
        WebArchive app2 = ShrinkWrap.create(WebArchive.class, APP_NAME_2+".war").addClass(DeclaredRolesApplication.class).addClass(RolesResource.class).addAsWebInfResource(new File("test-applications/declaredroles/WEB-INF/web.xml"));
        ShrinkHelper.exportAppToServer(server, app2, ShrinkHelper.DeployOptions.SERVER_ONLY);

        server.addDropinDefaultConfiguration("dropins/app1.xml");
        server.addDropinDefaultConfiguration("dropins/app2.xml");

        server.startServer();

        server.waitForLTPAConfigReady();
        urlApp1 = "http://localhost:" + server.getHttpDefaultPort() + "/" + APP_NAME + RESOURCE_PATH;
        urlApp2 = "http://localhost:" + server.getHttpDefaultPort() + "/" + APP_NAME_2 + RESOURCE_PATH;
        Log.info(c, "setUp", "Server started successfully");
    }

    @Test
    public void singleUserSingleAppRolesTest() throws Exception {
        Set<String> expectedResult = Collections.singleton("Role1");
        String result = executeGetRequestBasicAuth(urlApp1, USER1, USER1_PASSWORD, 200);
        Set<String> returnedRoles = convertStringToSet(result);
        assertEquals(expectedResult, returnedRoles);
        assertTrue("Expected Role Role1 in set, but got " + result, expectedResult.containsAll(returnedRoles));
    }

    @Test
    public void singleUserTwoAppRolesTest() throws Exception {
        String result = executeGetRequestBasicAuth(urlApp1, USER2, USER2_PASSWORD, 200);
        Set<String> expectedResult = new HashSet<>();
        expectedResult.add("Role1");
        expectedResult.add("Role2");
        Set<String> returnedRoles = convertStringToSet(result);
        assertEquals(expectedResult.size(), returnedRoles.size());
        assertTrue("Expected Roles Role1, Role2 in set, but got " + result, expectedResult.containsAll(returnedRoles));
    }

    @Test
    public void singleUserOneRolePerApplicationTest() throws Exception {
        String result = executeGetRequestBasicAuth(urlApp1, USER3, USER3_PASSWORD, 200);
        Set<String> expectedResult = Collections.singleton("Role1");
        Set<String> returnedRoles = convertStringToSet(result);
        assertEquals(expectedResult, returnedRoles);
        assertTrue("Expected Role Role1 in set, but got " + result, expectedResult.containsAll(returnedRoles));

        String result2 = executeGetRequestBasicAuth(urlApp2, USER3, USER3_PASSWORD, 200);
        Set<String> expectedResult2 = Collections.singleton("Role2");
        Set<String> returnedRoles2 = convertStringToSet(result2);
        assertEquals(expectedResult2, returnedRoles2);
        assertTrue("Expected Role Role2 in set, but got " + result, expectedResult2.containsAll(returnedRoles2));
    }

    @Test
    public void authenticatedUserNoRolesTest() throws Exception {
        String url = "http://localhost:" + server.getHttpDefaultPort() + "/" + APP_NAME + "/noroles";
        String result = executeGetRequestBasicAuth(url, USER4, USER4_PASSWORD, 200);
        Set<String> returnedRoles = convertStringToSet(result);
        assertTrue("Expected an empty set, but got " + result, returnedRoles.isEmpty());
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }
}
