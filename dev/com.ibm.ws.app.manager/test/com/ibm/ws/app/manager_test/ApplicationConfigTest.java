/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager_test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.util.Hashtable;

import org.junit.Test;
import org.osgi.framework.Constants;

import com.ibm.websphere.application.ApplicationMBean;
import com.ibm.ws.app.manager.internal.AppManagerConstants;
import com.ibm.ws.app.manager.internal.ApplicationConfig;

public class ApplicationConfigTest {
    @Test
    public void testDerivedFromLocation() {
        String pid = "com.ibm.ws.app.manager.some.unique.id";
        Hashtable<String, Object> ca = new Hashtable<String, Object>();
        ca.put(AppManagerConstants.LOCATION, "snoop.war");
        ca.put(Constants.SERVICE_PID, pid);

        ApplicationConfig config = new ApplicationConfig(pid, ca);

        assertConfig(config, "snoop", "snoop.war", "war", true);
    }

    @Test
    public void testAllSpecified() {
        String pid = "com.ibm.ws.app.manager.some.unique.id";
        Hashtable<String, Object> ca = new Hashtable<String, Object>();
        ca.put(AppManagerConstants.LOCATION, "snoop.war");
        ca.put(AppManagerConstants.NAME, "my name");
        ca.put(AppManagerConstants.TYPE, "ear");
        ca.put(Constants.SERVICE_PID, pid);
        ca.put(AppManagerConstants.AUTO_START, false);

        ApplicationConfig config = new ApplicationConfig(pid, ca);

        assertConfig(config, "my name", "snoop.war", "ear", false);
    }

    public void testInvalidConfig() {
        String pid = "com.ibm.ws.app.manager.some.unique.id";
        ApplicationConfig config = new ApplicationConfig(pid, new Hashtable<String, Object>());

        assertFalse("The config is valid when it should be invalid", config.isValid());
    }

    @Test
    public void testValidConfig() {
        String pid = "com.ibm.ws.app.manager.some.unique.id";
        Hashtable<String, Object> ca = new Hashtable<String, Object>();
        ca.put(AppManagerConstants.LOCATION, "snoop.war");
        ca.put(Constants.SERVICE_PID, pid);

        ApplicationConfig config = new ApplicationConfig(pid, ca);

        assertTrue("Config is not valid and it should be", config.isValid());
        assertConfig(config, "snoop", "snoop.war", "war", true);
    }

    /**
     * @param config
     */
    private void assertConfig(ApplicationConfig config, String name, String loc, String type, boolean autoStart) {
        assertEquals("The application name wasn't right", name, config.getName());
        assertEquals("The location wasn't right", loc, config.getLocation());
        assertEquals("The type wasn't right", type, config.getType());
        assertEquals("The auto start wasn't set right", autoStart, config.isAutoStarted());
        assertEquals("The MBean name wasn't right", "WebSphere:service=" + ApplicationMBean.class.getName() + ",name=" + name, config.getMBeanName());
    }
}
