/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.grpc.internal.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.ibm.ws.http2.GrpcServletServices;

import test.common.SharedOutputManager;

/**
 * Basic unit tests for grpc-1.0 configuration
 */
public class GrpcServiceConfigTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule rule = outputMgr;

    @Rule
    public TestName name = new TestName();

    @After
    public void tearDown() {
        GrpcServletServices.destroy();
    }

    /**
     * Verify that the application name map works as expected
     */
    @Test
    public void testApplicationMap() {
        String app_name_1 = "APP1";
        String app_name_2 = "APP2";
        String app_name_3 = "APP3";
        GrpcServiceConfigImpl config = new GrpcServiceConfigImpl();
        GrpcServiceConfigImpl.addApplication(app_name_1);
        GrpcServiceConfigImpl.addApplication(app_name_2);
        GrpcServiceConfigImpl.addApplication(app_name_3);
        GrpcServiceConfigImpl.removeApplication(app_name_1);
        GrpcServiceConfigImpl.removeApplication(app_name_2);
        Set<String> apps = config.getDependentApplications();
        Assert.assertEquals(1, apps.size());
        Assert.assertTrue(apps.contains(app_name_3));
        GrpcServiceConfigImpl.removeApplication(app_name_3);
        Assert.assertEquals(0, config.getDependentApplications().size());
    }
    
    /**
     * Verify that the application name map works as expected
     */
    @Test
    public void testModified() {
        // create and activate a property set
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("prop1", "value1");
        props.put(GrpcConfigConstants.TARGET_PROP, "*");
        GrpcServiceConfigImpl config1 = new GrpcServiceConfigImpl();
        config1.activate(props);
        Assert.assertTrue(GrpcServiceConfigHolder.getURIProps("uri").containsKey("prop1"));

        // modify the property and make sure the new values are in use 
        String serviceName = "ExplicitName";
        props.put(GrpcConfigConstants.TARGET_PROP, "ExplicitName");
        config1.modified(props);
        Assert.assertNull(GrpcServiceConfigHolder.getURIProps("uri"));
        Assert.assertTrue(GrpcServiceConfigHolder.getURIProps("ExplicitName").containsKey("prop1"));

        // test some additional params
        String fakeInterceptorClass = "com.fake.Class";
        props.put(GrpcConfigConstants.SERVER_INTERCEPTORS_PROP, fakeInterceptorClass);
        props.put(GrpcConfigConstants.MAX_INBOUND_MSG_SIZE_PROP, "64");
        config1.modified(props);
        Assert.assertEquals(fakeInterceptorClass, GrpcServiceConfigHolder.getServiceInterceptors(serviceName));
        Assert.assertEquals(64, GrpcServiceConfigHolder.getMaxInboundMessageSize(serviceName));
        config1.deactivate();
        Assert.assertNull(GrpcServiceConfigHolder.getURIProps(serviceName));
    }
}
