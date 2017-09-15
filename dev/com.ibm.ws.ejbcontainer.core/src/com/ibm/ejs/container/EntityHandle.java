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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.Properties;

import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.rmi.PortableRemoteObject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

//
//     ***** Obsolete ****
//     This class was replaced by com.ibm.ejb.portable.EJBMetaDataImpl,
//     so do not update this class, update the new class.
//     ***** Obsolete ****
//

/**
 * An EJSHandle provides a concrete implementation of an EJB Handle
 * for EJBObjects living in an EJS server. <p>
 */
public class EntityHandle implements Handle, Serializable
{

    private static final TraceComponent tc = Tr.register(EntityHandle.class, "EJBContainer", "com.ibm.ejs.container.container");//d121558

    //private static final String CLASS_NAME = "com.ibm.ejs.container.EntityHandle";

    // p115726 - remove final from instance variables since readObject method
    // needs to set the instance variables.

    String homeJNDIName; // JNDI name of the bean's home interface
    String homeInterface; // The name of class for the home interface
    Serializable key; // The primary key of the bean
    final Properties initialContextProperties; // Initial context properties of the bean

    transient EJBObject object; // The EJBObject to which this handle points
    transient ClassLoader classLoader;

    private static final long serialVersionUID = 2909502407438195167L;

    /**
     * Create a new <code>EntityHandle</code> instance for the given
     * <code>BeanId</code>. <p>
     */

    EntityHandle(BeanId id, BeanMetaData bmd, Properties props)//d145386
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "<init>", id);

        homeJNDIName = id.getJNDIName();//89554
        key = id.getPrimaryKey();
        homeInterface = bmd.homeInterfaceClass.getName();
        initialContextProperties = props;// d145386

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "<init>");
    } // EntityHandle

    /**
     * Return <code>EJBObject</code> reference for this handle. <p>
     */

    public EJBObject getEJBObject() throws RemoteException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getEJBObject");

        if (object == null)
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
                    ClassLoader cl =
                                    Thread.currentThread().getContextClassLoader();
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

                    //               FFDCFilter.processException(ex, CLASS_NAME + ".getEJBObject",
                    //                                           "147", this);
                    try
                    {
                        homeClass = Class.forName(homeInterface);
                    } catch (ClassNotFoundException e)
                    {
                        //                  FFDCFilter.processException(e, CLASS_NAME + ".getEJBObject",
                        //"155", this);
                        throw new ClassNotFoundException(homeInterface);
                    }

                }

                InitialContext ctx = null;
                EJBHome home = null;
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
                            //                     FFDCFilter.processException(ne, CLASS_NAME + ".getEJBObject",
                            //"186", this);
                            ctx = new InitialContext();
                        }
                    }
                    //91851 end
                    home = (EJBHome) PortableRemoteObject.
                                    narrow(ctx.lookup(homeJNDIName), homeClass);
                } catch (NoInitialContextException e)
                {
                    //               FFDCFilter.processException(e, CLASS_NAME + ".getEJBObject", "197", this);
                    java.util.Properties p = new java.util.Properties();
                    p.put(Context.INITIAL_CONTEXT_FACTORY,
                          "com.ibm.websphere.naming.WsnInitialContextFactory");
                    ctx = new InitialContext(p);
                    home = (EJBHome) PortableRemoteObject.
                                    narrow(ctx.lookup(homeJNDIName), homeClass);
                }

                // Introspect to find and invoke the findByPrimaryKey method
                Method fbpk = findFindByPrimaryKey(homeClass);
                object = (EJBObject) fbpk.invoke(home, new Object[] { key });

            } catch (InvocationTargetException e)
            {

                // Unwrap the real exception and pass it back

                //            FFDCFilter.processException(e, CLASS_NAME + ".getEJBObject", "216", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "getEJBObject", e);

                Throwable t = e.getTargetException();
                if (t instanceof RemoteException)
                {
                    throw (RemoteException) t;
                }
                else
                {
                    throw new RemoteException("Could not find bean", t);
                }

            } catch (NamingException e)
            {

                // Problem looking up the home

                //            FFDCFilter.processException(e, CLASS_NAME + ".getEJBObject", "236", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "getEJBObject", e);

                RemoteException re =
                                new NoSuchObjectException("Could not find home in JNDI");
                re.detail = e;

                throw re;

            } catch (ClassNotFoundException e)
            {

                // We couldn't find the home interface's class

                //            FFDCFilter.processException(e, CLASS_NAME + ".getEJBObject", "252", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "getEJBObject", e);
                throw new RemoteException("Could not load home interface", e);

            } catch (IllegalAccessException e)
            {

                // This shouldn't happen
                //            FFDCFilter.processException(e, CLASS_NAME + ".getEJBObject", "262", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "getEJBObject", e);
                throw new RemoteException("Bad home interface definition", e);

            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getEJBObject");

        return object;
    } // getEJBObject

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

} // EntityHandle

