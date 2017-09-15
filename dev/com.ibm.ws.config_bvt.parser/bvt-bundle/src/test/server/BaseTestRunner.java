/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server;

import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

import test.server.config.dynamic.ConfigWriter;

public abstract class BaseTestRunner extends BaseHttpTest {

    protected BundleContext bundleContext;
    protected WsLocationAdmin locationService;
    protected ConfigurationAdmin configAdmin;

    @Override
    protected void activate(ComponentContext context) throws Exception {
        super.activate(context);

        bundleContext = context.getBundleContext();
        ServiceReference<?> ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        configAdmin = (ConfigurationAdmin) bundleContext.getService(ref);
    }

    protected void addTest(BaseTest test) {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_PID, test.getName());
        bundleContext.registerService(test.getServiceClasses(), test, properties);
    }

    @Reference(name = "locationService", service = WsLocationAdmin.class)
    protected void setLocationService(WsLocationAdmin ref) {
        this.locationService = ref;
    }

    protected void unsetLocationService(WsLocationAdmin ref) {
        if (ref == this.locationService) {
            this.locationService = null;
        }
    }

    private WsResource getConfigRoot() {
        return locationService.resolveResource("${server.config.dir}/server.xml");
    }

    protected ConfigWriter readConfiguration() throws Exception {
        InputStream in = getConfigRoot().get();
        try {
            return new ConfigWriter(in);
        } finally {
            close(in);
        }
    }

    protected void writeConfiguration(ConfigWriter configWriter) throws Exception {
        // try to write the entire config in one step (or least possible)
        OutputStreamWriter os = new OutputStreamWriter(getConfigRoot().putStream(), "UTF-8");
        BufferedWriter writer = new BufferedWriter(os, 25 * 1024);
        try {
            configWriter.write(writer);
        } finally {
            close(writer);
        }
        StringWriter sw = new StringWriter();
        configWriter.write(sw);
        System.out.println(sw.toString());
    }

    public static void dictionaryEquals(Dictionary one, Dictionary two) {
        if (one.size() != two.size()) {
            fail("Dictionaries are not the same: " + one + " " + two);
        }
        Enumeration e = one.keys();
        while (e.hasMoreElements()) {
            Object key = e.nextElement();
            Object value1 = one.get(key);
            Object value2 = two.get(key);
            if (value1 instanceof String[] && value2 instanceof String[]) {
                value1 = Arrays.asList((String[]) value1);
                value2 = Arrays.asList((String[]) value2);
            }
            if (!value1.equals(value2)) {
                fail("Dictionaries are not the same: " + key + " " + value1 + " " + value2);
            }
        }
    }

    private void invokeTest(String testName) throws Exception {
        Method method = getClass().getDeclaredMethod(testName);
        method.invoke(this);
    }

    public class TestDynamicConfigServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        @Override
        public void doGet(HttpServletRequest rq, HttpServletResponse rsp) throws IOException {
            PrintWriter pw = rsp.getWriter();
            rsp.setContentType("text/plain");

            String testName = rq.getParameter("testName");

            try {
                log("Begin test: " + testName);
                invokeTest(testName);
                pw.println("OK");
            } catch (NoSuchMethodException e) {
                pw.println("FAILED - Invalid test name: " + testName);
            } catch (InvocationTargetException e) {
                pw.println("FAILED");
                e.getTargetException().printStackTrace(pw);
            } catch (Throwable e) {
                pw.println("FAILED");
                e.printStackTrace(pw);
            } finally {
                log("End test: " + testName);
            }
        }

    }

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignore) {
            }
        }
    }
}
