/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.persistence.service.hibernate.fat.consumer;

import java.io.Writer;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Map;

import javax.sql.DataSource;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.feature.ServerStarted;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.persistence.PersistenceService;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;
import com.ibm.wsspi.persistence.PersistenceServiceUnitConfig;
import com.ibm.wsspi.resource.ResourceConfig;
import com.ibm.wsspi.resource.ResourceConfigFactory;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

import io.openliberty.persistence.service.hibernate.fat.consumer.internal.model.Person;
import io.openliberty.persistence.service.hibernate.fat.persistence.test.Consumer;

/**
 * OSGi DS component that wires up the Hibernate-backed PersistenceService for the FAT test.
 * Registered as a ResourceFactory so the test servlet can look it up via JNDI.
 *
 * This exercises the full persistenceService-3.0 feature chain:
 *   server.xml <hibernateConsumer> config element
 *     -> activates this component
 *     -> injects HibernatePersistenceServiceImpl (from io.openliberty.persistenceService-3.0)
 *     -> creates a PersistenceServiceUnit backed by Hibernate 8.x / JPA 4.0
 */
@Component(name = "io.openliberty.persistence.service.hibernate.consumer",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = {
               "config.displayId=hibernateConsumerService",
               "jndiName=hibernateConsumerFactory",
               "creates.objectClass=io.openliberty.persistence.service.hibernate.fat.persistence.test.Consumer"
           },
           immediate = true)
public class ConsumerService implements ResourceFactory {

    private static final String PERSISTENCE_SERVICE = "persistenceService";
    private static final TraceComponent tc = Tr.register(ConsumerService.class);

    private final AtomicServiceReference<PersistenceService> ps =
        new AtomicServiceReference<>(PERSISTENCE_SERVICE);

    private ResourceFactory jtaDataSourceFactory;
    private ResourceFactory nonJtaDataSourceFactory;
    private ResourceConfigFactory resourceConfigFactory;

    private PersistenceServiceUnit pu;
    private PersistenceServiceUnitConfig conf;

    @Activate
    public void activate(ComponentContext cc, Map<String, Object> properties) throws Exception {
        ps.activate(cc);

        boolean createTables = Boolean.TRUE.equals(properties.get("createTables"));

        ResourceConfig resourceConfig = resourceConfigFactory.createResourceConfig(DataSource.class.getName());
        resourceConfig.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
        resourceConfig.setSharingScope(ResourceConfig.SHARING_SCOPE_UNSHAREABLE);

        DataSource jta    = (DataSource) jtaDataSourceFactory.createResource(resourceConfig);
        DataSource nonJta = (DataSource) nonJtaDataSourceFactory.createResource(resourceConfig);

        conf = new PersistenceServiceUnitConfig();
        conf.setJtaDataSource(jta);
        conf.setNonJtaDataSource(nonJta);
        conf.setClasses(Arrays.asList(Person.class.getName()));
        conf.setConsumerLoader(ConsumerService.class.getClassLoader());
        conf.setCreateOrUpdateTables(createTables);

        pu = ps.getService().createPersistenceServiceUnit(conf);
    }

    @Deactivate
    public void deactivate(ComponentContext cc) {
        pu.close();
        ps.deactivate(cc);
    }

    @Override
    public Object createResource(ResourceInfo info) throws Exception {
        return new ConsumerImpl(pu, this);
    }

    @Reference(target = "(id=consumerJtaDs)")
    protected void setDataSourceFactory(ResourceFactory svc) { jtaDataSourceFactory = svc; }
    protected void unsetDataSourceFactory(ResourceFactory svc) {}

    @Reference(target = "(id=consumerNonJtaDs)")
    protected void setNonJtaDataSourceFactory(ResourceFactory svc) { nonJtaDataSourceFactory = svc; }
    protected void unsetNonJtaDataSourceFactory(ResourceFactory svc) {}

    @Reference(service = ResourceConfigFactory.class)
    protected void setResourceConfigFactory(ResourceConfigFactory svc) { resourceConfigFactory = svc; }
    protected void unsetResourceConfigFactory(ResourceConfigFactory svc) {}

    @Reference(name = PERSISTENCE_SERVICE, service = PersistenceService.class)
    protected void setPersistenceService(ServiceReference<PersistenceService> ref) { ps.setReference(ref); }
    protected void unsetPersistenceService(ServiceReference<PersistenceService> ref) { ps.unsetReference(ref); }

    @Reference
    protected synchronized void setServerStarted(ServerStarted serverStarted) {}
    protected synchronized void unsetServerStarted(ServerStarted serverStarted) {}
}
