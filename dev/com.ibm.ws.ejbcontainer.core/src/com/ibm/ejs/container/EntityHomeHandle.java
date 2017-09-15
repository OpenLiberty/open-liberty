/*******************************************************************************
 * Copyright (c) 1998, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.Properties;

import javax.ejb.EJBHome;
import javax.ejb.HomeHandle;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.rmi.PortableRemoteObject;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

//
//     ***** Obsolete ****
//     This class was replaced by com.ibm.ejb.portable.EJBMetaDataImpl,
//     so do not update this class, update the new class.
//     ***** Obsolete ****
//

/**
 * An EntityHomeHandle provides a concrete implementation of a HomeHandle
 * for EJBHomes living in an EJS server. <p>
 */

public class EntityHomeHandle implements HomeHandle, Serializable
{
    //d121558
    private static final TraceComponent tc = Tr.register(EntityHomeHandle.class, "EJBContainer", "com.ibm.ejs.container.container"); //p115726

    //private static final String CLASS_NAME = "com.ibm.ejs.container.EntityHomeHandle";

    // p115726 - remove final from instance variables since readObject method
    // needs to set the instance variables.

    String homeJNDIName; // JNDI name of the bean's home interface
    String homeInterface; // The name of class for the home interface
    transient EJBHome home; // The EJBHome to which this handle points
    transient ClassLoader classLoader; // Container's class loader
    final J2EEName j2eeName;
    final Properties initialContextProperties; // Initial context properties of the bean

    private static final long serialVersionUID = -9080113035042415332L;

    /**
     * Create a new <code>EntityHomeHandle</code> instance for the given
     * <code>BeanId</code>. <p>
     */

    EntityHomeHandle(BeanId id, String homeInterface, BeanMetaData bmd/* 91851 */, Properties props)
    //d145385 sig changes
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "EntityHomeHandle", new Object[] { id, homeInterface });

        this.homeJNDIName = id.getJNDIName();//89554
        this.homeInterface = homeInterface;

        this.j2eeName = id.getJ2EEName();
        initialContextProperties = props;//d145386

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "EntityHomeHandle");
    } // EntityHandle

    /**
     * Return <code>EJBHome</code> reference for this HomeHandle. <p>
     */

    public EJBHome getEJBHome() throws RemoteException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getEJBHome");

        if (home == null)
        {
            try
            {
                Class homeClass = null;
                try
                {
                    //
                    // If we are running on the server side, then the thread
                    // context loader would have been set appropriately by
                    // the container. If running on a client, then check the
                    // thread context class loader first
                    //
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    if (cl != null)
                    {
                        homeClass = cl.loadClass(homeInterface);
                    }
                    else
                    {
                        throw new ClassNotFoundException();
                    }
                } catch (ClassNotFoundException ex)
                {
                    //FFDCFilter.processException(ex, CLASS_NAME + ".getEJBHome", "141", this);
                    try
                    {
                        homeClass = Class.forName(homeInterface);
                    } catch (ClassNotFoundException e)
                    {
                        //FFDCFilter.processException(e, CLASS_NAME + ".getEJBHome",
                        //                       "148", this);
                        throw new ClassNotFoundException(homeInterface);
                    }
                }

                InitialContext ctx = null;
                try
                {
                    // Locate the home
                    //91851 begin
                    if (this.initialContextProperties == null)
                    {
                        ctx = new InitialContext();
                    }
                    else
                    {
                        try
                        {
                            ctx = new InitialContext(this.initialContextProperties);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) // d144064
                                Tr.debug(tc, "Created an initial context with the " +
                                             "initialContextProperties, providerURL = " +
                                             (String) initialContextProperties.get("java.naming.provider.url") +
                                             " INITIAL_CONTEXT_FACTORY = " +
                                             (String) initialContextProperties.get(Context.INITIAL_CONTEXT_FACTORY));
                        } catch (NamingException ne)
                        {
                            //FFDCFilter.processException(ne, CLASS_NAME + ".getEJBHome",
                            //                          "177", this);
                            ctx = new InitialContext();
                        }
                    }
                    //91851 end
                    home = (EJBHome) PortableRemoteObject.
                                    narrow(ctx.lookup(homeJNDIName), homeClass);
                } catch (NoInitialContextException e)
                {
                    //FFDCFilter.processException(e, CLASS_NAME + ".getEJBHome", "188", this);
                    java.util.Properties p = new java.util.Properties();
                    p.put(Context.INITIAL_CONTEXT_FACTORY,
                          "com.ibm.websphere.naming.WsnInitialContextFactory");
                    ctx = new InitialContext(p);
                    home = (EJBHome) PortableRemoteObject.
                                    narrow(ctx.lookup(homeJNDIName), homeClass);
                }
            } catch (NamingException e)
            {
                // Problem looking up the home

                //FFDCFilter.processException(e, CLASS_NAME + ".getEJBHome", "201", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "getEJBHome", e);

                RemoteException re = new NoSuchObjectException("Could not find home in JNDI");
                re.detail = e;
                throw re;
            } catch (ClassNotFoundException e)
            {
                // We couldn't find the home interface's class

                //FFDCFilter.processException(e, CLASS_NAME + ".getEJBHome", "213", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "getEJBHome", e);
                throw new RemoteException("Could not load home interface", e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getEJBHome");

        return home;
    } // getEJBHome

    /*
     * Finds the findByPrimaryKey method in the bean's home interface
     */

    private Method findFindByPrimaryKey(Class c)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "findFindByPrimaryKey", c);

        Method[] methods = c.getMethods();

        for (int i = 0; i < methods.length; ++i)
        {
            if (methods[i].getName().equals("findByPrimaryKey"))
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "findFindByPrimaryKey");
                return methods[i];
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "findFindByPrimaryKey: method findByPrimaryKey not found!");
        return null;
    }

} // EntityHomeHandle

