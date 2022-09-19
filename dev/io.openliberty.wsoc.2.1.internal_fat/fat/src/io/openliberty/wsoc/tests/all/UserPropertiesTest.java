package io.openliberty.wsoc.tests.all;

import io.openliberty.wsoc.util.wsoc.WsocTest;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import io.openliberty.wsoc.util.wsoc.WsocTestRunner;
import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.endpoints.client.basic.ClientUserPropertiesClientEP;
import io.openliberty.wsoc.endpoints.client.basic.ServerUserPropertiesClientEP;
import io.openliberty.wsoc.endpoints.client.basic.ClientUserPropertiesClientEP.UserPropertyClientEndpointConfig;
import jakarta.websocket.ClientEndpointConfig.Builder;

import jakarta.websocket.ClientEndpointConfig;

import java.util.List;

import org.junit.Assert;

public class UserPropertiesTest {
    
    private WsocTest wsocTest = null;

    public UserPropertiesTest(WsocTest test) {
        this.wsocTest = test;
    }

    public void testUserPropertiesOnServer() throws Exception {
        // Checks are performed within client endpoint because wsoc Impl uses HashMap which doesn't guarentee order.
        // Also avoids sending the properties to the client
        String uri = "/basic21/userproperties";

        WsocTestContext testdata = wsocTest.runWsocTest(new ServerUserPropertiesClientEP.UserPropertiesTest(), uri,WsocTestRunner.getDefaultConfig(), 2, Constants.getDefaultTimeout());
        List<Object> list = testdata.getMessage();
        Assert.assertEquals(list.size(), 2);
        Assert.assertTrue(list.contains("MODIFY-2"));
        Assert.assertTrue(list.contains("SERVER-1"));
        testdata.reThrowException();

        // User Properties must be independent of each session.
        // Modifications in the first run should not be reflected in the second run
        // IllegalStateException is thrown on the endpoint if the properties are not correct.
        WsocTestContext secondRunTestData = wsocTest.runWsocTest(new ServerUserPropertiesClientEP.UserPropertiesTest(), uri,WsocTestRunner.getDefaultConfig(), 2, Constants.getDefaultTimeout());
        secondRunTestData.reThrowException();

    }

    public void testUserPropertiesOnClient() throws Exception {
        // Checks are performed within client endpoint because wsoc Impl uses HashMap which doesn't guarentee order.
        String uri = "/basic21/echo";
        WsocTestContext testdata = wsocTest.runWsocTest(new ClientUserPropertiesClientEP.UserPropertiesTest(), uri, (ClientEndpointConfig) new ClientUserPropertiesClientEP.UserPropertyClientEndpointConfig(), 0, Constants.getDefaultTimeout());
        List<Object> list = testdata.getMessage();
        Assert.assertEquals(list.size(), 2);
        Assert.assertTrue(list.contains("MODIFY-1"));
        Assert.assertTrue(list.contains("CLIENT-1"));
        testdata.reThrowException();

        // User Properties must be independent between the two runs 
        // Modifications in the first run should not be reflected in the second run
        // IllegalStateException is thrown on the endpoint if the properties are not correct.
        WsocTestContext secondRunTestData = wsocTest.runWsocTest(new ClientUserPropertiesClientEP.UserPropertiesTest(), uri, (ClientEndpointConfig) new ClientUserPropertiesClientEP.UserPropertyClientEndpointConfig(), 0, Constants.getDefaultTimeout());
        secondRunTestData.reThrowException();
    }
}
