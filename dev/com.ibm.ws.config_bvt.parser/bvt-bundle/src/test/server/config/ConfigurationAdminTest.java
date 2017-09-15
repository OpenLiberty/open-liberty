/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config;

import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.Assert;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 *
 */
public class ConfigurationAdminTest extends Test {
    private final ConfigurationAdmin configAdmin;

    public ConfigurationAdminTest(String name, ConfigurationAdmin ca) {
        super(name, 0);
        this.configAdmin = ca;
    }

    public void testConfigurationAdmin() throws Exception {
        String myPID = "my.test.pid";
        Configuration cfg = configAdmin.createFactoryConfiguration(myPID);
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("a", "a");
//        props.put("A", "a");
        cfg.update(props);

        Configuration caCfg = configAdmin.getConfiguration(cfg.getPid());
        Dictionary<String, Object> caProps = caCfg.getProperties();

        // check # of elements
        Assert.assertNotNull("missing key a", caProps.remove("a"));
        Object o = caProps.remove("A");
        Assert.assertNull("has key A: " + o, o);

        // update dictionary make sure its copied
        props.put("b", "b");
        Assert.assertNull("b is present", caProps.get("b"));

        caProps.put("c", "c");
        Dictionary<String, Object> caProps2 = caCfg.getProperties();
        Assert.assertNull("c is present", caProps2.get("c"));

        caCfg.update(caProps);
        Assert.assertNull("c is present", caProps2.get("c"));

        caProps2 = caCfg.getProperties();
        Assert.assertNotNull("c is not present", caProps2.get("C"));
    }

    @Override
    public String[] getServiceClasses() {
        return null;
    }

    @Override
    public Throwable getException() {
        try {
            testConfigurationAdmin();
        } catch (Throwable t) {
            exception = t;
        }

        return exception;
    }
}
