/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.factory;

import static com.ibm.ws.ejbcontainer.injection.factory.MBLinkInfoRefAddr.ADDR_TYPE;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.container.HomeOfHomes;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionUtil;

/**
 * MB-Link Resolver Factory for auto-link resolution. <p>
 * 
 * This class is used as an object factory that returns a ManagedBean
 * instance. <p>
 * 
 * This factory is used when injection occurs or when a component performs
 * a lookup in the java:comp name space for managed bean references with no
 * binding override.
 */
public class MBLinkObjectFactory implements ObjectFactory
{
    private static final String CLASS_NAME = MBLinkObjectFactory.class.getName();

    private static final TraceComponent tc = Tr.register(MBLinkObjectFactory.class, "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    /**
     * Default constructor for an MBLinkObjectFactory.
     */
    public MBLinkObjectFactory()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init>");
    }

    /**
     * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object, javax.naming.Name, javax.naming.Context, java.util.Hashtable)
     */
    public Object getObjectInstance(Object obj,
                                    Name name,
                                    Context nameCtx,
                                    Hashtable<?, ?> environment)
                    throws Exception
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getObjectInstance : " + obj);

        Object retObj = null;

        // -----------------------------------------------------------------------
        // Is obj a Reference?
        // -----------------------------------------------------------------------
        if (!(obj instanceof Reference))
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : null (non-Reference)");
            return null; //d744997
        }

        Reference ref = (Reference) obj;

        // -----------------------------------------------------------------------
        // Is the right factory for this reference?
        // -----------------------------------------------------------------------
        if (!ref.getFactoryClassName().equals(CLASS_NAME))
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : null (wrong factory class: " + ref.getFactoryClassName() + " )");
            return null; //d744997
        }

        // -----------------------------------------------------------------------
        // Is address null?
        // -----------------------------------------------------------------------
        RefAddr addr = ref.get(ADDR_TYPE);
        if (addr == null)
        {
            NamingException nex = new NamingException("The address for this Reference is empty (null)");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : " + nex);
            throw nex;
        }

        // Reference has the right factory and non empty address,
        // so it is OK to generate the object now
        MBLinkInfo info = (MBLinkInfo) addr.getContent();

        // -----------------------------------------------------------------------
        // First - find the bean home (EJSHome) based on what is known
        // -----------------------------------------------------------------------
        // Has the home been resolved already, and cached in the binding info?
        EJSHome home = info.ivHome;

        String beanType = info.ivBeanType;

        // If the home has not been resolved previously, then the home must
        // be located via the auto-link information
        if (home == null)
        {
            HomeOfHomes homeOfHomes = EJSContainer.homeOfHomes;

            // Look for the home based on the referenced managed bean type
            if (beanType != null)
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "auto-link on " + beanType);

                // A specific bean was not identified in config, so attempt
                // 'auto-link', where a bean home is located based on
                // whether the bean implements the type being injected.                                      d429866.1
                home = homeOfHomes.getHomeByInterface(info.ivApplication,
                                                      info.ivModule,
                                                      beanType);
            }

            // If the home could not be found, or the home is for an EJB rather
            // than a managed bean, then report the error and fail. Currently
            // EJBs and ManagedBeans are intermixed in the HomeOfHomes, so it
            // is also possible an AmbiguousEJBReferenceException occurs when
            // it should not.
            if (home == null || !home.isManagedBeanHome())
            {
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "ManagedBean reference could not be resolved : " + home);
                String cause = (home == null) ? "Managed bean of type " + beanType +
                                                " could not be located in the application."
                                : "The " + beanType + " reference type is an EJB;" +
                                  " an EJB reference must be used.";
                throw new InjectionException("ManagedBean reference " + info.ivRefName +
                                             "  could not be resolved. " + cause);
            }

            info.ivHome = home;
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "home = " + home.getJ2EEName());

        // -----------------------------------------------------------------------
        // Second - create the return object
        // -----------------------------------------------------------------------

        BeanMetaData bmd = home.getBeanMetaData();

        try
        {
            // For ManagedBeans, use createBusinessObject on the home since it will
            // create a new instance, performing PostConstruct and injection.
            retObj = home.createBusinessObject(bmd.enterpriseBeanClassName, false);
        } catch (Throwable ex)
        {
            // CreateException must be caught here... but basically all failures
            // that may occur while creating the bean instance must cause the
            // injection to fail.
            FFDCFilter.processException(ex, CLASS_NAME + ".getObjectInstance",
                                        "257", this);
            String msg = "The " + info.ivRefName + " managed bean reference in the " +
                         info.ivModule + " module of the " + info.ivApplication + " application" +
                         " could not be resolved. A failure occurred creating an instance of the " +
                         bmd.j2eeName.getComponent() + " managed bean in the " + bmd.j2eeName.getModule() +
                         " module of the " + bmd.j2eeName.getApplication() + " application.";

            InjectionException inex = InjectionUtil.checkForRecursiveException(ex, msg);

            if (isTraceOn && tc.isDebugEnabled())
                Tr.exit(tc, "getObjectInstance: " + inex);

            throw inex;
        }

        // Careful not to call toString or it may invoke the customer bean!
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getObjectInstance : " + retObj.getClass().getName());

        return retObj;
    }
}
