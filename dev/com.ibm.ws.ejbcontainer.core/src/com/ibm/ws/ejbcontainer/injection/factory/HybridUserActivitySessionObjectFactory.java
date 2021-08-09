/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.factory;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.injectionengine.InjectionException;

public class HybridUserActivitySessionObjectFactory implements ObjectFactory
{

    private static final TraceComponent tc = Tr.register(HybridUserActivitySessionObjectFactory.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    private static final ObjectFactory svNonEJBFactory;
    private static final ObjectFactory svEJBFactory;

    /**
     * The Class of the factory that is used to produce an ActivitySession.
     * 
     * Reflection is used to get an instance of this class. So, for performance
     * reasons, we cache the Class instance here to avoid multiple uses of
     * reflection.
     */
    static
    {
        // We want this to to be in a static initializer for performance and thread
        // safety reasons.  Unfortunately, this method throws an exception, so we
        // have no choice but to have a try/catch/re-throw logic here.  This is not
        // ideal, but it must be done if the code is to live in a static block.
        //
        // We are probably safe doing this, because it should not be a problem loading
        // this class.

        try
        {
            svNonEJBFactory = (ObjectFactory) Class.forName("com.ibm.ws.ActivitySession.UserActivitySessionWebFactory").newInstance();
            svEJBFactory = (ObjectFactory) Class.forName("com.ibm.ejs.container.UserActivitySessionWrapperFactory").newInstance();
        } catch (Throwable e)
        {
            throw new Error(e);
        }
    }

    public HybridUserActivitySessionObjectFactory()
    {

    }

    public Object getObjectInstance(Object obj,
                                    Name name,
                                    Context nameCtx,
                                    Hashtable<?, ?> environment)
                    throws Exception
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "getObjectInstance", new Object[] { obj, name });
        }

        // EJB and Web require different ActivitySession factories.
        //
        // If there is an EJB component metadata on the thread, then
        // we bring back the EJB specific flavor of the factory.  If there
        // is a non-EJB flavor of component metadata on the thread, then
        // we bring back the Web flavor of the factory.
        //
        // Additionally, if an EJB component is requesting the ActivitySession,
        // we only allow it if that EJB component is configured to use bean-managed
        // transaction (as opposed to container managed).
        // This restriction is needed to ensure that behavior for an EJB component
        // packaged in a war matches the behavior for an EJB component packaged in
        // a JAR.
        //
        // When the EJB component is packaged in a JAR, the ActivitySession factory
        // is only bound into the namespace if the bean uses bean managed
        // transactions (and the activity session service is enabled).  However, in the
        // ejb-in-war scenario, this HybridUserActivitySessionObjectFactory is always
        // bound into the namespace when the ActivitySession service is enabled 
        // (so that Web components can use ActivitySessions), and so if we didn't
        // have this check, it would be possible for an EJB packaged in a war to
        // "cheat" and obtain the ActivitySession factory, even though it wasn't
        // configured for bean managed transactions.
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();

        Object returnValue;
        if (cmd instanceof BeanMetaData)
        {
            BeanMetaData bmd = (BeanMetaData) cmd;

            // The 'usesBeanManagedAS' flag is only set to true when the activitySession
            // service is enabled AND the bean-component is using bean-managed transactions.
            // If the activitySession service is not enabled, or the bean-component uses
            // container-managed transactions, then this flag is false.
            if (bmd.usesBeanManagedAS)
            {
                if (isTraceOn && tc.isDebugEnabled())
                {
                    Tr.debug(tc,
                             "We have an EJB context on the thread, and it allows " +
                                             "UserTransactions and ActivitySessions.  Using an EJB " +
                                             "specific factory to obtain the ActivitySession instance.");
                }

                returnValue = svEJBFactory.getObjectInstance(obj, name, nameCtx, environment);
            }
            else
            {
                InjectionException iex = new InjectionException
                                ("ActivitySession may only be looked up by or injected into an EJB " +
                                 "if that EJB is configured to use bean managed transactions. " +
                                 "EJB " + bmd.j2eeName + " is configured for container managed transactions.");

                if (isTraceOn && tc.isEntryEnabled())
                {
                    Tr.exit(tc, "getObjectInstance : ", iex);
                }
                throw iex;
            }
        }
        else
        {
            if (isTraceOn && tc.isDebugEnabled())
            {
                Tr.debug(tc,
                         "We have a non-ejb context (or null content) on the thread. " +
                                         "Using Web factory to obtain ActivitySession instance.");
            }

            returnValue = svNonEJBFactory.getObjectInstance(obj, name, nameCtx, environment);

        }

        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "getObjectInstance", returnValue);
        }
        return returnValue;
    }
}
