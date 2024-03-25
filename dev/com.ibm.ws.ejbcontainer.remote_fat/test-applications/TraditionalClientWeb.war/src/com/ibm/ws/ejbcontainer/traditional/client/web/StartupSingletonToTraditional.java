/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.traditional.client.web;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.CreateException;
import javax.ejb.RemoveException;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.management.j2ee.Management;
import javax.management.j2ee.ManagementHome;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

/**
 * Demonstrates various JNDI names used to lookup EJBs on WebSphere traditional.
 */
@Singleton
@Startup
public class StartupSingletonToTraditional {

    private static final Logger logger = Logger.getLogger(StartupSingletonToTraditional.class.getName());

    // ------------------------------------------------------------------------
    // Set the following fields to WebSphere traditional server
    // ------------------------------------------------------------------------

    // WebSphere traditional IIOP port : bootstrap.properties - traditionalIIOPPort, default 2809
    private static final String IIOPPort = System.getProperty("traditionalIIOPPort", "2809");

    // WebSphere traditional host name : bootstrap.properties - traditionalHostName, default <localhost>
    private static final String remoteHostName = getRemoteHostName();

    // WebSphere traditional node name : bootstrap.properties - traditionalNodeName, default <localhost>Node01
    private static final String remoteNodeName = System.getProperty("traditionalNodeName", remoteHostName + "Node01");

    // WebSphere traditional server name : bootstrap.properties - traditionalServerName, default server1
    private static final String remoteServerName = System.getProperty("traditionalServerName", "server1");

    // ------------------------------------------------------------------------
    // Set the above fields to WebSphere traditional server
    // ------------------------------------------------------------------------

    private static final String CORBA_NAME = "corbaname::" + remoteHostName + ":" + IIOPPort;
    private static final String CORBA_NAME_NS = CORBA_NAME + "/NameService";
    private static final String CORBA_NAME_NSR = CORBA_NAME + "/NameServiceServerRoot";

    private static final String SERVER_CONTEXT_ROOT = "cell/nodes/" + remoteNodeName + "/servers/" + remoteServerName;

    private static final String SHORT_MEJB_HOME = "ejb/mgmt/MEJB";

    private static final String JAVA_GLOBAL_MEJB_HOME = "applications/ManagementEJB/mejb/Management!javax.management.j2ee.ManagementHome";
    private static final String JAVA_GLOBAL_MEJB_HOME_SHORT = "applications/ManagementEJB/mejb/Management";

    private static Context defaultContext;
    private static Context remoteContext;
    private static Context remoteRootContext;

    public static boolean AllLookupsPassed = false;
    public static List<Throwable> Failures = new ArrayList<Throwable>();

    private static String getRemoteHostName() {
        String remoteHost = System.getProperty("traditionalHost", "NA");
        if ("NA".equals(remoteHost)) {
            try {
                remoteHost = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException uhex) {
                Failures.add(uhex);
            }
        }
        return remoteHost;
    }

    /**
     * Provides the default InitialContext.
     *
     * @return an InitialContext with no properties
     * @throws NamingException if a NamingException is encountered
     */
    private static Context getDefaultContext() throws NamingException {
        if (defaultContext == null) {
            defaultContext = new InitialContext();
        }
        return defaultContext;
    }

    /**
     * Provides the Context of the remote system NameService
     *
     * @return the context of the remote system NameService
     * @throws NamingException if a NamingException is encountered
     */
    private static Context getRemoteContext() throws NamingException {
        if (remoteContext == null) {
            logger.info("creating remote context with : " + CORBA_NAME_NS);
            Context context = new InitialContext();
            remoteContext = (Context) context.lookup(CORBA_NAME_NS);
        }

        return remoteContext;
    }

    /**
     * Provides the Context of the remote system NameServiceServerRoot
     *
     * @return the context at the root of the remote system NameService
     * @throws NamingException if a NamingException is encountered
     */
    private static Context getRemoteRootContext() throws NamingException {
        if (remoteRootContext == null) {
            logger.info("creating remote root context with : " + CORBA_NAME_NSR);
            Context context = new InitialContext();
            remoteRootContext = (Context) context.lookup(CORBA_NAME_NSR);
        }

        return remoteRootContext;
    }

    private Object lookupDefaultContext(String jndiName) {
        try {
            Object home = getDefaultContext().lookup(jndiName);
            logger.info("DefaultContext : " + jndiName + " -> " + home);
            return home;
        } catch (Exception ex) {
            logger.info("DefaultContext : " + jndiName + " -> " + ex.toString());
            ex.printStackTrace(System.out);
            Failures.add(ex);
        }
        Failures.add(new NullPointerException("lookupDefaultContext : " + jndiName));
        return null;
    }

    private Object lookupRemoteContext(String jndiName) {
        try {
            Object home = getRemoteContext().lookup(jndiName);
            logger.info("RemoteContext : " + jndiName + " -> " + home);
            return home;
        } catch (Exception ex) {
            logger.info("RemoteContext : " + jndiName + " -> " + ex.toString());
            ex.printStackTrace(System.out);
            Failures.add(ex);
        }
        Failures.add(new NullPointerException("lookupRemoteContext : " + jndiName));
        return null;
    }

    private Object lookupRemoteRootContext(String jndiName) {
        try {
            Object home = getRemoteRootContext().lookup(jndiName);
            logger.info("RemoteRootContext : " + jndiName + " -> " + home);
            return home;
        } catch (Exception ex) {
            logger.info("RemoteRootContext : " + jndiName + " -> " + ex.toString());
            ex.printStackTrace(System.out);
            Failures.add(ex);
        }
        Failures.add(new NullPointerException("lookupRemoteRootContext : " + jndiName));
        return null;
    }

    @PostConstruct
    public void postConstruct() {
        String jndiName = null;

        try {
            // lookup the bean using default context with corbaname
            // corbaname::<host>:2809#cell/nodes/<host>Node01/servers/server1/ejb/mgmt/MEJB
            jndiName = CORBA_NAME + "#" + SERVER_CONTEXT_ROOT + "/" + SHORT_MEJB_HOME;
            testManagementBean(lookupDefaultContext(jndiName));

            // lookup the bean using default context with corbaname (including NameService)
            // corbaname::<host>:2809/NameService#cell/nodes/<host>Node01/servers/server1/ejb/mgmt/MEJB
            jndiName = CORBA_NAME_NS + "#" + SERVER_CONTEXT_ROOT + "/" + SHORT_MEJB_HOME;
            testManagementBean(lookupDefaultContext(jndiName));

            // lookup the bean using default context with corbaname (including NameServiceServerRoot)
            // corbaname::<host>:2809/NameServiceServerRoot#ejb/mgmt/MEJB
            jndiName = CORBA_NAME_NSR + "#" + SHORT_MEJB_HOME;
            testManagementBean(lookupDefaultContext(jndiName));

            // lookup the bean using the default remote context
            // cell/nodes/<host>Node01/servers/server1/ejb/mgmt/MEJB
            jndiName = SERVER_CONTEXT_ROOT + "/" + SHORT_MEJB_HOME;
            testManagementBean(lookupRemoteContext(jndiName));

            // lookup the bean using the default remote root context
            // ejb/mgmt/MEJB
            jndiName = SHORT_MEJB_HOME;
            testManagementBean(lookupRemoteRootContext(jndiName));

            // lookup the bean using default context with java:global equivalent name
            // corbaname::<host>:2809#applications/ManagementEJB/mejb/Management!javax.management.j2ee.ManagementHome
            jndiName = CORBA_NAME + "#" + JAVA_GLOBAL_MEJB_HOME;
            testManagementBean(lookupDefaultContext(jndiName));

            // lookup the bean using default context with java:global equivalent name without interface
            // corbaname::<host>:2809#applications/ManagementEJB/mejb/Management
            jndiName = CORBA_NAME + "#" + JAVA_GLOBAL_MEJB_HOME_SHORT;
            testManagementBean(lookupDefaultContext(jndiName));
        } catch (Throwable th) {
            th.printStackTrace(System.out);
            Failures.add(th);
        }
        if (Failures.isEmpty()) {
            AllLookupsPassed = true;
        }
    }

    private void testManagementBean(Object remoteObj) {
        if (remoteObj != null) {
            ManagementHome home = (ManagementHome) PortableRemoteObject.narrow(remoteObj, ManagementHome.class);
            logger.info("testManagementBean : " + home);
            try {
                Management bean = home.create();
                logger.info("testManagementBean : create() -> " + bean);
                bean.remove();
            } catch (RemoteException rex) {
                logger.info("testManagementBean : RemoteException : " + rex.getMessage());
                rex.printStackTrace(System.out);
                Failures.add(rex);
            } catch (CreateException cex) {
                logger.info("testManagementBean : CreateException : " + cex.getMessage());
                cex.printStackTrace(System.out);
                Failures.add(cex);
            } catch (RemoveException rem) {
                logger.info("testManagementBean : RemoveException : " + rem.getMessage());
                rem.printStackTrace();
                Failures.add(rem);
            }
        }
    }

    public Management getManagementEJB() throws CreateException {
        try {
            // lookup the bean using default context with corbaname (including NameServiceServerRoot)
            String jndiName = CORBA_NAME_NSR + "#" + SHORT_MEJB_HOME;
            Object remoteObj = lookupDefaultContext(jndiName);
            ManagementHome home = (ManagementHome) PortableRemoteObject.narrow(remoteObj, ManagementHome.class);
            Management bean = home.create();
            return bean;
        } catch (RemoteException rex) {
            throw new CreateException("Error creating Management EJB : " + rex);
        }
    }
}
