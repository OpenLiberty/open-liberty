/*******************************************************************************
 * Copyright (c) 2012,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mongo.internal;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.mongo.MongoChangeListener;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 * Provides the {@link ResourceFactory} implementation for the <mongoDB> configuration element
 * in server.xml; made available as a declarative services component.
 */
@Component(name = "com.ibm.ws.mongo.mongoDB",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { ResourceFactory.class, ApplicationRecycleComponent.class },
           property = { "service.vendor=IBM", "creates.objectClass=com.mongodb.DB" })
public class MongoDBService implements ResourceFactory, ApplicationRecycleComponent, MongoChangeListener {
    private static final TraceComponent tc = Tr.register(MongoDBService.class);

    /**
     * Name of the property that indicates the database name.
     */
    private static final String DATABASE_NAME = "databaseName";

    /**
     * Name of the property that indicates the JNDI name.
     */
    private static final String JNDI_NAME = "jndiName";

    /**
     * Name of reference to the ApplicationRecycleCoordinator
     */
    private static final String APP_RECYCLE_SERVICE = "appRecycleService";

    /**
     * Reference to the shared library that contains the MongoDB Java driver.
     */
    private volatile MongoService mongo;

    /**
     * Service reference to this instance.
     */
    private final AtomicReference<ComponentContext> cctx = new AtomicReference<ComponentContext>();

    /**
     * Reference to the ApplicationRecycleCoordinator (if any) that is configured for this connection factory.
     */
    @Reference(name = APP_RECYCLE_SERVICE)
    private ApplicationRecycleCoordinator appRecycleSvcRef;

    /**
     * Names of applications using this ResourceFactory
     */
    private final Set<String> applications = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Activate the DeclarativeService instance that corresponds to an <mongoDB>
     * configuration element.
     *
     * @param context DeclarativeService defined/populated component context.
     * @param props the Component Properties from ComponentContext.getProperties.
     */
    @Activate
    protected void activate(ComponentContext context, Map<String, Object> props) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "MongoDBService activate: jndiName=" + props.get(JNDI_NAME) + ", database=" + props.get(DATABASE_NAME));

        // Register with the required MongoService to listen for changes and also
        // save the context to obtain the properties as needed. Since this service
        // has a modified method; the context properties may change.
        mongo.registerChangeListener(this);
        cctx.set(context);
    }

    @Override
    public Object createResource(ResourceInfo resourceInfo) throws Exception {
        ComponentContext context = cctx.get();

        String databaseName = (String) context.getProperties().get(DATABASE_NAME);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "MongoDBService createResource: database=" + databaseName + ", " + context);

        Object dbInstance = mongo.getDB(databaseName);

        ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cData != null)
            applications.add(cData.getJ2EEName().getApplication());

        return dbInstance;
    }

    /**
     * Deactivate the DeclarativeService instance that corresponds to an <mongoDB>
     * configuration element.
     *
     * @param context DeclarativeService defined/populated component context
     * @param props the Component Properties from ComponentContext.getProperties.
     */
    @Deactivate
    protected void deactivate(ComponentContext context, Map<String, Object> props) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "MongoDBService deactivate: jndiName=" + props.get(JNDI_NAME) + ", database=" + props.get(DATABASE_NAME));

        mongo.unregisterChangeListener(this);
        cctx.set(null);
    }

    @Reference(name = MongoService.MONGO, target = "(id=unbound)", policyOption = ReferencePolicyOption.GREEDY)
    protected void setMongoService(MongoService mongoService) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setMongoService: " + mongo + " -> " + mongoService);
        mongo = mongoService;
    }

    protected void unsetMongoService(MongoService mongoService) {
        if (mongoService == mongo) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "unsetMongoService: " + mongo + " -> " + null);
            mongo = null;
        }
    }

    @Trivial
    @Override
    public ApplicationRecycleContext getContext() {
        return null;
    }

    @Trivial
    @Override
    public Set<String> getDependentApplications() {
        Set<String> members = new HashSet<String>(applications);
        applications.removeAll(members);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getDependentApplications: " + members);
        return members;
    }

    /**
     * Modify the DeclarativeService instance that corresponds to an <mongoDB>
     * configuration element. <p>
     *
     * This method is only called if the <mogoDB> configuration element changes
     * and not the <mongo> configuration element. MongoDBService is deactivated
     * if <mongo> is changed, since the MongoService to deactivated.
     *
     * @param context DeclarativeService defined/populated component context
     * @param props the Component Properties from ComponentContext.getProperties.
     */
    @Modified
    protected void modified(ComponentContext context, Map<String, Object> props) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Dictionary<String, Object> oldProps = cctx.get().getProperties();
            Tr.debug(this, tc, "MongoDBService modified: jndiName: " + oldProps.get(JNDI_NAME) + " -> " + props.get(JNDI_NAME)
                               + ", database: " + oldProps.get(DATABASE_NAME) + " -> " + props.get(DATABASE_NAME));
        }

        cctx.set(context);

        // Assuming for now that all changes require an App recycle
        if (!applications.isEmpty()) {
            Set<String> members = new HashSet<String>(applications);
            applications.removeAll(members);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "recycling applications: " + members);
            appRecycleSvcRef.recycleApplications(members);
        }
    }

    @Override
    public void changeOccurred() {
        // Some change has occurred, all DB references are invalid so we need to restart any applications using them
        if (!applications.isEmpty()) {
            Set<String> members = new HashSet<String>(applications);
            applications.removeAll(members);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "recycling applications: " + members);
            appRecycleSvcRef.recycleApplications(members);
        }
    }
}