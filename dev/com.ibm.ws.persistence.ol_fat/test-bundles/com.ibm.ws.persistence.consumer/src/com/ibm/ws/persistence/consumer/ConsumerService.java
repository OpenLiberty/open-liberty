/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.persistence.consumer;

import java.io.File;
import java.io.FileInputStream;
import java.io.Writer;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.persistence.consumer.internal.model.Car;
import com.ibm.ws.persistence.consumer.internal.model.Person;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.persistence.DDLGenerationParticipant;
import com.ibm.wsspi.persistence.InMemoryMappingFile;
import com.ibm.wsspi.persistence.PersistenceService;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;
import com.ibm.wsspi.persistence.PersistenceServiceUnitConfig;
import com.ibm.wsspi.resource.ResourceConfig;
import com.ibm.wsspi.resource.ResourceConfigFactory;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

@Component(name = "consumer",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = { "config.displayId=consumerService", "jndiName=consumerFactory", "creates.objectClass=com.ibm.websphere.persistence.test.Consumer" },
           immediate = true)
public class ConsumerService implements ResourceFactory, DDLGenerationParticipant {
    private static final String PERSISTENCE_SERVICE = "persistenceService";

    private final AtomicServiceReference<PersistenceService> ps = new AtomicServiceReference<PersistenceService>(PERSISTENCE_SERVICE);

    private ResourceFactory jtaDataSourceFactory;
    private ResourceFactory nonJtaDataSourceFactory;
    private ResourceFactory privDataSourceFactory;
    private ResourceConfigFactory resourceConfigFactory;

    private PersistenceServiceUnit pu;

    private PersistenceServiceUnitConfig conf;

    @Activate
    public void activate(ComponentContext cc, Map<String, Object> properties) throws Exception {
        ps.activate(cc);

        String inmemMappingFileName = (String) properties.get("inmemMappingFile");
        String staticMappingFileName = (String) properties.get("staticMappingFile");
        boolean createTables = Boolean.parseBoolean((String) properties.get("createTables"));

        ResourceConfig resourceConfig = resourceConfigFactory.createResourceConfig(DataSource.class.getName());
        resourceConfig.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
        resourceConfig.setSharingScope(ResourceConfig.SHARING_SCOPE_UNSHAREABLE);

        DataSource jta = (DataSource) jtaDataSourceFactory.createResource(resourceConfig);
        DataSource nonJta = (DataSource) nonJtaDataSourceFactory.createResource(resourceConfig);
        DataSource priv = (DataSource) privDataSourceFactory.createResource(resourceConfig);

        conf = new PersistenceServiceUnitConfig();
        conf.setJtaDataSource(jta);
        conf.setNonJtaDataSource(nonJta);
        conf.setPrivilegedDataSource(priv);
        conf.setClasses(Arrays.asList(new String[] { Person.class.getName(), Car.class.getName() }));

        conf.setConsumerLoader(ConsumerService.class.getClassLoader());
        String serverHome = System.getProperty("WLP_USER_DIR");
        if ((staticMappingFileName != null) && (staticMappingFileName.length() > 0)) {
            File staticMappingFile = new File(serverHome, staticMappingFileName);
            if (staticMappingFile.exists()) {
                conf.setMappingFileNames(Arrays.asList(new String[] { staticMappingFile.getAbsolutePath() }));
            }
        }

        // Read a file from the fs into a byte[] to 'fake out' generation
        if ((inmemMappingFileName != null) && (inmemMappingFileName.length() > 0)) {
            File inmemMappingFile = new File(serverHome, inmemMappingFileName);
            if (inmemMappingFile.exists()) {
                byte[] bytes = IOUtils.toByteArray(new FileInputStream(inmemMappingFile));

                InMemoryMappingFile immf = new InMemoryMappingFile(bytes);
                conf.setInMemoryMappingFiles(Arrays.asList(new InMemoryMappingFile[] { immf }));
            }
        }

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("eclipselink.logging.parameters", "true");

        String databaseVendor = (String) properties.get("dbVendor");
        String databaseMajorVersion = (String) properties.get("dbMajorVersion");
        String databaseMinorVersion = (String) properties.get("dbMinorVersion");

        if ((databaseVendor != null) && (databaseMajorVersion != null) && (databaseMinorVersion != null)) {
            props.put("javax.persistence.database-product-name", databaseVendor);
            props.put("javax.persistence.database-major-version", databaseMajorVersion);
            props.put("javax.persistence.database-minor-version", databaseMinorVersion);
        }

        conf.setProperties(props);
        conf.setCreateOrUpdateTables(createTables);

        pu = ps.getService().createPersistenceServiceUnit(conf);
    }

    @Override
    public Object createResource(ResourceInfo info) throws Exception {
        return new ConsumerImpl(pu, this);
    }

    @Reference(target = "(id=consumerJtaDs)")
    protected void setDataSourceFactory(ResourceFactory svc) {
        jtaDataSourceFactory = svc;
    }

    @Reference(target = "(id=consumerdsNonTxDs)")
    protected void setNonJtaDataSourceFactory(ResourceFactory svc) {
        nonJtaDataSourceFactory = svc;
    }

    @Reference(target = "(id=privDs)")
    protected void setPrivDataSourceFactory(ResourceFactory svc) {
        privDataSourceFactory = svc;
    }

    @Reference(name = "resourceConfigFactory", service = ResourceConfigFactory.class)
    protected void setResourceConfigFactory(ResourceConfigFactory svc) {
        resourceConfigFactory = svc;
    }

    protected void unsetJtaDataSourceFactory(ResourceFactory svc) {
        if (svc == jtaDataSourceFactory) {
            jtaDataSourceFactory = null;
        }
    }

    protected void unsetNonJtaDataSourceFactory(ResourceFactory svc) {
        if (svc == nonJtaDataSourceFactory) {
            nonJtaDataSourceFactory = null;
        }
    }

    protected void unsetResourceConfigFactory(ResourceConfigFactory svc) {
        if (svc == resourceConfigFactory) {
            resourceConfigFactory = null;
        }
    }

    protected void updatedDataSourceFactory(ResourceFactory svc) {
        // Some sort of update has been made to the resource factory.
        // It could be something minor, like switching some timeout value,
        // or maybe it now points to an entirely different database on another machine.
        // TODO -- could test in the future?
    }

    @Deactivate
    public void deactivate(ComponentContext cc) {
        pu.close();
        ps.deactivate(cc);
    }

    @Reference(name = PERSISTENCE_SERVICE, service = PersistenceService.class)
    protected void setPersistenceService(ServiceReference<PersistenceService> reference) {
        ps.setReference(reference);
    }

    protected void unsetPersistenceService(ServiceReference<PersistenceService> reference) {
        ps.unsetReference(reference);
    }

    @Override
    public String getDDLFileName() {
        return "consumerService";
    }

    @Override
    public void generate(Writer out) {
        pu.generateDDL(out);
    }

    public PersistenceServiceUnitConfig getPersistenceServiceUnitConfig(boolean copy) {
        if (copy) {
            return new PersistenceServiceUnitConfig(conf);
        } else {
            return conf;
        }
    }

    public PersistenceServiceUnit createPersistenceServiceUnit(PersistenceServiceUnitConfig conf) {
        return ps.getService().createPersistenceServiceUnit(conf);
    }

}
