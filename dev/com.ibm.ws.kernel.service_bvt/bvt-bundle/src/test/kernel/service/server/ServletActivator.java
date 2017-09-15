/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.kernel.service.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Test bundle component.
 */
public class ServletActivator implements SynchronousBundleListener {
    private static final TraceComponent tc = Tr.register(ServletActivator.class, "FeatureManager");

    private volatile HttpService http = null;
    private BundleContext bContext = null;
    /** Used to wait until osgiConsole is added */
    private final AtomicBoolean osgiConsoleAdded = new AtomicBoolean(false);

    /** Used to wait until osgiConsole is removed */
    private final AtomicBoolean osgiConsoleRemoved = new AtomicBoolean(false);

    protected void activate(ComponentContext context) {
        Tr.event(this, tc, "Test component activated");
        bContext = context.getBundleContext();
        bContext.addBundleListener(this);

        ConfigWriterServlet configServlet = new ConfigWriterServlet();
        try {
            http.registerServlet("/config", configServlet, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void deactivate(ComponentContext context) {
        Tr.event(this, tc, "Test component deactivated");
        bContext.removeBundleListener(this);
        if (http != null) {
            http.unregister("/config");
        }
        http = null;
    }

    protected synchronized void setHttp(HttpService ref) {
        this.http = ref;
    }

    protected synchronized void unsetHttp(HttpService ref) {
        if (ref == this.http)
            this.http = null;
    }

    /**
     * Config servlet
     */
    class ConfigWriterServlet extends HttpServlet {

        /**  */
        private static final long serialVersionUID = 1L;

        @Override
        public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            final String uri = req.getRequestURI();
            Tr.event(this, tc, "Test: Received request: " + uri);

            try {
                resp.setStatus(200);
                resp.setContentType("text/xml");

                PrintWriter pw = resp.getWriter();
                if ("/config/include".equals(uri)) {
                    pw.println(" <server>");
                    pw.println("<featureManager>");
                    pw.println("<feature>osgiConsole-1.0</feature>");
                    pw.println("</featureManager>");
                    pw.println("</server>");
                } else if ("/config/isAdded".equals(uri)) {
                    if (waitForAdd()) {
                        pw.println("ADD FEATURE SUCCESS");
                    } else {
                        pw.println("ADD FEATURE TIMEOUT");
                    }
                }

                pw.close();
            } catch (Throwable t) {
                if (!(t instanceof IOException)) {
                    t = new IOException("Wrapped Throwable: " + t.getMessage(), t);
                }
                throw (IOException) t;
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public void bundleChanged(BundleEvent event) {
        Bundle b = event.getBundle();
        if (b.getSymbolicName().equals("com.ibm.ws.org.eclipse.equinox.console")) {
            if (event.getType() == BundleEvent.INSTALLED) {
                synchronized (osgiConsoleAdded) {
                    osgiConsoleAdded.set(true);
                    osgiConsoleAdded.notify();
                }
            } else if (event.getType() == BundleEvent.UNINSTALLED) {
                synchronized (osgiConsoleRemoved) {
                    osgiConsoleRemoved.set(true);
                    osgiConsoleRemoved.notify();
                }
            }
        }
    }

    public boolean waitForAdd() {
        if (osgiConsoleAdded.get() == false) {
            synchronized (osgiConsoleAdded) {
                try {
                    osgiConsoleAdded.wait(10000); // 10 seconds
                } catch (InterruptedException ex) {
                }
            }
        }
        return osgiConsoleAdded.get();
    }

    public boolean waitForRemove() {
        if (osgiConsoleRemoved.get() == false) {
            synchronized (osgiConsoleRemoved) {
                try {
                    osgiConsoleRemoved.wait(10000); // 10 seconds
                } catch (InterruptedException ex) {
                }
            }
        }
        return osgiConsoleRemoved.get();
    }

}
