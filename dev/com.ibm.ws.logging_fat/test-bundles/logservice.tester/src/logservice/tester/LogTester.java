/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package logservice.tester;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogService;
import org.osgi.service.log.Logger;

@WebServlet(urlPatterns = { "log" })
public class LogTester extends HttpServlet {

    enum EventTest {
        bundle,
        service,
        framework
    }

    private static final long serialVersionUID = 1L;

    private volatile Bundle b1;
    private volatile Bundle b2;

    @Override
    public void init(ServletConfig config) throws ServletException {
        BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();
        File bundles = bc.getDataFile("bundles");
        bundles.mkdir();
        try {
            b1 = bc.installBundle(createBundle(bundles, ".1").toURI().toString());
            b2 = bc.installBundle(createBundle(bundles, ".2").toURI().toString());
            b1.start();
            b2.start();
        } catch (Exception e) {
            throw new ServletException(e);
        }

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference<LogService> ref = bc.getServiceReference(LogService.class);
        if (ref == null) {
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        if (!testEvent(req)) {
            testLog(bc, ref, req);
        }

        resp.getWriter().print("DONE");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private boolean testEvent(HttpServletRequest req) throws ServletException {
        EventTest eventTest = getEvent(req);
        if (eventTest == null) {
            return false;
        }
        try {
            switch (eventTest) {
                case bundle:
                    b1.stop();
                    b1.start();
                    b2.stop();
                    b2.start();
                    break;
                case service:
                    b1.getBundleContext().registerService(Object.class, new Object(), null).unregister();
                    b2.getBundleContext().registerService(Object.class, new Object(), null).unregister();
                    break;
                case framework:
                    Bundle sb = FrameworkUtil.getBundle(getClass()).getBundleContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
                    FrameworkWiring fwkWiring = sb.adapt(FrameworkWiring.class);
                    final CountDownLatch refreshLatch = new CountDownLatch(1);
                    fwkWiring.refreshBundles(Arrays.asList(b1, b2), new FrameworkListener() {

                        @Override
                        public void frameworkEvent(FrameworkEvent arg0) {
                            refreshLatch.countDown();
                        }
                    });
                    refreshLatch.await(30, TimeUnit.SECONDS);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
        return true;
    }

    private void testLog(BundleContext bc, ServiceReference<LogService> ref, HttpServletRequest req) {
        try {
            LogService logService = bc.getService(ref);
            Logger logger = logService.getLogger(Logger.ROOT_LOGGER_NAME);
            log(logger, req, ref);
        } finally {
            bc.ungetService(ref);
        }
    }

    private void log(Logger logger, HttpServletRequest req, ServiceReference<LogService> ref) {
        LogLevel level = getLevel(req);
        String msg = getMessage(req);
        Throwable t = getThrowable(req);
        List<Object> args = new ArrayList<Object>(2);
        if (t != null) {
            args.add(t);
        }
        if (includeRef(req)) {
            args.add(ref);
        }
        switch (level) {
            case AUDIT:
                logger.audit(msg, args.toArray());
                break;
            case ERROR:
                logger.error(msg, args.toArray());
                break;
            case WARN:
                logger.warn(msg, args.toArray());
                break;
            case INFO:
                logger.info(msg, args.toArray());
                break;
            case DEBUG:
                logger.debug(msg, args.toArray());
                break;
            case TRACE:
                logger.trace(msg, args.toArray());
                break;
            default:
                break;
        }
    }

    private EventTest getEvent(HttpServletRequest req) {
        String event = req.getParameter("event");
        return event == null ? null : EventTest.valueOf(event);
    }

    private LogLevel getLevel(HttpServletRequest req) {
        String level = req.getParameter("level");
        return level == null ? LogLevel.AUDIT : LogLevel.valueOf(level);
    }

    private String getMessage(HttpServletRequest req) {
        String msg = req.getParameter("msg");
        return msg == null ? "TEST" : msg;
    }

    private Throwable getThrowable(HttpServletRequest req) {
        String throwableMsg = req.getParameter("throw");
        return throwableMsg == null ? null : new RuntimeException(throwableMsg);
    }

    private boolean includeRef(HttpServletRequest req) {
        return req.getParameter("service") != null;
    }

    public static File createBundle(File outputDir, String id) throws IOException {
        File file = new File(outputDir, "bundle" + id + ".jar");
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(file), createManifest(id));
        jos.flush();
        jos.close();
        return file;
    }

    private static Manifest createManifest(String id) {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.putValue("Manifest-Version", "1.0");
        attributes.putValue("Bundle-ManifestVersion", "2");
        attributes.putValue("Bundle-SymbolicName", "bundle" + id);
        return manifest;
    }
}
