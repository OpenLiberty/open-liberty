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
package test.server.feature;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.feature.LibertyFeature;

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

    /** Used to wait until user is added */
    private final AtomicBoolean userAdded = new AtomicBoolean(false);

    /** Used to wait until user is removed */
    private final AtomicBoolean userRemoved = new AtomicBoolean(false);

    /** Used to wait until product is added */
    private final AtomicBoolean productAdded = new AtomicBoolean(false);

    /** Used to wait until product is removed */
    private final AtomicBoolean productRemoved = new AtomicBoolean(false);

    /** Map of feature names to liberty features */
    private ServiceTracker<LibertyFeature, String> libertyFeatures;
    private final Map<String, CountDownLatch> featureServiceLatches = new HashMap<String, CountDownLatch>();

    /**
     *
     */
    public ServletActivator() {
        featureServiceLatches.put("ADD:osgiConsole-1.0", new CountDownLatch(1));
        featureServiceLatches.put("REMOVE:osgiConsole-1.0", new CountDownLatch(1));
        featureServiceLatches.put("ADD:usr:usertest", new CountDownLatch(1));
        featureServiceLatches.put("REMOVE:usr:usertest", new CountDownLatch(1));
        featureServiceLatches.put("ADD:testproduct:prodtest-1.0", new CountDownLatch(1));
        featureServiceLatches.put("REMOVE:testproduct:prodtest-1.0", new CountDownLatch(1));
    }

    protected void activate(ComponentContext context) {
        Tr.event(this, tc, "Test component activated");
        bContext = context.getBundleContext();
        bContext.addBundleListener(this);

        libertyFeatures = new ServiceTracker<LibertyFeature, String>(bContext, LibertyFeature.class, new ServiceTrackerCustomizer<LibertyFeature, String>() {
            @Override
            public String addingService(ServiceReference<LibertyFeature> ref) {
                String featureName = (String) ref.getProperty("ibm.featureName");
                System.out.println("Tracking feature service: " + featureName);
                CountDownLatch latch = featureServiceLatches.get("ADD:" + featureName);
                if (latch != null) {
                    latch.countDown();
                }
                return featureName;
            }

            @Override
            public void modifiedService(ServiceReference<LibertyFeature> ref, String featureName) {
                // nothing
            }

            @Override
            public void removedService(ServiceReference<LibertyFeature> ref, String featureName) {
                System.out.println("Untracking feature service: " + featureName);
                CountDownLatch latch = featureServiceLatches.get("REMOVE:" + featureName);
                if (latch != null) {
                    latch.countDown();
                }
            }
        });

        libertyFeatures.open();

        AddRemoveServlet addRemove = new AddRemoveServlet();
        try {
            http.registerServlet("/feature", addRemove, null, null);
            Tr.audit(tc, "BVT: ServletActivator registered the /feature context root");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void deactivate(ComponentContext context) {
        Tr.event(this, tc, "Test component deactivated");
        bContext.removeBundleListener(this);
        if (http != null) {
            http.unregister("/feature");
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
     * Test servlet.
     */
    class AddRemoveServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        @Override
        public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            final String uri = req.getRequestURI();
            Tr.event(this, tc, "Test: Received request: " + uri);

            try {
                PrintWriter pw = resp.getWriter();
                resp.setStatus(200);
                resp.setContentType("text/plain");

                if ("/feature/hello".equals(uri)) {
                    pw.println("Hello World");
                } else if ("/feature/add".equals(uri)) {
                    if (waitForAdd()) {
                        pw.println("ADD FEATURE SUCCESS");
                    } else {
                        pw.println("ADD FEATURE TIMEOUT");
                    }
                } else if ("/feature/remove".equals(uri)) {
                    if (waitForRemove()) {
                        pw.println("REMOVE FEATURE SUCCESS");
                    } else {
                        pw.println("REMOVE FEATURE TIMEOUT");
                    }
                } else if ("/feature/useradd".equals(uri)) {
                    if (waitForUserAdd()) {
                        pw.println("ADD USER FEATURE SUCCESS");
                    } else {
                        pw.println("ADD USER FEATURE TIMEOUT");
                    }
                } else if ("/feature/userremove".equals(uri)) {
                    if (waitForUserRemove()) {
                        pw.println("REMOVE USER FEATURE SUCCESS");
                    } else {
                        pw.println("REMOVE USER FEATURE TIMEOUT");
                    }
                } else if ("/feature/productadd".equals(uri)) {
                    if (waitForProductAdd()) {
                        pw.println("ADD PRODUCT FEATURE SUCCESS");
                    } else {
                        pw.println("ADD PRODUCT FEATURE TIMEOUT");
                    }
                } else if ("/feature/productremove".equals(uri)) {
                    if (waitForProductRemove()) {
                        pw.println("REMOVE PRODUCT FEATURE SUCCESS");
                    } else {
                        pw.println("REMOVE PRODUCT FEATURE TIMEOUT");
                    }
                } else if ("/feature/service/add".equals(uri)) {
                    String featureName = req.getParameter("feature");
                    Tr.event(this, tc, "finding: " + featureName);
                    CountDownLatch latch = featureServiceLatches.get("ADD:" + featureName);
                    if (latch == null) {
                        pw.println("NO FEATURE SERVICE LATCH FOUND: " + featureName);
                    } else if (latch.await(10, TimeUnit.SECONDS)) {
                        pw.println("LIBERTY FEATURE SERVICE FOUND");
                    } else {
                        pw.println("LIBERTY FEATURE SERVICE NOT FOUND: " + featureName);
                    }
                } else if ("/feature/service/remove".equals(uri)) {
                    String featureName = req.getParameter("feature");
                    Tr.event(this, tc, "finding: " + featureName);
                    CountDownLatch latch = featureServiceLatches.get("REMOVE:" + featureName);
                    if (latch == null) {
                        pw.println("NO FEATURE SERVICE LATCH FOUND: " + featureName);
                    } else if (latch.await(10, TimeUnit.SECONDS)) {
                        pw.println("LIBERTY FEATURE SERVICE UNREGISTERED");
                    } else {
                        pw.println("LIBERTY FEATURE SERVICE NOT UNREGISTERED: " + featureName);
                    }
                } else {
                    pw.print("unknownTest: " + uri);
                }
            } catch (Throwable t) {
                if (!(t instanceof IOException)) {
                    t = new IOException("Wrapped Throwable: " + t.getMessage(), t);
                }
                throw (IOException) t;
            }
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

            ServletInputStream is = req.getInputStream();
            ServletOutputStream os = resp.getOutputStream();
            int clength = req.getContentLength();
            byte[] data = new byte[clength];
            int offset = 0;
            do {
                int rc = is.read(data, offset, data.length - offset);
                if (-1 == rc) {
                    break;
                }
                offset += rc;
            } while (offset < data.length);
            os.print(new String(data, 0, offset));
            os.println();
            resp.setContentType(req.getContentType());
            is.close();
            os.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void bundleChanged(BundleEvent event) {
        Bundle b = event.getBundle();
        System.err.println("BUNDLE IS " + b.getSymbolicName());
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
        } else if (b.getSymbolicName().equals("usertest")) {
            if (event.getType() == BundleEvent.INSTALLED) {
                synchronized (userAdded) {
                    userAdded.set(true);
                    userAdded.notify();
                }
            } else if (event.getType() == BundleEvent.UNINSTALLED) {
                synchronized (userRemoved) {
                    userRemoved.set(true);
                    userRemoved.notify();
                }
            }
        } else if (b.getSymbolicName().equals("com.ibm.ws.prodtest.internal")) {
            if (event.getType() == BundleEvent.INSTALLED) {
                synchronized (productAdded) {
                    productAdded.set(true);
                    productAdded.notify();
                }
            } else if (event.getType() == BundleEvent.UNINSTALLED) {
                synchronized (productRemoved) {
                    productRemoved.set(true);
                    productRemoved.notify();
                }
            }
        }
    }

    //Before we were piecemeal updating timeouts by 5-10 second increments.
    //I don't see why not to make one unified timeout number for all feature updates.
    private final int UPDATE_TIMEOUT = 20000;

    public boolean waitForAdd() {
        if (osgiConsoleAdded.get() == false) {
            synchronized (osgiConsoleAdded) {
                try {
                    osgiConsoleAdded.wait(UPDATE_TIMEOUT);
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
                    osgiConsoleRemoved.wait(UPDATE_TIMEOUT);
                } catch (InterruptedException ex) {
                }
            }
        }
        return osgiConsoleRemoved.get();
    }

    public boolean waitForUserAdd() {
        if (userAdded.get() == false) {
            synchronized (userAdded) {
                try {
                    userAdded.wait(UPDATE_TIMEOUT);
                } catch (InterruptedException ex) {
                }
            }
        }
        return userAdded.get();
    }

    public boolean waitForUserRemove() {
        if (userRemoved.get() == false) {
            synchronized (userRemoved) {
                try {
                    userRemoved.wait(UPDATE_TIMEOUT);
                } catch (InterruptedException ex) {
                }
            }
        }
        return userRemoved.get();
    }

    public boolean waitForProductAdd() {
        if (productAdded.get() == false) {
            synchronized (productAdded) {
                try {
                    productAdded.wait(UPDATE_TIMEOUT);
                } catch (InterruptedException ex) {
                }
            }
        }
        return productAdded.get();
    }

    public boolean waitForProductRemove() {
        if (productRemoved.get() == false) {
            synchronized (productRemoved) {
                try {
                    productRemoved.wait(UPDATE_TIMEOUT);
                } catch (InterruptedException ex) {
                }
            }
        }
        return productRemoved.get();
    }

}
