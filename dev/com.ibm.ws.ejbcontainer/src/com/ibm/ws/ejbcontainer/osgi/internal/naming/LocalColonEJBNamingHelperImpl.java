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

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.ContainerProperties;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.naming.LocalColonEJBNamingHelper;
import com.ibm.ws.ejbcontainer.osgi.EJBHomeRuntime;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

/**
 * This {@link LocalColonEJBNamingHelper} implementation provides support for
 * the standard Java EE component naming context for local: <p>
 *
 */
@Component(service = { LocalColonEJBNamingHelper.class, LocalColonEJBNamingHelperImpl.class })
public class LocalColonEJBNamingHelperImpl extends EJBNamingInstancer implements LocalColonEJBNamingHelper<EJBBinding> {

    private static final TraceComponent tc = Tr.register(LocalColonEJBNamingHelperImpl.class, "EJBContainer", "com.ibm.ejs.container.container");

    private final HashMap<String, EJBBinding> localColonEJBBindings = new HashMap<String, EJBBinding>();

    private final ReentrantReadWriteLock javaColonLock = new ReentrantReadWriteLock();

    /** {@inheritDoc} */
    @Override
    public Object getObjectInstance(String name) throws NamingException {
        Lock readLock = javaColonLock.readLock();
        readLock.lock();

        EJBBinding binding;
        try {
            binding = localColonEJBBindings.get(name);
        } finally {
            readLock.unlock();
        }

        if (binding != null && binding.isAmbiguousReference) {
            throwAmbiguousEJBReferenceException(binding, name);
        }

        return initializeEJB(binding, "local:" + name);
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
    public synchronized boolean bind(EJBBinding binding, String name, boolean isSimpleName) throws NamingException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        boolean notAmbiguous = true;
        EJBBinding newBinding = new EJBBinding(binding.homeRecord, binding.interfaceName, binding.interfaceIndex, binding.isLocal);

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "bind: " + name);
        }

        EJBBinding previousBinding = localColonEJBBindings.get(name);

        // There won't be a previous binding for an ambiguous simple binding name
        if (isSimpleName) {
            newBinding.setAmbiguousReference();
            notAmbiguous = false;
        }

        if (previousBinding != null) {

            OnError onError = ContainerProperties.customBindingsOnErr;

            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "found ambiguous binding and customBindingsOnErr=" + onError.toString());
            }

            BeanMetaData bmd = newBinding.homeRecord.getBeanMetaData();
            BeanMetaData oldbmd = previousBinding.homeRecord.getBeanMetaData();
            switch (onError) {
                case WARN:
                    //NAME_ALREADY_BOUND_WARN_CNTR0338W=CNTR0338W: The {0} interface of the {1} bean in the {2} module of the {3} application cannot be bound to the {4} name location. The {5} interface of the {6} bean in the {7} module of the {8} application is already bound to the {4} name location. The {4} name location is not accessible.
                    Tr.warning(tc, "NAME_ALREADY_BOUND_WARN_CNTR0338W",
                               new Object[] { newBinding.interfaceName, bmd.j2eeName.getComponent(), bmd.j2eeName.getModule(), bmd.j2eeName.getApplication(), name,
                                              previousBinding.interfaceName, oldbmd.j2eeName.getComponent(), oldbmd.j2eeName.getModule(), oldbmd.j2eeName.getApplication() });
                    break;
                case FAIL:
                    Tr.error(tc, "NAME_ALREADY_BOUND_WARN_CNTR0338W",
                             new Object[] { newBinding.interfaceName, bmd.j2eeName.getComponent(), bmd.j2eeName.getModule(), bmd.j2eeName.getApplication(), name,
                                            previousBinding.interfaceName, oldbmd.j2eeName.getComponent(), oldbmd.j2eeName.getModule(), oldbmd.j2eeName.getApplication() });
                    throw new NamingException("The " + newBinding.interfaceName + " interface of the " + bmd.j2eeName.getComponent() + " bean in the "
                                              + bmd.j2eeName.getModule() + " module of the application cannot be bound to " + name
                                              + ", a bean is already bound to that location.");
                case IGNORE:
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(tc, "customBindingsOnErr is IGNORE, not binding");
                    }
                    return false;
            }

            newBinding.setAmbiguousReference();
            newBinding.addJ2EENames(previousBinding.getJ2EENames());
            notAmbiguous = false;
        }

        Lock writeLock = javaColonLock.writeLock();
        writeLock.lock();
        try {
            localColonEJBBindings.put(name, newBinding);
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
            localColonEJBBindings.remove(name);
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
                localColonEJBBindings.remove(name);
            }
        } finally {
            writeLock.unlock();
        }
    }

}
