/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.internal;

import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 *
 */
public class HostAliasTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule rule = outputMgr;

    @Rule
    public TestName name = new TestName();

    @After
    public void tearDown() {
        //outputMgr.copySystemStreams();
    }

    @Test
    public void testBadWildcardNoPort() {
        // Define a bad alias, which should get us a warning about the wildcard.. 
        String alias = "*bad";
        HostAlias ha = new HostAlias(alias, "vhost1");
        System.out.println(name.getMethodName() + ": " + alias + " --> " + ha);
        Assert.assertTrue("Should see a warning about bad use of wildcard for " + alias, outputMgr.checkForMessages("CWWKT0026W.*wildcard"));
        Assert.assertFalse("isValid should be false for ", ha.isValid);
    }

    @Test
    public void testBadWildcardWithPort() {
        String alias = "*bad:90";
        HostAlias ha = new HostAlias(alias, "vhost1");
        System.out.println(name.getMethodName() + ": " + alias + " --> " + ha);
        Assert.assertTrue("Should see a warning about bad use of wildcard for " + alias, outputMgr.checkForMessages("CWWKT0026W.*wildcard"));
        Assert.assertFalse("isValid should be false for ", ha.isValid);
    }

    @Test
    public void testWildcardWithBadPort() {
        String alias = "*:bad";
        HostAlias ha = new HostAlias(alias, "vhost1");
        System.out.println(name.getMethodName() + ": " + alias + " --> " + ha);
        Assert.assertTrue("Should see a warning about bad port for " + alias, outputMgr.checkForMessages("CWWKT0026W.*port"));
        Assert.assertFalse("isValid should be false for ", ha.isValid);
    }

    @Test
    public void testBadWildcardPort() {
        // Define a bad alias, which should get us a warning about the wildcard.. 
        String alias = "*:*";
        HostAlias ha = new HostAlias(alias, "vhost1");
        System.out.println(name.getMethodName() + ": " + alias + " --> " + ha);
        Assert.assertTrue("Should see a warning about bad port for " + alias, outputMgr.checkForMessages("CWWKT0026W.*port"));
        Assert.assertFalse("isValid should be false for ", ha.isValid);
    }

    @Test
    public void testBadPort() {
        String alias = "host:bad";
        HostAlias ha = new HostAlias(alias, "vhost1");
        System.out.println(name.getMethodName() + ": " + alias + " --> " + ha);
        Assert.assertTrue("Should see a warning about bad use of port for " + alias, outputMgr.checkForMessages("CWWKT0026W.*port"));
        Assert.assertFalse("isValid should be false for ", ha.isValid);
    }

    @Test
    public void testBadIPv4Addr() {
        String alias = "127.0.0:90";
        HostAlias ha = new HostAlias(alias, "vhost1");
        System.out.println(name.getMethodName() + ": " + alias + " --> " + ha);
        Assert.assertTrue("Should see a warning about bad host name for " + alias, outputMgr.checkForMessages("CWWKT0026W.*host"));
        Assert.assertFalse("isValid should be false for ", ha.isValid);
    }

    @Test
    public void testBadIPv6Addr() {
        String alias = "::f:90";
        HostAlias ha = new HostAlias(alias, "vhost1");
        System.out.println(name.getMethodName() + ": " + alias + " --> " + ha);
        Assert.assertTrue("Should see a warning about bad host name for " + alias, outputMgr.checkForMessages("CWWKT0026W.*host"));
        Assert.assertFalse("isValid should be false for ", ha.isValid);
    }

    @Test
    public void testWildcardWithPort() {
        String alias = "*:9443";
        HostAlias ha = new HostAlias(alias, "vhost1");
        System.out.println(name.getMethodName() + ": " + alias + " --> " + ha);
        Assert.assertEquals("Host should be set to * for " + alias, "*", ha.hostName);
        Assert.assertEquals("Port string should be 9443 for " + alias, "9443", ha.portString);
        Assert.assertTrue("isValid should be true for " + alias, ha.isValid);
    }

    @Test
    public void testPlainHost() {
        String alias = "plainHost";
        HostAlias ha = new HostAlias(alias, "vhost1");
        System.out.println(name.getMethodName() + ": " + alias + " --> " + ha);
        Assert.assertEquals("Host should be set to plainhost for " + alias, "plainhost", ha.hostName);
        Assert.assertEquals("Port string should be 80 for " + alias, "80", ha.portString);
        Assert.assertTrue("isValid should be true for " + alias, ha.isValid);
    }

    @Test
    public void testIPv4Host() {
        String alias = "127.0.0.1";
        HostAlias ha = new HostAlias(alias, "vhost1");
        System.out.println(name.getMethodName() + ": " + alias + " --> " + ha);
        Assert.assertEquals("Host should be set to 127.0.0.1 for " + alias, "127.0.0.1", ha.hostName);
        Assert.assertEquals("Port string should be 80 for " + alias, "80", ha.portString);
        Assert.assertTrue("isValid should be true for " + alias, ha.isValid);
    }

    @Test
    public void testIPv4HostPort() {
        String alias = "127.0.0.1:9080";
        HostAlias ha = new HostAlias(alias, "vhost1");
        System.out.println(name.getMethodName() + ": " + alias + " --> " + ha);
        Assert.assertEquals("Host should be set to 127.0.0.1 for " + alias, "127.0.0.1", ha.hostName);
        Assert.assertEquals("Port string should be 9080 for " + alias, "9080", ha.portString);
        Assert.assertTrue("isValid should be true for " + alias, ha.isValid);
    }

    @Test
    public void testIPv6Host() {
        String alias = "[::1]";
        HostAlias ha = new HostAlias(alias, "vhost1");
        System.out.println(name.getMethodName() + ": " + alias + " --> " + ha);
        Assert.assertEquals("Host should be set to [::1] for " + alias, "[::1]", ha.hostName);
        Assert.assertEquals("Port string should be 80 for " + alias, "80", ha.portString);
        Assert.assertTrue("isValid should be true for " + alias, ha.isValid);
    }

    @Test
    public void testIPv6HostPort() {
        String alias = "[::f]:90";
        HostAlias ha = new HostAlias(alias, "vhost1");
        System.out.println(name.getMethodName() + ": " + alias + " --> " + ha);
        Assert.assertEquals("Host should be set to [::f] for " + alias, "[::f]", ha.hostName);
        Assert.assertEquals("Port string should be 90 for " + alias, "90", ha.portString);
        Assert.assertTrue("isValid should be true for " + alias, ha.isValid);
    }

}
