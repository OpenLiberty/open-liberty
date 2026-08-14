/*******************************************************************************
 * Copyright (c) 2023, 2026 IBM Corporation and others.
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

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
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

            // DNS lookup failures occur when trying the lookup and that is permissible 
            // (varies from platform to platform and DNS configuration). Specifically, across multiple test engines.
            //  Strict upstream network DNS servers often reject this short format with a 'DNS format error [response code 1]'.

            // The purpose of this test is to attempt the DNS connection to ensure the illegalAccessException FFDC is not created, not to ensure the DNS resolution was successful
            Attributes attrs = dirContext.getAttributes("dns:/" + InetAddress.getLocalHost().getHostName());
        } catch (NamingException ex) { 
            // Catching NamingException handles CommunicationException, NameNotFoundException, 
            // and network-specific DNS format errors.
        }
    }
}
