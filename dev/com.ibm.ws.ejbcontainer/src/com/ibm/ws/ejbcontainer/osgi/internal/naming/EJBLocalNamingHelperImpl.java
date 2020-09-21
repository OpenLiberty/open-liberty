/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.naming;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.naming.NamingException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.naming.EJBLocalNamingHelper;
import com.ibm.ws.ejbcontainer.osgi.EJBHomeRuntime;

/**
 * This {@link EJBLocalNamingHelper} implementation provides support for
 * the standard Java EE component naming context for ejblocal: <p>
 *
 */
@Component(service = { EJBLocalNamingHelper.class, EJBLocalNamingHelperImpl.class })
public class EJBLocalNamingHelperImpl extends EJBNamingInstancer implements EJBLocalNamingHelper<EJBBinding> {

    private static final TraceComponent tc = Tr.register(EJBLocalNamingHelperImpl.class);

    private final HashMap<String, EJBBinding> EJBLocalBindings = new HashMap<String, EJBBinding>();

    private final ReentrantReadWriteLock javaColonLock = new ReentrantReadWriteLock();

    /** {@inheritDoc} */
    @Override
    public Object getObjectInstance(String name) throws NamingException {
        Lock readLock = javaColonLock.readLock();
        readLock.lock();

        EJBBinding binding;
        try {
            binding = EJBLocalBindings.get(name);
        } finally {
            readLock.unlock();
        }

        if (binding != null && binding.isAmbiguousReference) {
            throwAmbiguousEJBReferenceException(binding, name);
        }

        return initializeEJB(binding, "ejblocal:" + name);
    }

    @Reference(service = EJBHomeRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setEJBHomeRuntime(ServiceReference<EJBHomeRuntime> ref) {
        homeRuntime = true;
    }

    protected void unsetEJBHomeRuntime(ServiceReference<EJBHomeRuntime> ref) {
        homeRuntime = false;
    }

    @Override
    public synchronized boolean bind(EJBBinding binding, String name, boolean isSimpleName) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        boolean notAmbiguous = true;
        EJBBinding newBinding = new EJBBinding(binding.homeRecord, binding.interfaceName, binding.interfaceIndex, binding.isLocal);

        if (name.toLowerCase().startsWith("ejblocal:")) {
            name = name.substring(9);
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "bind: " + name);
        }

        EJBBinding previousBinding = EJBLocalBindings.get(name);

        // There won't be a previous binding for an ambiguous simple binding name
        if (isSimpleName) {
            newBinding.setAmbiguousReference();
            notAmbiguous = false;
        }

        if (previousBinding != null) {
            newBinding.setAmbiguousReference();
            newBinding.addJ2EENames(previousBinding.getJ2EENames());
            notAmbiguous = false;
        }

        Lock writeLock = javaColonLock.writeLock();
        writeLock.lock();
        try {
            EJBLocalBindings.put(name, newBinding);
        } finally {
            writeLock.unlock();
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "bind: notAmbiguous = " + notAmbiguous);
        }

        return notAmbiguous;
    }

    public void unbind(String name) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "unbinding: " + name);
        }

        Lock writeLock = javaColonLock.writeLock();
        writeLock.lock();

        try {
            EJBLocalBindings.remove(name);
        } finally {
            writeLock.unlock();
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "unbind");
        }
    }

    @Override
    public void removeBindings(List<String> names) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        Lock writeLock = javaColonLock.writeLock();
        writeLock.lock();

        try {
            for (String name : names) {
                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "unbinding: " + name);
                }
                EJBLocalBindings.remove(name);
            }
        } finally {
            writeLock.unlock();
        }
    }

}
