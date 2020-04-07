/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.test.jca;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.resource.cci.ConnectionFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class JcaTestBeanJNDI {

    private BundleContext bpcontext;

    public void setBpContext(BundleContext bundleContext) {
        bpcontext = bundleContext;
        System.out.println("Injected the context:" + bpcontext);
    }

    public void init() throws NamingException, InterruptedException {

        try {
            javax.resource.cci.ConnectionFactory cf = (javax.resource.cci.ConnectionFactory) new InitialContext()
                            .lookup("ims/cf1");
            System.out.println("Test Failed for ims/cf1 " + cf);
        } catch (NamingException ne) {
            if (ne.getMessage().contains("CWWKN0008E:")) {
                System.out.println("Lookup failed for ims/cf1 " + ne);
            } else {
                System.out.println("Test Failed for ims/cf1 " + ne);
            }
        }

        try {
            Object obj = new InitialContext().lookup("jms/queue1");
            System.out.println("Test Failed for jms/queue1 " + obj);
        } catch (NamingException ne) {
            if (ne.getMessage().contains("CWWKN0008E:")) {
                System.out.println("Lookup failed for jms/queue1 " + ne);
            } else {
                System.out.println("Test Failed for jms/queue1 " + ne);
            }
        }

        try {
            Object obj = new InitialContext().lookup("jms/topic1");
            System.out.println("Test Failed for jms/topic1 " + obj);
        } catch (NamingException ne) {
            if (ne.getMessage().contains("CWWKN0008E:")) {
                System.out.println("Lookup failed for jms/topic1 " + ne);
            } else {
                System.out.println("Test Failed for jms/topic1 " + ne);
            }
        }

        Context ctx = new InitialContext();

        try {
            Object r = ctx
                            .lookup("osgi:service/javax.resource.cci.ConnectionFactory/(osgi.jndi.service.name=ims/cf1)");
        } catch (NameNotFoundException nnfe) {
            System.out.println("Lookup failed for ims/cf1 " + nnfe);
        }

        try {
            ServiceReference<ConnectionFactory> ref = bpcontext.getServiceReference(ConnectionFactory.class);
            ConnectionFactory cf = bpcontext.getService(ref);
            if (cf != null) {
                System.out.println("Test Failed for javax.resource.cci.ConnectionFactory:" + cf);
            } else {
                System.out.println("Lookup failed for javax.resource.cci.ConnectionFactory");
            }
        } catch (Exception ex) {
            System.out.println("Lookup failed for javax.resource.cci.ConnectionFactory " + ex);
        }

    }
}
