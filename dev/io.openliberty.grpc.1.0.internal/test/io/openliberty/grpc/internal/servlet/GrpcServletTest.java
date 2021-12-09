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
package io.openliberty.grpc.internal.servlet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.ibm.ws.http2.GrpcServletServices;
import com.ibm.ws.http2.GrpcServletServices.ServiceInformation;

import test.common.SharedOutputManager;

/**
 * Basic unit tests for the grpcServlet feature
 */
public class GrpcServletTest {

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
     * Verify that the GrpcServletServices map works as expected
     */
    @Test
    public void testGrpcServletServices() {
        String grpcService1 = "helloworld.Greeter1";
        String grpcService2 = "helloworld.Greeter2";
        String grpcService3 = "helloworld.Greeter3";
        String grpcService4 = "helloworld.Greeter4";
        String grpcService5 = "helloworld.Greeter5";
        String grpcService6 = "helloworld.Greeter6";
        String grpcService7 = "helloworld.Greeter7";
        String grpcService8 = "helloworld.Greeter8";
        String app1 = "app1";
        String app2 = "app2";
        String app3 = "app3";
        
        CountDownLatch latch = new CountDownLatch(3);

        // simulate async app startup
        Thread appThread1 = new Thread() {
            public void run() {
                GrpcServletServices.addServletGrpcService(grpcService1, app1, GrpcServletServices.class);
                GrpcServletServices.addServletGrpcService(grpcService6, app1, GrpcServletServices.class);
                GrpcServletServices.addServletGrpcService(grpcService8, app1, GrpcServletServices.class);
                GrpcServletServices.addServletGrpcService(grpcService3, app1, GrpcServletServices.class);
                latch.countDown();
            }
        };
        Thread appThread2 = new Thread() {
            public void run() {
                GrpcServletServices.addServletGrpcService(grpcService4, app2, GrpcServletServices.class);
                GrpcServletServices.addServletGrpcService(grpcService5, app2, GrpcServletServices.class);
                latch.countDown();
            }
        };
        Thread appThread3 = new Thread() {
            public void run() {
                GrpcServletServices.addServletGrpcService(grpcService2, app3, GrpcServletServices.class);
                GrpcServletServices.addServletGrpcService(grpcService7, app3, GrpcServletServices.class);
                latch.countDown();
            }
        };

        appThread1.start();
        appThread2.start();
        appThread3.start();
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail("GrpcServletServices.addServletGrpcService() "
                    + "timed out");
        }

        // make sure all 8 applications were added
        Map<String, ServiceInformation> services = GrpcServletServices.getServletGrpcServices();
        Assert.assertEquals(8, services.size());
        try {
            GrpcServletServices.addServletGrpcService(grpcService1, app2, GrpcServletServices.class);
            Assert.fail("GrpcServletServices.addServletGrpcService() "
                    + "should have failed when passed a duplicate service name");
        } catch (RuntimeException re) {}
        
        // verify the services are mapped correctly
        Assert.assertEquals(8, services.size());
        Assert.assertEquals(app1, services.get(grpcService1).getContextRoot());
        Assert.assertEquals(GrpcServletServices.class, services.get(grpcService1).getServiceClass());
        Assert.assertEquals(app2, services.get(grpcService4).getContextRoot());
        Assert.assertEquals(app3, services.get(grpcService7).getContextRoot());
        
        // remove a few services and make sure the mappings are correct
        GrpcServletServices.removeServletGrpcService(grpcService1);
        GrpcServletServices.removeServletGrpcService(grpcService4);
        GrpcServletServices.removeServletGrpcService(grpcService7);
        Assert.assertEquals(5, services.size());
        Assert.assertNull(services.get(grpcService1));
    }
    
    /**
     * Test the GrpcServletApplication object
     */
    @Test
    public void testGrpcServletApplication() {
        String serviceClassName = "io.openliberty.grpc.internal.servlet.ExampleServletService";
        Set<String> serviceClassNames = new HashSet<String>();
        serviceClassNames.add(serviceClassName);
        String serviceName = "helloworld.Greeter";
        String contextRoot = "sample_application";

        // create a GrpcServletApplication
        GrpcServletApplication app = new GrpcServletApplication();
        app.addServiceClassName(serviceClassName);
        app.addServiceName(serviceName, contextRoot, GrpcServletServices.class);
        
        // make sure that GrpcServletApplication is correct 
        Assert.assertEquals(1, app.getServiceClassNames().size());
        Assert.assertEquals(1, GrpcServletServices.getServletGrpcServices().size());
        Assert.assertTrue(app.getServiceClassNames().contains(serviceClassName));
        // GrpcServletServices should have been updated with the new service
        Assert.assertEquals(contextRoot, GrpcServletServices.getServletGrpcServices().get(serviceName).getContextRoot());
        Assert.assertEquals(GrpcServletServices.class, GrpcServletServices.getServletGrpcServices().get(serviceName).getServiceClass());


        // destroy the app and make sure everything has been cleaned up
        app.destroy();
        Assert.assertNull(app.getServiceClassNames());
        Assert.assertNull(GrpcServletServices.getServletGrpcServices());
        
        // check that the same path can be re-added
        app = new GrpcServletApplication();
        app.addServiceClassName(serviceClassName);
        app.addServiceName(serviceName, contextRoot, GrpcServletServices.class);
        Assert.assertEquals(contextRoot, GrpcServletServices.getServletGrpcServices().get(serviceName).getContextRoot());
        app.destroy();
    }

    /**
     * Verify that the gRPC URL translation works as expected
     */
    @Test
    public void testPathTranslation() {
        String normalPath1 = "app_context_root/helloworld.Greeter/SayHello";
        String normalPath2 = "helloworld.Greeter/SayHello";
        String badPath1 = "//";
        String badPath2 = "helloworld.Greeter/";
        String badPath3 = "/";
        String badPath4 = "/SayHello/";
        String badPath5 = "helloworld.Greeter/SayHello/";
        Assert.assertEquals("helloworld.Greeter/SayHello", GrpcServletUtils.translateLibertyPath(normalPath1));
        Assert.assertEquals("helloworld.Greeter/SayHello", GrpcServletUtils.translateLibertyPath(normalPath2));
        Assert.assertEquals("/", GrpcServletUtils.translateLibertyPath(badPath1));
        Assert.assertEquals(badPath2, GrpcServletUtils.translateLibertyPath(badPath2));
        Assert.assertEquals(badPath3, GrpcServletUtils.translateLibertyPath(badPath3));
        Assert.assertEquals("SayHello/", GrpcServletUtils.translateLibertyPath(badPath4));
        Assert.assertEquals("SayHello/", GrpcServletUtils.translateLibertyPath(badPath5));
    }
    

}
