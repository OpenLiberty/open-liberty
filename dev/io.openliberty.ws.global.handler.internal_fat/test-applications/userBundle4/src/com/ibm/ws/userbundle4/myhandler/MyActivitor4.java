package com.ibm.ws.userbundle4.myhandler;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.webservices.handler.Handler;

public class MyActivitor4 implements BundleActivator {
    private ServiceRegistration<Handler> serviceRegistration1;
    private ServiceRegistration<Handler> serviceRegistration2;

    @Override
    public void start(BundleContext context) throws Exception {

        final Hashtable<String, Object> handlerProps = new Hashtable<String, Object>();

        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 2);
        TestHandler1InBundle4 testHandler1 = new TestHandler1InBundle4();
        serviceRegistration1 = context.registerService(Handler.class, testHandler1, handlerProps);

        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 3);
        TestHandler2InBundle4 testHandler2 = new TestHandler2InBundle4();
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
