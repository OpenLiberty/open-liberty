/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.web;

import java.net.InetAddress;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/IllegalAccessTestServlet")
public class IllegalAccessTestServlet extends FATServlet {

    @Test
    // This test passes if the illegalAccessException FFDC is not generated
    public void testJdkNamingDnsDoesNotExportComSunJndiUurlDns() throws Exception {
        try {
            Properties env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            env.put(Context.PROVIDER_URL, "dns:");
            InitialDirContext dirContext = new InitialDirContext(env);

            // This will cause an illegalAccessException FFDC if the following entry is not in the java9.options file:
            // --add-exports
            // jdk.naming.dns/com.sun.jndi.url.dns=ALL-UNNAMED
            // 
            // Attempt a DNS lookup of the local host name using the underlying machine's DNS config
            Attributes attrs = dirContext.getAttributes("dns:/" + InetAddress.getLocalHost().getHostName());
        } catch (NameNotFoundException nnfe) { // dirContext.getAttributes may throw a NameNotFoundException, it can be ignored
            // Ignore
        }
    }
}
