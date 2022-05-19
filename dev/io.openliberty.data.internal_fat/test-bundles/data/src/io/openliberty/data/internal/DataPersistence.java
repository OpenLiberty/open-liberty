/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.persistence.DatabaseStore;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;

// TODO ought to be used as OSGi service component, not static!
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = DataPersistence.class)
public class DataPersistence {
    private static final ConcurrentHashMap<SimpleImmutableEntry<String, ClassLoader>, PersistenceServiceUnit> units = new ConcurrentHashMap<>();

    @Reference
    protected LocalTransactionCurrent localTranCurrent;

    @Reference
    protected EmbeddableWebSphereTransactionManager tranMgr;

    @Activate
    protected void activate(ComponentContext context) {
        System.out.println("Activate");
    }

    /**
     * Define entity classes.
     *
     * @param dbStoreId id of databaseStore config element
     * @param loader    class loader
     * @param classList entity classes
     * @throws Exception
     */
    public void defineEntities(String dbStoreId, ClassLoader loader, List<Class<?>> classList) throws Exception {
        BundleContext bc = FrameworkUtil.getBundle(DataPersistence.class).getBundleContext();
        Collection<ServiceReference<DatabaseStore>> refs = bc.getServiceReferences(DatabaseStore.class,
                                                                                   FilterUtils.createPropertyFilter("id", dbStoreId));
        if (refs.isEmpty())
            throw new IllegalArgumentException("Not found: " + dbStoreId);

        DatabaseStore dbstore = bc.getService(refs.iterator().next());

        String[] classNames = new String[classList.size()];
        int i = 0;
        for (Class<?> c : classList)
            classNames[i++] = c.getName();

        PersistenceServiceUnit punit = dbstore.createPersistenceServiceUnit(loader, new HashMap<>(), classNames);
        units.put(new SimpleImmutableEntry<>(dbStoreId, loader), punit);
    }

    PersistenceServiceUnit getPersistenceUnit(String dbStoreId, ClassLoader loader) {
        System.out.println("Available persistence service units: " + units);
        SimpleImmutableEntry<String, ClassLoader> key = new SimpleImmutableEntry<>(dbStoreId, loader);
        PersistenceServiceUnit unit = units.get(key);
        System.out.println("Found " + unit + " using key ");
        return unit;
    }
}