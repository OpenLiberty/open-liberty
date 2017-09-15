/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.adaptable.module.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.kernel.service.util.PrivHelper;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.AdapterFactoryService;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.adaptable.module.adapters.EntryAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap;
import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;

@Component(xmlns = "http://felix.apache.org/xmlns/scr/v1.2.0-felix",
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM" })
public class AdapterFactoryServiceImpl implements AdapterFactoryService {

    private final static String toType = "toType";

    private final ConcurrentServiceReferenceSetMap<String, ContainerAdapter<?>> containerHelperMap = new ConcurrentServiceReferenceSetMap<String, ContainerAdapter<?>>("containerHelper");
    private final ConcurrentServiceReferenceSetMap<String, EntryAdapter<?>> entryHelperMap = new ConcurrentServiceReferenceSetMap<String, EntryAdapter<?>>("entryHelper");
    private final Set<String> containerToTypes = Collections.synchronizedSet(new HashSet<String>());
    private final Set<String> entryToTypes = Collections.synchronizedSet(new HashSet<String>());
    private Map<String, Object> baseProperties;

    @Activate
    protected Map<String, Object> activate(ComponentContext cCtx, Map<String, Object> properties) {
        entryHelperMap.activate(cCtx);
        containerHelperMap.activate(cCtx);
        baseProperties = properties;
        return getProperties();
    }

    @Deactivate
    protected void deactivate(ComponentContext cCtx) {
        entryHelperMap.deactivate(cCtx);
        containerHelperMap.deactivate(cCtx);
        baseProperties = null;
    }

    @Reference(name = "containerHelper", service = ContainerAdapter.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected Map<String, Object> setContainerHelper(ServiceReference<ContainerAdapter<?>> helper) {
        //ignore helpers that don't have the properties we require.
        Object o = helper.getProperty(toType);
        if (o == null)
            return null;

        if (o instanceof String) {
            String key = (String) o;
            containerToTypes.add(key);
            containerHelperMap.putReference(key, helper);
        } else if (o instanceof String[]) {
            for (String key : (String[]) o) {
                containerToTypes.add(key);
                containerHelperMap.putReference(key, helper);
            }
        }
        return getProperties();
    }

    @Reference(name = "entryHelper", service = EntryAdapter.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected Map<String, Object> setEntryHelper(ServiceReference<EntryAdapter<?>> helper) {
        //ignore helpers that don't have the properties we require.
        Object o = helper.getProperty(toType);
        if (o == null)
            return null;

        if (o instanceof String) {
            String key = (String) o;
            entryToTypes.add(key);
            entryHelperMap.putReference(key, helper);
        } else if (o instanceof String[]) {
            for (String key : (String[]) o) {
                entryToTypes.add(key);
                entryHelperMap.putReference(key, helper);
            }
        }
        return getProperties();
    }

    protected Map<String, Object> unsetContainerHelper(ServiceReference<ContainerAdapter<?>> helper) {
        //ignore helpers that don't have the properties we require.
        Object o = helper.getProperty(toType);
        if (o == null)
            return null;

        if (o instanceof String) {
            String key = (String) o;
            containerHelperMap.removeReference(key, helper);
            Iterator<ContainerAdapter<?>> test = containerHelperMap.getServices(key);
            if (test == null || !test.hasNext()) {
                containerToTypes.remove(key);
            }
        } else if (o instanceof String[]) {
            for (String key : (String[]) o) {
                containerHelperMap.removeReference(key, helper);
                Iterator<ContainerAdapter<?>> test = containerHelperMap.getServices(key);
                if (test == null || !test.hasNext()) {
                    containerToTypes.remove(key);
                }
            }
        }
        return getProperties();
    }

    protected Map<String, Object> unsetEntryHelper(ServiceReference<EntryAdapter<?>> helper) {
        //ignore helpers that don't have the properties we require.
        Object o = helper.getProperty(toType);
        if (o == null)
            return null;

        if (o instanceof String) {
            String key = (String) o;
            entryHelperMap.removeReference(key, helper);
            Iterator<EntryAdapter<?>> test = entryHelperMap.getServices(key);
            if (test == null || !test.hasNext()) {
                entryToTypes.remove(key);
            }
        } else if (o instanceof String[]) {
            for (String key : (String[]) o) {
                entryHelperMap.removeReference(key, helper);
                Iterator<EntryAdapter<?>> test = entryHelperMap.getServices(key);
                if (test == null || !test.hasNext()) {
                    entryToTypes.remove(key);
                }
            }
        }
        return getProperties();
    }

    private Map<String, Object> getProperties() {
        if (baseProperties == null) {
            return null;
        }
        Map<String, Object> props = new HashMap<String, Object>(baseProperties);
        props.put("containerToType", containerToTypes.toArray(new String[containerToTypes.size()]));
        props.put("entryToType", entryToTypes.toArray(new String[entryToTypes.size()]));
        return props;
    }

    @Override
    public <T> T adapt(final Container root, final OverlayContainer rootOverlay, final ArtifactContainer artifactContainer, final Container containerToAdapt, final Class<T> t) throws UnableToAdaptException {
        String key = t.getName();
        Iterator<ServiceAndServiceReferencePair<ContainerAdapter<?>>> i = containerHelperMap.getServicesWithReferences(key);
        if (i != null) {
            while (i.hasNext()) {
                ServiceAndServiceReferencePair<ContainerAdapter<?>> sandr = i.next();
                ServiceReference<ContainerAdapter<?>> sr = sandr.getServiceReference();
                try {
                    //consistency check, make sure that adapters idea of 't' matches ours
                    //has to be done because we are using string service properties to work round
                    //type erasure for generics in service references in osgi                
                    Class<?> clz = PrivHelper.loadClass(sr.getBundle(), t.getName());
                    if (clz == t) {
                        ContainerAdapter<?> ca = sandr.getService();
                        if (ca != null) {
                            @SuppressWarnings("unchecked")
                            T adapted = (T) ca.adapt(root, rootOverlay, artifactContainer, containerToAdapt);
                            if (adapted != null) {
                                return adapted;
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    //bad adapter bundle, claims to adapt to 't' but doesn't seem able to load 't'.
                }
            }
        }
        return null;
    }

    @Override
    public <T> T adapt(Container root, OverlayContainer rootOverlay, ArtifactEntry artifactEntry, Entry entryToAdapt, Class<T> t) throws UnableToAdaptException {
        String key = t.getName();
        Iterator<ServiceAndServiceReferencePair<EntryAdapter<?>>> i = entryHelperMap.getServicesWithReferences(key);
        if (i != null) {
            while (i.hasNext()) {
                ServiceAndServiceReferencePair<EntryAdapter<?>> sandr = i.next();
                ServiceReference<EntryAdapter<?>> sr = sandr.getServiceReference();
                try {
                    //consistency check, make sure that adapters idea of 't' matches ours
                    //has to be done because we are using string service properties to work round
                    //type erasure for generics in service references in osgi
                    Class<?> clz = PrivHelper.loadClass(sr.getBundle(), t.getName());
                    if (clz == t) {
                        EntryAdapter<?> ea = sandr.getService();
                        if (ea != null) {
                            @SuppressWarnings("unchecked")
                            T adapted = (T) ea.adapt(root, rootOverlay, artifactEntry, entryToAdapt);
                            if (adapted != null) {
                                return adapted;
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    //bad adapter bundle, claims to adapt to 't' but doesnt seem able to load 't'.
                }
            }
        }
        return null;
    }
}
