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
package test.kernel.boot;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpService;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.wsspi.channelfw.ChainEventListener;

@SuppressWarnings("serial")
public class KernelBootTestRunner extends HttpServlet implements ChainEventListener {

    private HttpService http;
    private CHFWBundle cfwBundle;

    public void setHttp(HttpService http) {
        this.http = http;
    }

    public void setChfwBundle(CHFWBundle cfwBundle) {
        this.cfwBundle = cfwBundle;
    }

    public void activate() throws Exception {
        System.out.println("BVT BUNDLE: Activating " + this);

        http.registerServlet("/com.ibm.ws.kernel.boot.bvt", this, null, null);
        cfwBundle.getFramework().addChainEventListener(this, ChainEventListener.ALL_CHAINS);

        ChainData[] runningChains = cfwBundle.getFramework().getRunningChains();
        if (runningChains != null && runningChains.length > 0) {
            for (ChainData cd : runningChains) {
                chainStarted(cd);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String testMethod = req.getParameter("test");
        System.out.println("BVT BUNDLE: Calling " + testMethod);
        try {
            getClass().getMethod(testMethod).invoke(this);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new ServletException(t);
        }
    }

    private String quote(String value) {
        return value == null ? "null" : '"' + value + '"';
    }

    private void assertEquals(String method, String name, String expected, String value, Map<?, ?> props) {
        if (value == null ? expected != null : !value.equals(expected)) {
            Object[] keys = props.keySet().toArray();
            Arrays.sort(keys);

            for (Object key : keys) {
                System.err.println(key + "=" + props.get(key));
            }
            throw new Error(method + "(" + name + ") = " + quote(value) + ", expected " + quote(expected));
        }
    }

    private void assertEnvironmentVariableExists(String name) {
        String value = System.getenv(name);
        Map<?, ?> props = System.getenv();
        if (value == null) {
            Object[] keys = props.keySet().toArray();
            Arrays.sort(keys);

            for (Object key : keys) {
                System.err.println(key + "=" + props.get(key));
            }
            throw new Error("System.getenv(" + name + ") was not set as it should have been");
        }
    }

    private void assertEnvironmentVariable(String name, String expected) {
        assertEquals("System.getenv", name, expected, System.getenv(name), System.getenv());
    }

    private void assertSystemProperty(String name, String expected) {
        assertEquals("System.getProperty", name, expected, System.getProperty(name), System.getProperties());
    }

    public void testJVMArgs() {
        // The script should *always* clear these environment variables
        assertEnvironmentVariable("LOG_DIR", null);
        assertEnvironmentVariable("LOG_FILE", null);
        assertEnvironmentVariable("PID_DIR", null);
        assertEnvironmentVariable("PID_FILE", null);
        assertEnvironmentVariableExists("X_LOG_DIR");

        assertSystemProperty("com.ibm.ws.kernel.boot.bvt.jvmarg1", "1");
        assertSystemProperty("com.ibm.ws.kernel.boot.bvt.jvmarg2", "2");
        assertSystemProperty("com.ibm.ws.kernel.boot.bvt.jvmargquoted", "a b");
    }

    public void testSpecifyLogDirServerEnv() {
        String logDir = System.getenv("X_LOG_DIR");
        String outputDir = System.getenv("WLP_OUTPUT_DIR") + File.pathSeparator
                           + "com.ibm.ws.kernel.boot.env.bvt" + File.pathSeparator
                           + "logs";

        if (logDir.startsWith(outputDir)) {
            Map<?, ?> props = System.getenv();
            Object[] keys = props.keySet().toArray();
            Arrays.sort(keys);

            for (Object key : keys) {
                System.err.println(key + "=" + props.get(key));
            }

            throw new Error("Independent LOG_DIR value not correctly applied. The X_LOG_DIR variable should not be a child of the server's logs dir for this test");
        }
    }

    public void testSpecifyLogDirJVMOptions() {}

    public void testServerAndInstallServerEnv() {
        assertEnvironmentVariable("WLP_KERNEL_BOOT_BVT_INSTALL", "install");
        assertEnvironmentVariable("WLP_KERNEL_BOOT_BVT_OVERRIDE", "server");
        assertEnvironmentVariable("WLP_KERNEL_BOOT_BVT_SERVER", "server");
        assertEnvironmentVariable("WLP_KERNEL_BOOT_BVT_CHARS", "a b\\c");
        assertEnvironmentVariable("WLP_KERNEL_BOOT_BVT_CR", "cr");
    }

    public void testServerAndInstallJVMOptions() {
        assertSystemProperty("com.ibm.ws.kernel.boot.bvt.install", null);
        assertSystemProperty("com.ibm.ws.kernel.boot.bvt.override", "server");
        assertSystemProperty("com.ibm.ws.kernel.boot.bvt.server", "server");
        assertSystemProperty("com.ibm.ws.kernel.boot.bvt.chars", "a b\\c");
        assertSystemProperty("com.ibm.ws.kernel.boot.bvt.cr", "cr");
    }

    public void testInstallOnlyServerEnv() {
        assertEnvironmentVariable("WLP_KERNEL_BOOT_BVT_INSTALL", "install");
        assertEnvironmentVariable("WLP_KERNEL_BOOT_BVT_OVERRIDE", "install");
        assertEnvironmentVariable("WLP_KERNEL_BOOT_BVT_SERVER", null);
    }

    public void testInstallOnlyJVMOptions() {
        assertSystemProperty("com.ibm.ws.kernel.boot.bvt.install", "install");
        assertSystemProperty("com.ibm.ws.kernel.boot.bvt.override", "install");
        assertSystemProperty("com.ibm.ws.kernel.boot.bvt.server", null);
        assertSystemProperty("java.awt.headless", "true");
    }

    @Override
    public void chainInitialized(ChainData chainData) {}

    @Override
    public void chainStarted(ChainData chainData) {
        File started = new File("started.txt");
        try {
            started.createNewFile();
            System.out.println("KernelBootTestRunner has been activated: " + started.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Could not create file: " + started.getAbsolutePath() + ", " + e.toString());
        }
    }

    @Override
    public void chainStopped(ChainData chainData) {}

    @Override
    public void chainQuiesced(ChainData chainData) {}

    @Override
    public void chainDestroyed(ChainData chainData) {}

    @Override
    public void chainUpdated(ChainData chainData) {}
}
