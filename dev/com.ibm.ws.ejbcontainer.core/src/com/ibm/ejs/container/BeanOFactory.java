/*******************************************************************************
 * Copyright (c) 1998, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;


/**
 * A <code>BeanOFactory</code> creates BeanO instances. <p>
 * 
 * The <code>BeanOFactory</code> is used by <code>EJSHomes</code> to
 * create <code>BeanOs</code> as needed. It allows the homes to be written
 * generically, in terms of BeanOs, and be parameterized with a factory
 * to generate the more specific type of <code>BeanO</code> needed for the
 * type of beans managed by the home (stateless session, container managed
 * entity, etc...) <p>
 * 
 */
public abstract class BeanOFactory
{
    private static final TraceComponent tc = Tr.register(BeanOFactory.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    public enum BeanOFactoryType
    {
        CM_STATELESS_BEANO_FACTORY("Container Managed Stateless"),
        BM_STATELESS_BEANO_FACTORY("Bean Managed Stateless"),
        CM_STATEFUL_BEANO_FACTORY("Container Managed Stateful"),
        BM_STATEFUL_BEANO_FACTORY("Bean Managed Stateful"),
        CM_MESSAGEDRIVEN_BEANO_FACTORY("Container Managed Message-Driven"),
        BM_MESSAGEDRIVEN_BEANO_FACTORY("Bean Managed Message-Driven"),
        CONTAINER_MANAGED_BEANO_FACTORY("Container Managed 1.x Entity"),
        CONTAINER_MANAGED_2_0_BEANO_FACTORY("Container Managed 2.x Entity"),
        BEAN_MANAGED_BEANO_FACTORY("Bean Managed Entity"),
        SINGLETON_BEANO_FACTORY("Singleton"),
        MANAGED_BEANO_FACTORY("Managed Bean");

        private final String ivName;

        private BeanOFactoryType(String name)
        {
            ivName = name;
        }

        @Override
        public String toString()
        {
            return ivName;
        }
    }

    /**
     * Create a new <code>BeanO</code> instance. <p>
     * 
     * @param c the <code>EJSContainer</code> the newly created
     *            <code>BeanO</code> lives in <p>
     * 
     * @param b the bean instance associated with the
     *            newly created <code>BeanO</code> <p>
     * 
     * @param h the <code>EJSHome</code> associated with the newly
     *            created <code>BeanO</code> <p>
     */
    protected abstract BeanO newInstance(EJSContainer c, EJSHome h); // d623673.1

    /**
     * Create and initialize a new <code>BeanO</code> instance. <p>
     * 
     * @param c the <code>EJSContainer</code> the newly created
     *            <code>BeanO</code> lives in <p>
     * 
     * @param h the <code>EJSHome</code> associated with the newly
     *            created <code>BeanO</code> <p>
     * 
     * @param reactivate true if a passivated stateful session bean is being reactivated
     */
    // Changed EnterpriseBean to Object.                             d366807.1
    public final BeanO create(EJSContainer c, EJSHome h, boolean reactivate) // d623673.1
    throws RemoteException, InvocationTargetException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "create: " + h);

        BeanO beanO = newInstance(c, h);
        boolean success = false;

        try
        {
            beanO.initialize(reactivate);
            success = true;
        } finally
        {
            if (!success)
            {
                beanO.discard();
            }

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "create: " + beanO + ", success=" + success);
        }

        return beanO;
    }
}
