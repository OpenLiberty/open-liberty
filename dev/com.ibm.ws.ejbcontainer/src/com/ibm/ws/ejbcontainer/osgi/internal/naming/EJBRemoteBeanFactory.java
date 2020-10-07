/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.naming;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.osgi.EJBHomeRuntime;
import com.ibm.ws.ejbcontainer.osgi.EJBRemoteRuntime;

/**
 * ObjectFactory class for Initializing a remote EJB looked up through legacy
 * bindings. Extends EJBNamingInstancer to initializeEJB.
 */
@Component(service = { ObjectFactory.class, EJBRemoteBeanFactory.class })
public class EJBRemoteBeanFactory extends EJBNamingInstancer implements ObjectFactory {
    private static final TraceComponent tc = Tr.register(EJBRemoteBeanFactory.class);

    @Reference(service = EJBHomeRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setEJBHomeRuntime(ServiceReference<EJBHomeRuntime> ref) {
        homeRuntime = true;
    }

    protected void unsetEJBHomeRuntime(ServiceReference<EJBHomeRuntime> ref) {
        homeRuntime = false;
    }

    @Reference(service = EJBRemoteRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setEJBRemoteRuntime(ServiceReference<EJBRemoteRuntime> ref) {
        remoteRuntime = true;
    }

    protected void unsetEJBRemoteRuntime(ServiceReference<EJBRemoteRuntime> ref) {
        remoteRuntime = false;
    }

    /** {@inheritDoc} */
    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getObjectInstance : " + obj);

        // -----------------------------------------------------------------------
        // Is obj a Reference?
        // -----------------------------------------------------------------------
        if (!(obj instanceof EJBRemoteReferenceBinding)) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : null (non-EJBRemoteReferenceBinding)");
            return null;
        }

        EJBRemoteReferenceBinding ref = (EJBRemoteReferenceBinding) obj;

        // -----------------------------------------------------------------------
        // Is the right factory for this reference?
        // -----------------------------------------------------------------------
        if (!getClass().getName().equals(ref.getFactoryClassName())) // F93680
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : null (wrong factory class: " + ref.getFactoryClassName() + ")");
            return null;
        }

        // Get the EJBBinding Object and initialize it
        EJBBinding binding = ref.getReferenceBinding();

        if (binding != null && binding.isAmbiguousReference) {
            String bindingName = ref.getBindingName();
            throwAmbiguousEJBReferenceException(binding, bindingName);
        }

        return initializeEJB(binding, binding.homeRecord.getJ2EEName().toString());
    }
}
