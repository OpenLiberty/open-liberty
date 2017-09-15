/*******************************************************************************
 * Copyright (c) 2006, 2014 IBM Corporation and others.
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
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.UserTransactionWrapper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;
import com.ibm.wsspi.injectionengine.factory.InjectionObjectFactory;

/**
 * ObjectFactory that returns an appropriate UserTransaction depending on
 * whether the injected object (or lookup context) is an EJB.
 */
public class HybridUserTransactionObjectFactory implements InjectionObjectFactory
{
    private static final TraceComponent tc = Tr.register(HybridUserTransactionObjectFactory.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    private static volatile ObjectFactory svUserTranFactory;
    private static Name svUserTranName;

    @Override
    public Object getObjectInstance(Object obj,
                                    Name name,
                                    Context nameCtx,
                                    Hashtable<?, ?> environment)
                    throws Exception
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "getObjectInstance (jndi)");

        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        BeanMetaData bmd = cmd instanceof BeanMetaData ? (BeanMetaData) cmd : null;
        return getObjectInstance(obj, bmd);
    }

    @Override
    public Object getInjectionObjectInstance(Reference ref,
                                             Object targetInstance,
                                             InjectionTargetContext targetContext)
                    throws Exception
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "getObjectInstance (injection)");

        BeanMetaData bmd = null;
        if (targetContext != null)
        {
            BeanO beanO = targetContext.getInjectionTargetContextData(BeanO.class);
            bmd = beanO == null ? null : beanO.getHome().getBeanMetaData();
        }

        return getObjectInstance(ref, bmd);
    }

    private Object getObjectInstance(Object obj, BeanMetaData bmd)
                    throws Exception
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getObjectInstance", new Object[] { obj, bmd });

        // EJB and non-EJB require different UserTransaction factories.
        //
        // Additionally, if we are dealing with an ejb, we ensure the
        // ejb is one that actually allows a UserTransaction (meaning, it uses
        // bean managed transactions).
        Object returnValue;
        if (bmd != null)
        {
            if (bmd.usesBeanManagedTx)
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "BMT EJB");
                returnValue = UserTransactionWrapper.INSTANCE; // d631349
            }
            else
            {
                InjectionException iex = new InjectionException
                                ("UserTransaction may only be looked up by or injected into an EJB " +
                                 "if that EJB is configured to use bean managed transactions.  " +
                                 "EJB " + bmd.j2eeName + " is configured for container managed transactions.");

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "getObjectInstance", iex);
                throw iex;
            }
        }
        else
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "non-EJB");

            ObjectFactory utxFactory;
            Name utxName;

            synchronized (this)
            {
                utxFactory = svUserTranFactory;
                utxName = svUserTranName;

                if (utxFactory == null)
                {
                    utxName = new InitialContext().getNameParser("java:").parse("java:comp/UserTransaction");
                    utxFactory = (ObjectFactory) Class.forName("com.ibm.ws.Transaction.JTA.UtxJNDIFactory").newInstance();

                    svUserTranName = utxName;
                    svUserTranFactory = utxFactory;
                }
            }

            // F743-17630.1
            returnValue = utxFactory.getObjectInstance(obj, utxName, null, null);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getObjectInstance", returnValue);
        return returnValue;
    }
}
