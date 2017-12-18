/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.internal;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

import com.ibm.ws.artifact.contributor.ArtifactContainerFactoryHelper;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.artifact.factory.contributor.ArtifactContainerFactoryContributor;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap;
import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;

@Component(xmlns="http://felix.apache.org/xmlns/scr/v1.2.0-felix",
immediate = true, 
configurationPolicy = ConfigurationPolicy.IGNORE, 
property = { "service.vendor=IBM" })
public class ArtifactContainerFactoryService implements ArtifactContainerFactory {

    /**  */
    private static final String CATEGORY_PROP_NAME = "category";
    private final String ENTRY_KEY = ArtifactEntry.class.getName();
    private final ConcurrentServiceReferenceSetMap<String, ArtifactContainerFactoryHelper> helperMap = new ConcurrentServiceReferenceSetMap<String, ArtifactContainerFactoryHelper>("Helper");
    private final ConcurrentServiceReferenceSetMap<String, ArtifactContainerFactoryContributor> contributorMap = new ConcurrentServiceReferenceSetMap<String, ArtifactContainerFactoryContributor>("Contributor");
    private final ConcurrentServiceReferenceSetMap<String, ArtifactContainerFactoryHelper> helperCategoryMap = new ConcurrentServiceReferenceSetMap<String, ArtifactContainerFactoryHelper>("Helper");
    private final ConcurrentServiceReferenceSetMap<String, ArtifactContainerFactoryContributor> contributorCategoryMap = new ConcurrentServiceReferenceSetMap<String, ArtifactContainerFactoryContributor>("Contributor");
    private final Set<String> categories = Collections.synchronizedSet(new HashSet<String>());

    private Map<String, Object> baseProperties;

    @Activate
    protected Map<String, Object> activate(ComponentContext cCtx, Map<String, Object> properties) {
        helperMap.activate(cCtx);
        contributorMap.activate(cCtx);
        helperCategoryMap.activate(cCtx);
        contributorCategoryMap.activate(cCtx);
        this.baseProperties = properties;
        return getProperties();
    }

    @Deactivate
    protected Map<String, Object> deactivate(ComponentContext cCtx) {
        helperMap.deactivate(cCtx);
        contributorMap.deactivate(cCtx);
        helperCategoryMap.deactivate(cCtx);
        contributorCategoryMap.deactivate(cCtx);
        Map<String, Object> props = baseProperties;
        baseProperties = null;
        return props;
    }

    private Map<String, Object> getProperties() {
        if (baseProperties == null) {
            return null;
        }
        Map<String, Object> props = new HashMap<String, Object>(baseProperties);
        props.put(CATEGORY_PROP_NAME, categories.toArray(new String[categories.size()]));
        return props;
    }

    @Reference(service = ArtifactContainerFactoryHelper.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected Map<String, Object> setHelper(ServiceReference<ArtifactContainerFactoryHelper> helper) {
        return internalSetContributor(helper, helperMap, helperCategoryMap);
    }

    @Reference(service = ArtifactContainerFactoryContributor.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected Map<String, Object> setContributor(ServiceReference<ArtifactContainerFactoryContributor> helper) {
        return internalSetContributor(helper, contributorMap, contributorCategoryMap);
    }

    protected <T extends ArtifactContainerFactoryContributor> Map<String, Object> internalSetContributor(ServiceReference<T> helper,
                                                                                                         ConcurrentServiceReferenceSetMap<String, T> selectedMap,
                                                                                                         ConcurrentServiceReferenceSetMap<String, T> selectedCategoryMap) {
        if (helper.getProperty("handlesType") != null) {
            Object o = helper.getProperty("handlesType");
            if (o instanceof String) {
                String key = (String) o;
                selectedMap.putReference(key, helper);
            } else if (o instanceof String[]) {
                for (String key : (String[]) o) {
                    selectedMap.putReference(key, helper);
                }
            }
        }
        if (helper.getProperty(CATEGORY_PROP_NAME) != null) {
            Object o = helper.getProperty(CATEGORY_PROP_NAME);
            if (o instanceof String) {
                String key = (String) o;
                categories.add(key);
                selectedCategoryMap.putReference(key, helper);
            } else if (o instanceof String[]) {
                for (String key : (String[]) o) {
                    categories.add(key);
                    selectedCategoryMap.putReference(key, helper);
                }
            }
        }
        return getProperties();
    }

    protected Map<String, Object> unsetHelper(ServiceReference<ArtifactContainerFactoryHelper> helper) {
        return internalUnsetContributor(helper, helperMap, helperCategoryMap, contributorCategoryMap);
    }

    protected Map<String, Object> unsetContributor(ServiceReference<ArtifactContainerFactoryContributor> helper) {
        return internalUnsetContributor(helper, contributorMap, contributorCategoryMap, helperCategoryMap);
    }

    protected <T extends ArtifactContainerFactoryContributor, U extends ArtifactContainerFactoryContributor> Map<String, Object> internalUnsetContributor(ServiceReference<T> helper,
                                                                                                                                                          ConcurrentServiceReferenceSetMap<String, T> selectedMap,
                                                                                                                                                          ConcurrentServiceReferenceSetMap<String, T> selectedCategoryMap,
                                                                                                                                                          ConcurrentServiceReferenceSetMap<String, U> otherCategoryMap) {
        if (helper.getProperty("handlesType") != null) {
            Object o = helper.getProperty("handlesType");
            if (o instanceof String) {
                String key = (String) o;
                selectedMap.removeReference(key, helper);
            } else if (o instanceof String[]) {
                for (String key : (String[]) o) {
                    selectedMap.removeReference(key, helper);
                }
            }
        }
        if (helper.getProperty(CATEGORY_PROP_NAME) != null) {
            Object o = helper.getProperty(CATEGORY_PROP_NAME);
            if (o instanceof String) {
                String key = (String) o;
                selectedCategoryMap.removeReference(key, helper);
                Iterator<T> test = selectedCategoryMap.getServices(key);
                Iterator<U> test2 = otherCategoryMap.getServices(key);
                if ((test == null || !test.hasNext()) && (test2 == null || !test2.hasNext())) {
                    categories.remove(key);
                }
            } else if (o instanceof String[]) {
                for (String key : (String[]) o) {
                    selectedCategoryMap.removeReference(key, helper);
                    Iterator<T> test = selectedCategoryMap.getServices(key);
                    Iterator<U> test2 = otherCategoryMap.getServices(key);
                    if ((test == null || !test.hasNext()) && (test2 == null || !test2.hasNext())) {
                        categories.remove(key);
                    }
                }
            }
        }
        return getProperties();
    }

    @Override
    public ArtifactContainer getContainer(final File cacheDir, final Object o) {
        ArtifactContainer c = getContainerForObjectClass(o, new ContainerCallback() {
            @Override
            public ArtifactContainer getContainer(ArtifactContainerFactoryContributor cfh, ServiceReference<? extends ArtifactContainerFactoryContributor> sr) {
                return cfh.createContainer(cacheDir, o);
            }
        });

        return c;
    }

    private ArtifactContainer getContainerForClassName(final String className, final Object o, ContainerCallback containerCallback) {
        //ask each of the maps in turn, exit early if we find a match.
        Iterator<ServiceAndServiceReferencePair<ArtifactContainerFactoryHelper>> ih = helperMap.getServicesWithReferences(className);
        ArtifactContainer ch = getContainerForReference(ih, className, o, containerCallback);
        if (ch != null)
            return ch;
        Iterator<ServiceAndServiceReferencePair<ArtifactContainerFactoryContributor>> ic = contributorMap.getServicesWithReferences(className);
        ArtifactContainer cc = getContainerForReference(ic, className, o, containerCallback);
        return cc;
    }

    /**
     * @param o
     * @return
     */
    private ArtifactContainer getContainerForObjectClass(final Object o, ContainerCallback containerCallback) {
        // First try with the class name of the object
        Class<?> classToTest = o.getClass();
        String key = classToTest.getName();
        ArtifactContainer container = getContainerForClassName(key, o, containerCallback);
        if (container == null) {
            // Couldn't find one for this class so try for the interfaces so that the user can specify an interface for the object type
            do {
                Class<?>[] classInterfaces = classToTest.getInterfaces();
                for (Class<?> interfaceClass : classInterfaces) {
                    container = getContainerForClassName(interfaceClass.getName(), o, containerCallback);

                    // Found one!  Break out as we don't need to search any of the other interfaces
                    if (container != null) {
                        break;
                    }
                }

                // Still not found it, try the interfaces on the supertype
                classToTest = classToTest.getSuperclass();
            } while (container == null && classToTest != null);
        }
        return container;
    }

    @Override
    public ArtifactContainer getContainer(final File cacheDir, final ArtifactContainer parent, final ArtifactEntry e, final Object o) {
        ArtifactContainer c = getContainerForObjectClass(o, new ContainerCallback() {
            @Override
            public ArtifactContainer getContainer(ArtifactContainerFactoryContributor cfh, ServiceReference<? extends ArtifactContainerFactoryContributor> sr) {
                return cfh.createContainer(cacheDir, parent, e, o);
            }
        });

        if (c == null) {
            //if we are still here, no-one claimed ownership.. but maybe someone is able to handle Entry.
            c = getContainerForClassName(ENTRY_KEY, o, new ContainerCallback() {
                @Override
                public ArtifactContainer getContainer(ArtifactContainerFactoryContributor cfh, ServiceReference<? extends ArtifactContainerFactoryContributor> sr) {
                    //check for the extension list..
                    Object handlesEntries = sr.getProperty("handlesEntries");
                    if (handlesEntries != null) {
                        //extension list present, build list to check.
                        Set<String> values = new HashSet<String>();
                        if (handlesEntries instanceof String) {
                            values.add((String) handlesEntries);
                        } else if (handlesEntries instanceof String[]) {
                            List<String> s = Arrays.asList((String[]) handlesEntries);
                            values.addAll(s);
                        }
                        //compare list against name
                        String name = e.getName();
                        for (String s : values) {
                            // endsWith ignore case
                            if (name.regionMatches(true, name.length() - s.length(), s, 0, s.length())) {
                                //hit, send to cfh.
                                return cfh.createContainer(cacheDir, parent, e, e);
                            }
                        }
                        //no match, do not create container.
                        return null;
                    } else {
                        //no extension list, invoke for all extensions.
                        return cfh.createContainer(cacheDir, parent, e, e);
                    }
                }
            });
        }
        return c;
    }

    private interface ContainerCallback {
        public ArtifactContainer getContainer(ArtifactContainerFactoryContributor cfh, ServiceReference<? extends ArtifactContainerFactoryContributor> sr);
    }

    private <T extends ArtifactContainerFactoryContributor> ArtifactContainer getContainerForReference(Iterator<ServiceAndServiceReferencePair<T>> i, String key, Object o,
                                                                                                       ContainerCallback c) {
        if (i == null) {
            return null;
        }
        while (i.hasNext()) {
            ServiceAndServiceReferencePair<T> sandr = i.next();
            ServiceReference<T> sr = sandr.getServiceReference();
            try {
                //need to check this service will understand the type we are going to pass
                boolean passed = false;
                if (ENTRY_KEY.equals(key)) {
                    //if we are passing entry, then osgi has made sure it already understands.
                    passed = true;
                } else {
                    //check that this service's view of the key class is the same as ours
                    //otherwise it will be incompatible. We have to handle this because we
                    //are managing the type via a String, not via OSGi, due to type erasure.
                    // As we also support interfaces use isInstance instead of equality check
                    Class<?> clz = sr.getBundle().loadClass(key);
                    if (clz.isInstance(o)) {
                        passed = true;
                    }
                }
                if (passed) {
                    T cfh = sandr.getService();
                    //use the callback to invoke the right method on cfh.
                    ArtifactContainer container = c.getContainer(cfh, sr);
                    if (container != null) {
                        return container;
                    }
                }
            } catch (ClassNotFoundException ex) {
                //broken container factory helper, not a user error.
                //means someone is supplying a bundle that offers a factory helper for 
                //type 'xxx' when the bundle offering that helper is unable to load 'xxx'.
            }
        }
        return null;
    }
}
