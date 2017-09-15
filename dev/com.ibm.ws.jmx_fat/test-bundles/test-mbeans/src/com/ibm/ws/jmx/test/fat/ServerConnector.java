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
package com.ibm.ws.jmx.test.fat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jmx.PlatformMBeanService;
import com.ibm.wsspi.kernel.service.utils.OsgiPropertyUtils;

/**
 *
 */
public class ServerConnector {
    private static final TraceComponent tc = Tr.register(ServerConnector.class);

    String URL;
    JMXConnectorServer cs;
    ComponentContext context;
    ServiceReference<PlatformMBeanService> mbeanServerRef;
    Registry registry;

    public ServerConnector() {
        super();
        cs = null;
        context = null;
        mbeanServerRef = null;
        registry = null;
    }

    public void activate(ComponentContext compContext) {
        context = compContext;

        int port = Integer.valueOf(OsgiPropertyUtils.getProperty("bvt.prop.JMXTest", "8999"));
        final String URL = "service:jmx:rmi:///jndi/rmi://localhost:" + port + "/server";

        registry = null;
        try {
            registry = LocateRegistry.createRegistry(port);
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
            e1.printStackTrace();
            throw new RuntimeException(e1);
        }
        Tr.info(tc, "STARTED", URL);

        Runnable r = new Runnable() {

            @Override
            public void run() {
                PlatformMBeanService mbeanServer = null;
                try {
                    mbeanServer = context.locateService("jmxServer", mbeanServerRef);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                Tr.info(tc, "SERVER", strings(mbeanServer.getMBeanServer().getDomains()));

                JMXServiceURL url = null;
                try {
                    url = new JMXServiceURL(URL);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

                try {
                    cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbeanServer.getMBeanServer());
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

                try {
                    cs.start();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                Tr.info(tc, "SERVER_READY", strings(cs.getConnectionIds()));
            }
        };
        Thread thr = new Thread(r);
        thr.start();

    }

    public void deactivate(ComponentContext compContext) {
        try {
            cs.stop();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        context = null;
        try {
            UnicastRemoteObject.unexportObject(registry, true);
        } catch (NoSuchObjectException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
            e.printStackTrace();
        }
        registry = null;
        Tr.info(tc, "SERVER_STOPPED");
    }

    public void setJmxServer(ServiceReference<PlatformMBeanService> mbeanServerRef) {
        this.mbeanServerRef = mbeanServerRef;
    }

    public void unsetJmxServer(ServiceReference<PlatformMBeanService> mbeanServer) {
        cs = null;
    }

    private static String strings(String[] strs) {
        StringBuilder str = new StringBuilder("[");
        if (strs != null) {
            boolean first = true;
            for (String s : strs) {
                if (first) {
                    first = false;
                } else {
                    str.append(',');
                }
                str.append(s);
            }
        }
        str.append(']');
        return str.toString();
    }
}
