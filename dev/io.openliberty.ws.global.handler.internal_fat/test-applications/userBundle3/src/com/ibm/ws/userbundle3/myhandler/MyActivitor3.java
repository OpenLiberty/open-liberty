package com.ibm.ws.userbundle3.myhandler;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.webservices.handler.Handler;

public class MyActivitor3 implements BundleActivator {
    private ServiceRegistration<Handler> serviceRegistration1;
    private ServiceRegistration<Handler> serviceRegistration2;

    @Override
    public void start(BundleContext context) throws Exception {

        final Hashtable<String, Object> handlerProps = new Hashtable<String, Object>();

        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 2);
        TestHandler1InBundle3 testHandler1 = new TestHandler1InBundle3();
        serviceRegistration1 = context.registerService(Handler.class, testHandler1, handlerProps);

        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 4);
        TestHandler2InBundle3 testHandler2 = new TestHandler2InBundle3();
        serviceRegistration2 = context.registerService(Handler.class, testHandler2, handlerProps);

        System.out.println("in start method in bundle activator");

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (serviceRegistration1 != null) {
            serviceRegistration1.unregister();
        }
        if (serviceRegistration2 != null) {
            serviceRegistration2.unregister();
        }

    }

}
