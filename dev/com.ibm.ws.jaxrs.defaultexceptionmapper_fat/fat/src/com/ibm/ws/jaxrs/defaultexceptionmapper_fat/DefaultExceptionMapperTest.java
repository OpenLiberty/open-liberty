package com.ibm.ws.jaxrs.defaultexceptionmapper_fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs.defaultexceptionmapper_fat.app.TestApplication;
import com.ibm.ws.jaxrs.defaultexceptionmapper_fat.mapper.TestCallback1;
import com.ibm.ws.jaxrs.defaultexceptionmapper_fat.mapper.TestCallback2;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class DefaultExceptionMapperTest {

    private static final String APP_NAME = "testDefaultExceptionMapper";
    @Server("DefaultExceptionMapperServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        installSystemBundle("test.exceptionmapper");
        installSystemBundle("test.exceptionmapper.jakarta");
        server.installSystemFeature("test.exceptionmapper-2.0");
        server.installSystemFeature("test.exceptionmapper-3.0");

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(TestApplication.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testUnhandledException() throws IOException {
        HttpURLConnection con = HttpUtils.getHttpConnection(server, APP_NAME + "/causeException");
        assertThat(con.getResponseCode(), equalTo(500));
        assertThat(con.getHeaderFields(), allOf(hasEntry(equalTo(TestCallback1.EXCEPTION_MESSAGE_HEADER), contains("Test Exception")),
                                                hasEntry(equalTo(TestCallback1.RESOURCE_METHOD_NAME_HEADER), contains("causeException")),
                                                hasEntry(equalTo(TestCallback2.TEST_CALLBACK2_HEADER), contains(TestCallback2.TEST_CALLBACK2_VALUE))));
    }

    @Test
    public void testHandledException() throws IOException {
        HttpURLConnection con = HttpUtils.getHttpConnection(server, APP_NAME + "/handledException");
        assertThat(con.getResponseCode(), equalTo(400));
        assertThat(con.getHeaderFields(), not(anyOf(hasKey(TestCallback1.EXCEPTION_MESSAGE_HEADER),
                                                    hasKey(TestCallback1.RESOURCE_METHOD_NAME_HEADER),
                                                    hasKey(TestCallback2.TEST_CALLBACK2_HEADER))));
    }

    private static void installSystemBundle(String bundleName) throws Exception {
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getInstallRoot() + "/lib", "publish/files/bundles/" + bundleName + ".jar");
    }

}
