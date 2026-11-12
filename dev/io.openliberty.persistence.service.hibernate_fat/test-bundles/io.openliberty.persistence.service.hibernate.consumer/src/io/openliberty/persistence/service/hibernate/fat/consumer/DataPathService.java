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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.kernel.feature.ServerStarted;
import com.ibm.wsspi.persistence.DatabaseStore;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

import io.openliberty.persistence.service.hibernate.fat.consumer.internal.model.AnnotatedItem;
import io.openliberty.persistence.service.hibernate.fat.consumer.internal.model.UnannotatedItem;

/**
 * Behaves like the Jakarta Data runtime (DBStoreEHFactory) toward the persistence
 * service: it looks up a {@link DatabaseStore} by id and calls
 * createPersistenceServiceUnit with the internal properties
 * <ul>
 * <li>io.openliberty.persistence.internal.entityClassInfo (ORM XML for classes that
 * have no Jakarta Persistence annotations),</li>
 * <li>io.openliberty.persistence.internal.tableNames (table names for annotated
 * entities), and</li>
 * <li>io.openliberty.persistence.internal.generatedEntities (in-memory resources for
 * entity classes generated from records).</li>
 * </ul>
 * These are the same keys that the EclipseLink implementation of DatabaseStore reads,
 * so this component checks that the Hibernate implementation honors them identically.
 */
@Component(name = "io.openliberty.persistence.service.hibernate.datapath",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = {
                        "config.displayId=hibernateDataPathService",
                        "jndiName=hibernateDataPathFactory",
                        "creates.objectClass=io.openliberty.persistence.service.hibernate.fat.persistence.test.DataPathConsumer"
           },
           immediate = true)
public class DataPathService implements ResourceFactory {

    private static final String EOLN = System.lineSeparator();

    static final String ANNOTATED_TABLE = "DataAnnotated";
    static final String UNANNOTATED_TABLE = "UnannotatedItem";

    private DatabaseStore databaseStore;
    private PersistenceServiceUnit pu;

    @Activate
    public void activate(ComponentContext cc, Map<String, Object> properties) throws Exception {
        pu = createUnit(null);
    }

    @Deactivate
    public void deactivate(ComponentContext cc) {
        if (pu != null)
            pu.close();
    }

    @Override
    public Object createResource(ResourceInfo info) throws Exception {
        return new DataPathConsumerImpl(pu, this);
    }

    /**
     * Creates a unit the way DBStoreEHFactory does.
     *
     * @param extra additional properties, such as generatedEntities. Can be null.
     */
    PersistenceServiceUnit createUnit(Map<String, Object> extra) throws Exception {
        String prefix = databaseStore.getTablePrefix();

        // Annotated entity: only the class name and its table name are passed.
        LinkedHashSet<String> tableNames = new LinkedHashSet<>();
        tableNames.add(ANNOTATED_TABLE);

        // Unannotated entity: the mapping is passed as an ORM XML fragment.
        // This is the form that EntityParser/View produce.
        String entityXml = " <entity class=\"" + UnannotatedItem.class.getName() + "\">" + EOLN +
                           "  <table name=\"" + prefix + UNANNOTATED_TABLE + "\"/>" + EOLN +
                           "  <attributes>" + EOLN +
                           "   <id name=\"id\" access=\"FIELD\">" + EOLN +
                           "    <column nullable=\"false\"/>" + EOLN +
                           "   </id>" + EOLN +
                           "   <basic name=\"name\" access=\"FIELD\">" + EOLN +
                           "   </basic>" + EOLN +
                           "   <basic name=\"active\" access=\"FIELD\">" + EOLN +
                           "    <column nullable=\"false\"/>" + EOLN +
                           "   </basic>" + EOLN +
                           "   <element-collection name=\"tags\" access=\"FIELD\" fetch=\"EAGER\">" + EOLN +
                           "   </element-collection>" + EOLN +
                           "  </attributes>" + EOLN +
                           " </entity>" + EOLN;

        Map<String, Object> props = new HashMap<>();
        props.put("io.openliberty.persistence.internal.entityClassInfo", new String[] { entityXml });
        props.put("io.openliberty.persistence.internal.tableNames", tableNames);
        if (extra != null)
            props.putAll(extra);

        return databaseStore.createPersistenceServiceUnit(DataPathService.class.getClassLoader(),
                                                          props,
                                                          AnnotatedItem.class.getName());
    }

    String getTablePrefix() {
        return databaseStore.getTablePrefix();
    }

    @Reference(target = "(id=dataPathStore)")
    protected void setDatabaseStore(DatabaseStore svc) {
        databaseStore = svc;
    }

    protected void unsetDatabaseStore(DatabaseStore svc) {}

    @Reference
    protected synchronized void setServerStarted(ServerStarted serverStarted) {}

    protected synchronized void unsetServerStarted(ServerStarted serverStarted) {}
}
