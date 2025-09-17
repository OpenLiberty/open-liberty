/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package websphere.listener;

import javax.servlet.ServletContext;

import com.ibm.websphere.servlet.event.ApplicationEvent;
import com.ibm.websphere.servlet.event.ApplicationListener;
import com.ibm.websphere.servlet.event.ServletContextEventSource;

/*
 * WebSphere Servlet Event ApplicationListener.
 *
 * Register using the WebContainer's "listeners" in server.xml
 *
 *      <webContainer listeners="websphere.servlet.listener.WebSphereApplicationListener"/>
 *
 * This is the main entry which is invoked during server/application startup.
 * It then retrieves the ServletContextEventSource's attribute "com.ibm.websphere.servlet.event.ServletContextEventSource"
 * to register all other application's listeners.
 *
 * When request is invoked, these listeners will add their data into the response outputStream prior
 * to the target servlet.  The client will then assert on the response data.
 *
 */

public class WebSphereApplicationListener implements ApplicationListener {
    private static final String CLASS_NAME = WebSphereApplicationListener.class.getName();
    public static final String WEBSPHERE_ATT = "WEBSPHERE_API";
    private final String WEBSPHERE_ATT_VALUE = "WEBSPHERE Servlet API using ApplicationListener.";

    public static StringBuffer OUTBUFFER; //Hold all data from all resources.  It is written back to client from the WebSphereFilterInvocationListener.onFilterFinishDoFilter

    ServletContext context = null;

    ServletContextEventSource evtSource;

    //All listeners
    WebSphereServletListener wServletL;
    WebSphereServletInvocationListener wServletIL;
    WebSphereFilterListener wFilterL;
    WebSphereFilterInvocationListener wFilterIL;
    WebSphereServletErrorListener wServletErrL;
    WebSphereFilterErrorListener wFilterErrL;
    WebSphereFilterListenerImplExt wFilterLImp;

    public WebSphereApplicationListener() {
        log("WebSphereApplicationListener constructor");
        OUTBUFFER = new StringBuffer();
    }

    @Override
    public void onApplicationAvailableForService(ApplicationEvent arg0) {
        log("onApplicationAvailableForService, ServletContext from ApplicationEvent [" + context);
    }

    @Override
    public void onApplicationEnd(ApplicationEvent arg0) {
        log("onApplicationEnd, ServletContext from ApplicationEvent [" + context);

        if (evtSource != null) {
            log("onApplicationEnd, deregister all listeners using [" + evtSource + "]");
            evtSource.removeServletListener(wServletL);
            evtSource.removeServletInvocationListener(wServletIL);

            evtSource.removeFilterListener(wFilterL);
            evtSource.removeFilterInvocationListener(wFilterIL);

            evtSource.removeServletErrorListener(wServletErrL);
            evtSource.removeFilterErrorListener(wFilterErrL);
            evtSource.removeFilterListener(wFilterLImp);

            evtSource = null;
        }

    }

    @Override
    public void onApplicationStart(ApplicationEvent arg0) {
        context = arg0.getServletContext();

        //servlet will retrieve this context attribute and send in response to client for verify
        context.setAttribute(WebSphereApplicationListener.WEBSPHERE_ATT, WEBSPHERE_ATT_VALUE);

        log("onApplicationStart, set WebSphere context attribute using WebSphere API [" + context.getAttribute(WEBSPHERE_ATT) + "], context [" + context + "]");

        OUTBUFFER.append("(MAIN) WebSpshereApplicationListener.onApplicationStart \n");

        /*
         * Register other application's listener via WebSphere ServletContextEventSource's
         * "com.ibm.websphere.servlet.event.ServletContextEventSource" attribute
         */
        evtSource = (ServletContextEventSource) context.getAttribute(ServletContextEventSource.ATTRIBUTE_NAME);
        if (evtSource != null) {
            log("onApplicationStart, Register listeners using ServletContextEventSource [" + evtSource + "]");

            //filter
            evtSource.addFilterListener(wFilterL = new WebSphereFilterListener());
            evtSource.addFilterInvocationListener(wFilterIL = new WebSphereFilterInvocationListener());

            //servlet
            evtSource.addServletListener(wServletL = new WebSphereServletListener());
            evtSource.addServletInvocationListener(wServletIL = new WebSphereServletInvocationListener());

            //servlet error
            evtSource.addServletErrorListener(wServletErrL = new WebSphereServletErrorListener());

            //filter error
            evtSource.addFilterErrorListener(wFilterErrL = new WebSphereFilterErrorListener());

            //FilterListenerImpl
            evtSource.addFilterListener(wFilterLImp = new WebSphereFilterListenerImplExt());
        }
    }

    @Override
    public void onApplicationUnavailableForService(ApplicationEvent arg0) {
        log("onApplicationUnavailableForService, ServletContext from ApplicationEvent [" + context);
    }

    private void log(String s) {
        System.out.println("\t" + CLASS_NAME + ": " + s);
    }
}
