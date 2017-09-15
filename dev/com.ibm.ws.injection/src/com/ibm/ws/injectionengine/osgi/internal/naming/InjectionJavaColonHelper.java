/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.osgi.internal.naming;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.Remote;
import java.util.Collection;
import java.util.Collections;

import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.naming.JavaColonNamingHelper;
import com.ibm.ws.container.service.naming.NamingConstants;
import com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace;
import com.ibm.ws.container.service.naming.RemoteJavaColonNamingHelper;
import com.ibm.ws.container.service.naming.RemoteObjectInstance;
import com.ibm.ws.container.service.naming.RemoteObjectInstanceFactory;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.injectionengine.InternalInjectionEngine;
import com.ibm.ws.injectionengine.osgi.internal.IndirectReference;
import com.ibm.ws.injectionengine.osgi.internal.OSGiInjectionEngineImpl;
import com.ibm.ws.injectionengine.osgi.internal.OSGiInjectionScopeData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.serialization.SerializationContext;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionEngineAccessor;
import com.ibm.wsspi.injectionengine.InjectionException;

/**
 * This {@link JavaColonNamingHelper} implementation provides support for
 * the standard Java EE component naming context (java:comp/env). <p>
 * 
 * It is registered on the JNDI NamingHelper whiteboard and will be
 * consulted during object lookup in the appropriate namespace. <p>
 */
@Component(service = { JavaColonNamingHelper.class, RemoteJavaColonNamingHelper.class })
@Trivial
public class InjectionJavaColonHelper implements JavaColonNamingHelper, RemoteJavaColonNamingHelper {
    private static final TraceComponent tc = Tr.register(InjectionJavaColonHelper.class);

    private OSGiInjectionEngineImpl injectionEngine;

    private SerializationService serializationService;

    private RemoteObjectInstanceFactory roiFactory;

    @Reference
    protected void setInjectionEngine(OSGiInjectionEngineImpl injectionEngine) {
        this.injectionEngine = injectionEngine;
    }

    protected void unsetInjectionEngine(OSGiInjectionEngineImpl injectionEngine) {}

    @Reference
    protected void setSerializationService(SerializationService serializationService) {
        this.serializationService = serializationService;
    }

    protected void unssetSerializationService(SerializationService serializationService) {
        if (this.serializationService == serializationService) {
            this.serializationService = null;
        }
    }

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setRemoteObjectInstanceFactory(RemoteObjectInstanceFactory roiFactory) {
        this.roiFactory = roiFactory;
    }

    protected void unsetRemoteObjectInstanceFactory(RemoteObjectInstanceFactory roiFactory) {
        this.roiFactory = null;
    }

    /** {@inheritDoc} */
    @Override
    public Object getObjectInstance(JavaColonNamespace namespace, String name) throws NamingException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getObjectInstance (" + namespace + ", " + name + ")");

        Object instance = null;

        // If getInjectionBinding returns null, we do not immediately call
        // processDeferredReferenceData because we want to avoid that if
        // possible, and there might be another JavaColonNamingHelper that
        // can handle the name.  DeferredNonCompInjectionJavaColonHelper is
        // registered with a lower ranking, so it will be called after all
        // other JavaColonNamingHelper with default ranking.
        InjectionBinding<?> binding = getInjectionBinding(namespace, name);
        if (binding != null) {
            try {
                instance = binding.getInjectionObject();
            } catch (Exception ex) {
                NamingException nex = newCannotInstantiateObjectException(namespace.prefix() + name, ex);
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "getObjectInstance : " + nex.toString(true));
                throw nex;
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getObjectInstance : " + Util.identity(instance));

        return instance;
    }

    private InjectionBinding<?> getInjectionBinding(JavaColonNamespace namespace, String name) throws NamingException {
        // Obtain the injection metadata for the active component
        OSGiInjectionScopeData isd = getInjectionScopeData(namespace);

        InjectionBinding<?> binding;
        if (isd != null) {
            binding = isd.getInjectionBinding(namespace, name);
        } else {
            binding = null;
        }
        return binding;
    }

    /**
     * Internal method to obtain the injection metadata associated with
     * the specified component metadata. <p>
     * 
     * @return the associated injection metadata; or null if none exists
     */
    protected OSGiInjectionScopeData getInjectionScopeData(NamingConstants.JavaColonNamespace namespace) throws NamingException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        OSGiInjectionScopeData isd = null;

        // Get the ComponentMetaData for the currently active component.
        // There is no comp namespace if there is no active component.
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();

        if (cmd == null) {
            NamingException nex = new NamingException(Tr.formatMessage(tc, "JNDI_NON_JEE_THREAD_CWNEN1000E"));
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "getInjectionScopeData : (no CMD) : " + nex.toString(true));
            throw nex;
        }

        isd = injectionEngine.getInjectionScopeData(cmd, namespace);

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, cmd + " -> " + isd);

        return isd;
    }

    /**
     * Internal method that creates a NamingException that contains helpful
     * information regarding why a binding failed to resolve. <p>
     * 
     * The returned exception will provide similar information as the
     * CannotInstantiateObjectException from traditional WAS.
     */
    private NamingException newCannotInstantiateObjectException(String name,
                                                                Exception cause) {
        String msgTxt = Tr.formatMessage(tc, "JNDI_CANNOT_INSTANTIATE_OBJECT_CWNEN1001E", name);
        NamingException nex = new NamingException(msgTxt);
        nex.initCause(cause);

        return nex;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasObjectWithPrefix(JavaColonNamespace namespace, String name) throws NamingException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "hasObjectWithPrefix (" + namespace + ", " + name + ")");

        OSGiInjectionScopeData isd = getInjectionScopeData(namespace);
        boolean result = isd != null && isd.hasObjectWithPrefix(namespace, name);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "hasObjectWithPrefix", result);

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<? extends NameClassPair> listInstances(JavaColonNamespace namespace, String nameInContext) throws NamingException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "listInstances (" + namespace + ", " + nameInContext + ")");

        // Obtain the injection metadata for the active component
        OSGiInjectionScopeData isd = getInjectionScopeData(namespace);

        if (isd != null) {
            Collection<? extends NameClassPair> retVal = isd.listInstances(namespace, nameInContext);

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "listInstances: " + retVal);

            return retVal;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "listInstances : empty (no ISD)");
        return Collections.emptyList();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.container.service.naming.remote.RemoteJavaColonNamingHelper#getRemoteObjectInstance(com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace,
     * java.lang.String)
     */
    @Override
    public RemoteObjectInstance getRemoteObjectInstance(JavaColonNamespace namespace, String name) throws NamingException {
        RemoteObjectInstance roi = null;
        InjectionBinding<?> binding = getInjectionBinding(namespace, name);
        if (binding != null) {
            Object bindingObject = binding.getRemoteObject();
            if (bindingObject != null) {
                if (bindingObject instanceof IndirectReference) {
                    bindingObject = resolveIndirectReferences(binding, (IndirectReference) bindingObject, 0);
                }
                if (bindingObject instanceof javax.naming.Reference) {
                    SerializationContext sc = serializationService.createSerializationContext();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = null;
                    try {
                        oos = sc.createObjectOutputStream(baos);
                        oos.writeObject(bindingObject);
                    } catch (IOException ex) {
                        NamingException ne = new NamingException("Unable to serialize naming reference: " + bindingObject);
                        ne.initCause(ex);
                        throw ne;
                    } finally {
                        if (oos != null)
                            try {
                                oos.close();
                            } catch (IOException ex) {
                            }
                    }
                    roi = roiFactory.create(baos.toByteArray());
                } else if (bindingObject instanceof Remote) {
                    roi = roiFactory.create((Remote) bindingObject, binding.getInjectionClassTypeName());
                } else {
                    roi = roiFactory.create(bindingObject);
                }
            }
        }
        return roi;
    }

    @FFDCIgnore(InjectionException.class)
    private Object resolveIndirectReferences(InjectionBinding<?> binding, IndirectReference ref, int hopCounter) throws NamingException {
        if (hopCounter > 5) {
            throw new NamingException("Too many indirect references");
        }

        Object resolvedObject = ref;
        InternalInjectionEngine ie = (InternalInjectionEngine) InjectionEngineAccessor.getInstance();

        String factoryClassName = ref.getFactoryClassName();
        try {
            ObjectFactory factory = ie.getObjectFactory(factoryClassName, null);
            resolvedObject = factory.getObjectInstance(ref, null, null, null);
        } catch (Exception e) {
            NamingException ne = new NamingException();
            ne.initCause(e);
            throw ne;
        }

        if (resolvedObject instanceof IndirectReference) {
            resolvedObject = resolveIndirectReferences(binding, (IndirectReference) resolvedObject, hopCounter + 1);
        }

        return resolvedObject;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.container.service.naming.remote.RemoteJavaColonNamingHelper#hasRemoteObjectWithPrefix(com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace,
     * java.lang.String)
     */
    @Override
    public boolean hasRemoteObjectWithPrefix(JavaColonNamespace namespace, String name) throws NamingException {
        return hasObjectWithPrefix(namespace, name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.container.service.naming.remote.RemoteJavaColonNamingHelper#listRemoteInstances(com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace,
     * java.lang.String)
     */
    @Override
    public Collection<? extends NameClassPair> listRemoteInstances(JavaColonNamespace namespace, String nameInContext) throws NamingException {
        return listInstances(namespace, nameInContext);
    }

}
