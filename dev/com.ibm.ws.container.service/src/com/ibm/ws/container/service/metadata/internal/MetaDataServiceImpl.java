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
package com.ibm.ws.container.service.metadata.internal;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.metadata.ApplicationMetaDataListener;
import com.ibm.ws.container.service.metadata.ComponentMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.MetaDataService;
import com.ibm.ws.container.service.metadata.MethodMetaDataListener;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory;
import com.ibm.ws.container.service.metadata.extended.IdentifiableComponentMetaData;
import com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.MethodMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;

public class MetaDataServiceImpl implements MetaDataService, MetaDataIdentifierService {

    private final ApplicationMetaDataManager applicationMetaDataManager = new ApplicationMetaDataManager("applicationMetaDataListeners");
    private final ModuleMetaDataManager moduleMetaDataManager = new ModuleMetaDataManager("moduleMetaDataListeners");
    private final ComponentMetaDataManager componentMetaDataManager = new ComponentMetaDataManager("componentMetaDataListeners");
    private final MethodMetaDataManager methodMetaDataManager = new MethodMetaDataManager("methodMetaDataListeners");

    private final ConcurrentHashMap<String, Container> appModuleNameToModuleContainer = new ConcurrentHashMap<String, Container>();
    private final ConcurrentHashMap<String, ComponentMetaData> jeeNameToComponentMetaData = new ConcurrentHashMap<String, ComponentMetaData>();
    private final ConcurrentServiceReferenceSet<DeferredMetaDataFactory> deferredMetaDataFactories = new ConcurrentServiceReferenceSet<DeferredMetaDataFactory>("deferredMetaDataFactory");
    private final ConcurrentServiceReferenceMap<String, DeferredMetaDataFactory> deferredMetaDataInitializers = new ConcurrentServiceReferenceMap<String, DeferredMetaDataFactory>("deferredMetaDataFactory");

    protected void activate(ComponentContext cc) {
        applicationMetaDataManager.activate(cc);
        moduleMetaDataManager.activate(cc);
        componentMetaDataManager.activate(cc);
        methodMetaDataManager.activate(cc);
        deferredMetaDataFactories.activate(cc);
        deferredMetaDataInitializers.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        applicationMetaDataManager.deactivate(cc);
        moduleMetaDataManager.deactivate(cc);
        componentMetaDataManager.deactivate(cc);
        methodMetaDataManager.deactivate(cc);
        deferredMetaDataFactories.deactivate(cc);
        deferredMetaDataInitializers.deactivate(cc);
    }

    @SuppressWarnings("unchecked")
    <M extends MetaData> MetaDataManager<M, ?> getMetaDataManager(Class<M> metaDataClass) {
        if (metaDataClass == ApplicationMetaData.class) {
            return (MetaDataManager<M, ?>) applicationMetaDataManager;
        }
        if (metaDataClass == ModuleMetaData.class) {
            return (MetaDataManager<M, ?>) moduleMetaDataManager;
        }
        if (metaDataClass == ComponentMetaData.class) {
            return (MetaDataManager<M, ?>) componentMetaDataManager;
        }
        if (metaDataClass == MethodMetaData.class) {
            return (MetaDataManager<M, ?>) methodMetaDataManager;
        }
        throw new IllegalArgumentException(String.valueOf(metaDataClass));
    }

    // declarative services
    public void addApplicationMetaDataListener(ServiceReference<ApplicationMetaDataListener> ref) {
        applicationMetaDataManager.addListener(ref);
    }

    // declarative services
    public void removeApplicationMetaDataListener(ServiceReference<ApplicationMetaDataListener> ref) {
        applicationMetaDataManager.removeListener(ref);
    }

    @Override
    public void fireApplicationMetaDataCreated(ApplicationMetaData metaData, Container container) throws MetaDataException {
        if (container == null) {
            throw new IllegalArgumentException("container");
        }
        applicationMetaDataManager.fireMetaDataCreated(metaData, container);
    }

    @Override
    public void fireApplicationMetaDataDestroyed(ApplicationMetaData metaData) {
        applicationMetaDataManager.fireMetaDataDestroyed(metaData);
    }

    // declarative services
    public void addModuleMetaDataListener(ServiceReference<ModuleMetaDataListener> ref) {
        moduleMetaDataManager.addListener(ref);
    }

    // declarative services
    public void removeModuleMetaDataListener(ServiceReference<ModuleMetaDataListener> ref) {
        moduleMetaDataManager.removeListener(ref);
    }

    @Override
    public void fireModuleMetaDataCreated(ModuleMetaData metaData, Container container) throws MetaDataException {
        if (container == null) {
            throw new IllegalArgumentException("container");
        }
        J2EEName jeeName = metaData.getJ2EEName();
        if (jeeName != null)
            appModuleNameToModuleContainer.put(jeeName.toString(), container);
        moduleMetaDataManager.fireMetaDataCreated(metaData, container);
    }

    @Override
    public void fireModuleMetaDataDestroyed(ModuleMetaData metaData) {
        J2EEName jeeName = metaData.getJ2EEName();
        if (jeeName != null)
            appModuleNameToModuleContainer.remove(jeeName.toString());
        moduleMetaDataManager.fireMetaDataDestroyed(metaData);
    }

    // declarative services
    public void addComponentMetaDataListener(ServiceReference<ComponentMetaDataListener> ref) {
        componentMetaDataManager.addListener(ref);
    }

    // declarative services
    public void removeComponentMetaDataListener(ServiceReference<ComponentMetaDataListener> ref) {
        componentMetaDataManager.removeListener(ref);
    }

    @Override
    public void fireComponentMetaDataCreated(ComponentMetaData metaData) throws MetaDataException {
        J2EEName jeeName = metaData.getJ2EEName();
        if (jeeName != null)
            jeeNameToComponentMetaData.put(jeeName.toString(), metaData);

        componentMetaDataManager.fireMetaDataCreated(metaData, null);
    }

    @Override
    public void fireComponentMetaDataDestroyed(ComponentMetaData metaData) {
        J2EEName jeeName = metaData.getJ2EEName();
        if (jeeName != null)
            jeeNameToComponentMetaData.remove(jeeName.toString());

        componentMetaDataManager.fireMetaDataDestroyed(metaData);
    }

    // declarative services
    public void addMethodMetaDataListener(ServiceReference<MethodMetaDataListener> ref) {
        methodMetaDataManager.addListener(ref);
    }

    // declarative services
    public void removeMethodMetaDataListener(ServiceReference<MethodMetaDataListener> ref) {
        methodMetaDataManager.removeListener(ref);
    }

    @Override
    public void fireMethodMetaDataCreated(MethodMetaData metaData) throws MetaDataException {
        methodMetaDataManager.fireMetaDataCreated(metaData, null);
    }

    @Override
    public void fireMethodMetaDataDestroyed(MethodMetaData metaData) {
        methodMetaDataManager.fireMetaDataDestroyed(metaData);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService#getMetaDataIdentifier(com.ibm.ws.runtime.metadata.MetaData)
     */
    @Override
    public String getMetaDataIdentifier(MetaData metaData) throws IllegalArgumentException {
        if (metaData instanceof IdentifiableComponentMetaData) {
            return ((IdentifiableComponentMetaData) metaData).getPersistentIdentifier();
        } else if (metaData == null) {
            return null;
        } else {
            throw new IllegalArgumentException(metaData.getClass().getCanonicalName());
        }
    }

    // declarative services bind method
    protected void setDeferredMetaDataFactory(ServiceReference<DeferredMetaDataFactory> ref) {
        deferredMetaDataFactories.addReference(ref);

        if (Boolean.TRUE.equals(ref.getProperty("supportsDeferredInit"))) {
            Object types = ref.getProperty("deferredMetaData");
            if (types instanceof String)
                deferredMetaDataInitializers.putReference((String) types, ref);
            else if (types instanceof String[])
                for (String type : (String[]) types)
                    deferredMetaDataInitializers.putReference(type, ref);
            else if (types instanceof List)
                for (Object type : (List<?>) types)
                    deferredMetaDataInitializers.putReference((String) type, ref);
        }
    }

    // declarative services unbind method
    protected void unsetDeferredMetaDataFactory(ServiceReference<DeferredMetaDataFactory> ref) {
        deferredMetaDataFactories.removeReference(ref);

        if (Boolean.TRUE.equals(ref.getProperty("supportsDeferredInit"))) {
            Object types = ref.getProperty("deferredMetaData");
            if (types instanceof String)
                deferredMetaDataInitializers.removeReference((String) types, ref);
            else if (types instanceof String[])
                for (String type : (String[]) types)
                    deferredMetaDataInitializers.removeReference(type, ref);
            else if (types instanceof List)
                for (Object type : (List<?>) types)
                    deferredMetaDataInitializers.removeReference((String) type, ref);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService#getMetaData(java.lang.String)
     */
    @Override
    public MetaData getMetaData(String identifier) throws IllegalStateException {
        if (identifier == null) {
            return null;
        }

        int index = identifier.indexOf('#');
        String prefix = identifier.substring(0, index);
        String jeeName = identifier.substring(index + 1);
        ComponentMetaData cmd = jeeNameToComponentMetaData.get(jeeName);

        if (cmd == null) {
            for (Iterator<ServiceAndServiceReferencePair<DeferredMetaDataFactory>> iterator = deferredMetaDataFactories.getServicesWithReferences(); cmd == null
                                                                                                                                                     && iterator.hasNext();) {
                ServiceAndServiceReferencePair<DeferredMetaDataFactory> pair = iterator.next();
                Object type = pair.getServiceReference().getProperty("deferredMetaData");
                if (prefix.equals(type)
                    || type instanceof String[] && Arrays.asList((String[]) type).contains(prefix)
                    || type instanceof List && ((List<?>) type).contains(prefix)) {
                    DeferredMetaDataFactory factory = pair.getService();
                    cmd = factory.createComponentMetaData(identifier);
                }
            }
        }
        if (cmd == null)
            throw new IllegalStateException(identifier);

        DeferredMetaDataFactory deferredInitializer = deferredMetaDataInitializers.getService(prefix);
        if (deferredInitializer != null)
            deferredInitializer.initialize(cmd);

        return cmd;
    }

    /**
     * TODO: do we expect only 1 DeferredMDF for each prefix/type? Or would this method be more useful
     * returning a Set?
     * 
     * @return the DeferredMetaDataFactory who has deferredMetaData={prefix}, or null if none exists
     */
    protected DeferredMetaDataFactory getDeferredMetaDataFactoryForPrefix(String prefix) {

        for (Iterator<ServiceAndServiceReferencePair<DeferredMetaDataFactory>> iterator = deferredMetaDataFactories.getServicesWithReferences(); iterator.hasNext();) {
            ServiceAndServiceReferencePair<DeferredMetaDataFactory> pair = iterator.next();
            Object type = pair.getServiceReference().getProperty("deferredMetaData");
            if (prefix.equals(type)
                || type instanceof String[] && Arrays.asList((String[]) type).contains(prefix)
                || type instanceof List && ((List<?>) type).contains(prefix)) {
                return pair.getService();
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMetaDataIdentifier(String type, String appName, String moduleName, String componentName) throws IllegalArgumentException {
        DeferredMetaDataFactory factory = getDeferredMetaDataFactoryForPrefix(type);

        return (factory != null)
                        ? factory.getMetaDataIdentifier(appName, moduleName, componentName)
                        : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassLoader getClassLoader(String type, ComponentMetaData metadata) {
        DeferredMetaDataFactory factory = getDeferredMetaDataFactoryForPrefix(type);

        return (factory != null)
                        ? factory.getClassLoader(metadata)
                        : null;
    }

    /**
     * @see com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService#isMetaDataAvailable(java.lang.String)
     */
    @Override
    public boolean isMetaDataAvailable(String identifier) {
        if (identifier == null)
            return false;

        int index = identifier.indexOf('#');
        String prefix = identifier.substring(0, index);
        String jeeName = identifier.substring(index + 1);
        ComponentMetaData cmd = jeeNameToComponentMetaData.get(jeeName);

        if (cmd == null) {
            for (Iterator<ServiceAndServiceReferencePair<DeferredMetaDataFactory>> iterator = deferredMetaDataFactories.getServicesWithReferences(); cmd == null
                                                                                                                                                     && iterator.hasNext();) {
                ServiceAndServiceReferencePair<DeferredMetaDataFactory> pair = iterator.next();
                Object type = pair.getServiceReference().getProperty("deferredMetaData");
                if (prefix.equals(type)
                    || type instanceof String[] && Arrays.asList((String[]) type).contains(prefix)
                    || type instanceof List && ((List<?>) type).contains(prefix)) {
                    DeferredMetaDataFactory factory = pair.getService();
                    cmd = factory.createComponentMetaData(identifier);
                }
            }
        }

        return cmd != null;        
    }
}
