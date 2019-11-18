package com.ibm.ws.cxf.transport.http;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * This is intended to override the HTTPTransportActivator that
 * ships from Apache CXF.  We use the Liberty Channel Framework
 * to handle the underlying transport rather than the HTTP
 * transport layer provided by CXF.
 */
public class NoOpActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        // no op
    }

    @Override
   public void stop(BundleContext context) throws Exception {
       // no op
   }
}
