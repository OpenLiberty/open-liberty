/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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

import javax.ejb.EJBContext;
import javax.ejb.TimerService;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;
import com.ibm.wsspi.injectionengine.factory.InjectionObjectFactory;

/**
 * TimerService Resolver Factory for injection and lookup. <p>
 * 
 * This class is used as an object factory that returns the TimerService
 * for the currently active EJB. An InjectionException occurs whe the
 * factory is invoked outside the scope of an EJB. <p>
 * 
 * This factory is used when injection occurs or when a component performs
 * a lookup in the java:comp name space for TimerService subclasses. <p>
 * 
 * Nearly identical to the EJBContextObjectFactory, except the TimerService
 * is extracted from the EJBContext. <p>
 */
public class TimerServiceObjectFactory implements InjectionObjectFactory
{
    private static final TraceComponent tc = Tr.register
                    (TimerServiceObjectFactory.class,
                     "EJBContainer", "com.ibm.ejs.container.container");

    /**
     * Required default no argument constructor.
     */
    public TimerServiceObjectFactory()
    {
        // Default Constructor
    }

    /**
     * (non-Javadoc)
     * 
     * @see ObjectFactory#getObjectInstance
     */
    public Object getObjectInstance(Object obj,
                                    Name name,
                                    Context nameCtx,
                                    Hashtable<?, ?> environment)
                    throws Exception
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getObjectInstance : " + obj);

        // F743-17630CodRv
        // This factory should only be used from inside an ejb.
        //
        // Checking for the lack of EJBContext is good enough to tell us
        // we are not inside an ejb when the code flow was started by some
        // non-ejb component (such as a servlet).  However, in the case where
        // we've got an ejb that calls into a webservice endpoint, then there
        // may be a ContainerTx that contains a BeanO, even though we are not
        // allowed to use the factory from inside the webservice endpoint.
        //
        // To account for this, we must also check the ComponentMetaData (CMD) and
        // confirm that it is ejb specific.  In the webservice endpoint case,
        // the CMD will have been switched to some non-ejb CMD, and so this check
        // so stop us.
        EJBContext ejbContext = EJSContainer.getCallbackBeanO(); // d630940
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (ejbContext == null ||
            !(cmd instanceof BeanMetaData))
        {
            InjectionException iex = new InjectionException
                            ("The TimerService type may only be injected into an EJB instance " +
                             "or looked up within the context of an EJB.");
            Tr.error(tc, "EJB_CONTEXT_DATA_NOT_AVAILABLE_CNTR0329E",
                     new Object[] { TimerService.class.getName() });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : ", iex);
            throw iex;
        }

        TimerService timerService = ejbContext.getTimerService();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getObjectInstance : " + timerService);

        return timerService;
    }

    @Override
    public Object getInjectionObjectInstance(Reference ref,
                                             Object targetInstance,
                                             InjectionTargetContext targetContext)
                    throws Exception
    {
        if (targetContext != null)
        {
            TimerService timerService = targetContext.getInjectionTargetContextData(TimerService.class);
            if (timerService != null)
            {
                return timerService;
            }
        }

        return getObjectInstance(ref, null, null, null);
    }
}
