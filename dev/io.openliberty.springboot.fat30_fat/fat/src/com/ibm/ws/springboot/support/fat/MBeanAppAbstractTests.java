/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.JMX;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerDelegateMBean;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public abstract class MBeanAppAbstractTests extends AbstractSpringTests {
    public static LibertyServer mbeanServer = LibertyServerFactory.getLibertyServer("SpringBootTests");

    @Test
    public void testMBeanLocalConnector() throws Exception {
        String serverRoot = server.getServerRoot();
        LocalConnector lc = new LocalConnector(serverRoot);
        checkMBeanServerConnection(lc.getMBeanServer());
    }

    public void checkMBeanServerConnection(MBeanServerConnection mbsc) throws Exception {

        MBeanServer mbserver = ManagementFactory.getPlatformMBeanServer();
        assertNotNull("Server must not be null", mbsc);

        int numBeans = mbsc.getMBeanCount();
        assertTrue("Number of beans should be greater than or equal to 1, numBeans=" + numBeans, numBeans >= 1);

        Set<ObjectName> set = mbsc.queryNames(null, null);

        ObjectName on1 = new ObjectName("bean", "name", "testBean1");

        assertTrue("Set must contain object name '" + on1 + "': found " + set, set.contains(on1));

        MBeanInfo info = mbsc.getMBeanInfo(on1);
        assertNotNull("MBeanInfo for object name 1 must not be null", info);

        MBeanServerDelegateMBean serverDelegate = JMX.newMBeanProxy(mbsc, MBeanServerDelegate.DELEGATE_NAME, MBeanServerDelegateMBean.class);
        System.out.println("Server delegate: " + serverDelegate);
        System.out.println("Mbean server id: " + serverDelegate.getMBeanServerId());
        assertTrue("Expected that server ID starts with bean",
                   serverDelegate.getMBeanServerId().startsWith("WebSphere"));

        Object[] params_set1 = { "Hello", "MBeanAppTests20" };
        String[] signature_set1 = { "java.lang.String", "java.lang.String" };

        System.out.println("Default domain of MBeanServerConnection is: " + mbsc.getDefaultDomain());

        mbsc.invoke(on1, "setMessage", params_set1, signature_set1);
        Object[] params1 = { "MBeanAppAbstractTests" };
        String[] signature1 = { "java.lang.String" };
        Object result1 = mbsc.invoke(on1, "getMessage", params1, signature1);

        System.out.println("This is the result of the JMX invoked MBean operation for Get Message: " + result1.toString());
        assertEquals("Hello", result1);

        Object[] params_set2 = { "MBeanAppTests20" };
        String[] signature_set2 = { "java.lang.String" };

        mbsc.invoke(on1, "incrementCounter", params_set2, signature_set2);
        Object[] params2 = { "MBeanAppAbstractTests" };
        String[] signature2 = { "java.lang.String" };
        Object result2 = mbsc.invoke(on1, "getCounter", params1, signature1);

        System.out.println("This is the result of the JMX invoked MBean operation for Get Counter: " + result2.toString());
        assertEquals(Integer.valueOf(1), result2);
    }

    @Before
    public void setDefaultPort() {
        server.setHttpDefaultPort(DEFAULT_HTTP_PORT);
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_MBEAN;
    }

    @Override
    public boolean useDefaultVirtualHost() {
        return true;
    }

}
