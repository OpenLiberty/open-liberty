/*
 *
 * Copyright 2012,2013 International Business Machines Corp.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.jbatch.container.servicesmanager;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.exception.PersistenceException;
import com.ibm.jbatch.container.impl.BatchConfigImpl;
import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.servicesmanager.ServiceTypes.Name;
import com.ibm.jbatch.container.util.BatchContainerConstants;
import com.ibm.jbatch.container.ws.BatchDispatcher;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.jbatch.container.ws.smf.ZosJBatchSMFLogging;
import com.ibm.jbatch.spi.BatchSPIManager;
import com.ibm.jbatch.spi.DatabaseConfigurationBean;
import com.ibm.jbatch.spi.services.IBatchArtifactFactory;
import com.ibm.jbatch.spi.services.IBatchServiceBase;
import com.ibm.jbatch.spi.services.IBatchThreadPoolService;
import com.ibm.jbatch.spi.services.IJobXMLLoaderService;
import com.ibm.jbatch.spi.services.ITransactionManagementService;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class ServicesManagerImpl implements BatchContainerConstants, ServicesManager {

    private final static String sourceClass = ServicesManagerImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    // Declared 'volatile' to allow use in double-checked locking.  This 'isInited'
    // refers to whether the configuration has been hardened and possibly the
    // first service impl loaded, not whether the instance has merely been instantiated.
    private final byte[] isInitedLock = new byte[0];
    private volatile Boolean isInited = Boolean.FALSE;

    private DatabaseConfigurationBean databaseConfigBean = null;
    private BatchConfigImpl batchRuntimeConfig;
    private Properties batchContainerProps = null;

    private final Map<Name, String> serviceImplClassNames = ServiceTypes.getServiceImplClassNames();
    private final Map<String, Name> propertyNameTable = ServiceTypes.getServicePropertyNames();

    // Registry of all current services
    private final ConcurrentHashMap<Name, IBatchServiceBase> serviceRegistry = new ConcurrentHashMap<Name, IBatchServiceBase>();

    /**
     * Init doesn't actually load the service impls, which are still loaded lazily. What it does is it
     * hardens the config. This is necessary since the batch runtime by and large is not dynamically
     * configurable, (e.g. via MBeans). Things like the database config used by the batch runtime's
     * persistent store are hardened then, as are the names of the service impls to use.
     */
    private void initIfNecessary() {
        if (logger.isLoggable(Level.FINER)) {
            logger.config("In initIfNecessary().");
        }
        // Use double-checked locking with volatile.
        if (!isInited) {
            synchronized (isInitedLock) {
                if (!isInited) {
                    logger.config("--- Initializing ServicesManagerImpl ---");
                    batchRuntimeConfig = new BatchConfigImpl();

                    initFromPropertiesFiles();
                    initServiceImplOverrides();
                    initDatabaseConfig();
                    initOtherConfig();
                    isInited = Boolean.TRUE;

                    logger.config("--- Completed initialization of ServicesManagerImpl ---");
                }
            }
        }

        logger.config("Exiting initIfNecessary()");
    }

    private void initFromPropertiesFiles() {

        Properties serviceIntegratorProps = new Properties();
        InputStream batchServicesListInputStream = this.getClass().getResourceAsStream("/META-INF/services/" + BATCH_INTEGRATOR_CONFIG_FILE);

        if (batchServicesListInputStream != null) {
            try {
                logger.config("Batch Integrator Config File exists! loading it..");
                serviceIntegratorProps.load(batchServicesListInputStream);
                batchServicesListInputStream.close();
            } catch (IOException e) {
                logger.config("Error loading " + "/META-INF/services/" + BATCH_INTEGRATOR_CONFIG_FILE + " IOException=" + e.toString());
            } catch (Exception e) {
                logger.config("Error loading " + "/META-INF/services/" + BATCH_INTEGRATOR_CONFIG_FILE + " Exception=" + e.toString());
            }
        } else {
            logger.config("Could not find batch integrator config file: " + "/META-INF/services/" + BATCH_INTEGRATOR_CONFIG_FILE);
        }

        // See if any do not map to service impls.

        Set<String> removeThese = new HashSet<String>();
        for (Object key : serviceIntegratorProps.keySet()) {
            String keyStr = (String) key;
            if (!propertyNameTable.containsKey(keyStr)) {
                logger.fine("Found property named: " + keyStr
                            + " with value: " + serviceIntegratorProps.get(keyStr)
                            + " in " + BATCH_INTEGRATOR_CONFIG_FILE + " , but did not find a corresponding service type "
                            + "in the internal table of service types.\n Ignoring this property then.   Maybe this should have been set in batch-config.properties instead.");
                removeThese.add(keyStr);
            }
        }
        for (String s : removeThese) {
            serviceIntegratorProps.remove(s);
        }

        Properties adminProps = new Properties();
        InputStream batchAdminConfigListInputStream = this.getClass().getResourceAsStream("/META-INF/services/" + BATCH_ADMIN_CONFIG_FILE);

        if (batchServicesListInputStream != null) {
            try {
                logger.config("Batch Admin Config File exists! loading it..");
                adminProps.load(batchAdminConfigListInputStream);
                batchAdminConfigListInputStream.close();
            } catch (IOException e) {
                logger.config("Error loading " + "/META-INF/services/" + BATCH_ADMIN_CONFIG_FILE + " IOException=" + e.toString());
            } catch (Exception e) {
                logger.config("Error loading " + "/META-INF/services/" + BATCH_ADMIN_CONFIG_FILE + " Exception=" + e.toString());
            }
        } else {
            logger.config("Could not find batch admin config file: " + "/META-INF/services/" + BATCH_ADMIN_CONFIG_FILE);
        }

        // See if any DO map to service impls, which would be a mistake
        Set<String> removeTheseToo = new HashSet<String>();
        for (Object key : adminProps.keySet()) {
            String keyStr = (String) key;
            if (propertyNameTable.containsKey(keyStr)) {
                logger.fine("Found property named: " + keyStr + " with value: " + adminProps.get(keyStr) + " in "
                            + BATCH_ADMIN_CONFIG_FILE + " , but this is a batch runtime service configuration.\n"
                            + "Ignoring this property then, since this should have been set in batch-services.properties instead.");
                removeThese.add(keyStr);
            }
        }
        for (String s : removeTheseToo) {
            adminProps.remove(s);
        }

        // Merge the two into 'batchContainerProps'
        batchContainerProps = new Properties();
        batchContainerProps.putAll(adminProps);
        batchContainerProps.putAll(serviceIntegratorProps);

        logger.fine("Dumping contents of batchContainerProps after reading properties files.");
        for (Object key : batchContainerProps.keySet()) {
            logger.config("key = " + key);
            logger.config("value = " + batchContainerProps.get(key));
        }

        // Set this on the config.
        //
        // WARNING:  This sets us up for collisions since this is just a single holder of properties
        // potentially used by any service impl.
        batchRuntimeConfig.setConfigProperties(batchContainerProps);
    }

    private void initServiceImplOverrides() {

        // For each property we care about (i.e that defines one of our service impls)
        for (String propKey : propertyNameTable.keySet()) {
            // If the property is defined
            String value = batchContainerProps.getProperty(propKey);
            if (value != null) {
                // Get the corresponding serviceType enum and store the value of
                // the key/value property pair in the table where we store the service impl classnames.
                Name serviceType = propertyNameTable.get(propKey);
                String defaultServiceImplClassName = serviceImplClassNames.get(serviceType); // For logging.
                serviceImplClassNames.put(serviceType, value.trim());
                logger.config("Overriding serviceType: " + serviceType + ", replacing default impl classname: " +
                              defaultServiceImplClassName + " with override impl class name: " + value.trim());
            }
        }
    }

    private void initDatabaseConfig() {
        if (databaseConfigBean == null) {
            logger.config("First try to load 'suggested config' from BatchSPIManager");
            databaseConfigBean = BatchSPIManager.getInstance().getFinalDatabaseConfiguration();
            if (databaseConfigBean == null) {
                logger.fine("Loading database config from configuration properties file.");
                // Initialize database-related properties
                databaseConfigBean = new DatabaseConfigurationBean();
                databaseConfigBean.setJndiName(batchContainerProps.getProperty(JNDI_NAME, DEFAULT_JDBC_JNDI_NAME));
                databaseConfigBean.setJdbcDriver(batchContainerProps.getProperty(JDBC_DRIVER, DEFAULT_JDBC_DRIVER));
                databaseConfigBean.setJdbcUrl(batchContainerProps.getProperty(JDBC_URL, DEFAULT_JDBC_URL));
                databaseConfigBean.setDbUser(batchContainerProps.getProperty(DB_USER));
                databaseConfigBean.setDbPassword(batchContainerProps.getProperty(DB_PASSWORD));
                databaseConfigBean.setSchema(batchContainerProps.getProperty(DB_SCHEMA, DEFAULT_DB_SCHEMA));
            }
        } else {
            // Currently we do not expected this path to be used by Glassfish
            logger.config("Database config has been set directly from SPI, do NOT load from properties file.");
        }
        // In either case, set this bean on the main config bean
        batchRuntimeConfig.setDatabaseConfigurationBean(databaseConfigBean);
    }

    private void initOtherConfig() {
        String seMode = serviceImplClassNames.get(Name.JAVA_EDITION_IS_SE_DUMMY_SERVICE);
        if (seMode.equalsIgnoreCase("true")) {
            batchRuntimeConfig.setJ2seMode(true);
        }
    }

    // Look up registry and return requested service if exist
    // If not exist, create a new one, add to registry and return that one
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.jbatch.container.config.ServicesManager#getService(com.ibm.jbatch.container.config.ServicesManagerImpl.ServiceType)
     */
    private IBatchServiceBase getService(Name serviceType) throws BatchContainerServiceException {
        String sourceMethod = "getService";
        if (logger.isLoggable(Level.FINE))
            logger.entering(sourceClass, sourceMethod + ", serviceType=" + serviceType);

        initIfNecessary();

        IBatchServiceBase service = new ServiceLoader(serviceType).getService();

        if (logger.isLoggable(Level.FINE))
            logger.exiting(sourceClass, sourceMethod);

        return service;
    }

    /*
     * public enum Name {
     * JAVA_EDITION_IS_SE_DUMMY_SERVICE,
     * TRANSACTION_SERVICE,
     * PERSISTENCE_MANAGEMENT_SERVICE,
     * JOB_STATUS_MANAGEMENT_SERVICE,
     * BATCH_THREADPOOL_SERVICE,
     * BATCH_KERNEL_SERVICE,
     * JOB_ID_MANAGEMENT_SERVICE,
     * CALLBACK_SERVICE,
     * JOBXML_LOADER_SERVICE, // Preferred
     * DELEGATING_JOBXML_LOADER_SERVICE, // Delegating wrapper
     * CONTAINER_ARTIFACT_FACTORY_SERVICE, // Preferred
     * DELEGATING_ARTIFACT_FACTORY_SERVICE // Delegating wrapper
     */

    /**
     * OSGI deactivation.
     */
    @Deactivate
    protected void deactivate() {
        getBatchKernelService().shutdown();
    }

    private ITransactionManagementService transactionManagementService;
    private ZosJBatchSMFLogging jbatchSMF;

    //@Reference(name = "jbatchSMF", service = com.ibm.jbatch.container.smflogging.ZosJBatchSMFLogging.class)
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setZosJBatchSMFLogging(com.ibm.jbatch.container.ws.smf.ZosJBatchSMFLogging jbatchSMF) {
        this.jbatchSMF = jbatchSMF;
    }

    protected void unsetZosJBatchSMFLogging(com.ibm.jbatch.container.ws.smf.ZosJBatchSMFLogging jbatchSMF) {
        if (this.jbatchSMF == jbatchSMF) {
            this.jbatchSMF = null;
        }
    }

    @Override
    public ZosJBatchSMFLogging getJBatchSMFService() {
        // TODO Auto-generated method stub
        return jbatchSMF;
    }

    @Reference
    protected void setITransactionManagementService(ITransactionManagementService ref) {
        transactionManagementService = ref;
    }

    @Override
    public ITransactionManagementService getTransactionManagementService() {
        return transactionManagementService;
    }

    private IPersistenceManagerService persistenceService;

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    protected void setIPersistenceManagerService(IPersistenceManagerService ref, Map<String, Object> props) {
        persistenceService = ref;
    }

    @Override
    public IPersistenceManagerService getPersistenceManagerService() {
        return persistenceService;
    }

    private IBatchThreadPoolService batchThreadPoolService;

    @Reference
    protected void setIBatchThreadPoolService(IBatchThreadPoolService batchThreadPoolService) {
        this.batchThreadPoolService = batchThreadPoolService;
    }

    @Override
    public IBatchThreadPoolService getThreadPoolService() {
        return this.batchThreadPoolService;
    }

    private IBatchKernelService batchKernelService;

    @Reference
    protected void setIBatchKernelService(IBatchKernelService ref) {
        this.batchKernelService = ref;
    }

    @Override
    public IBatchKernelService getBatchKernelService() {
        return batchKernelService;
    }

    @Override
    public IJobXMLLoaderService getPreferredJobXMLLoaderService() {
        return jobXMLLoaderService;
    }

    private IJobXMLLoaderService jobXMLLoaderService;

    @Reference
    protected void setIJobXMLLoaderService(IJobXMLLoaderService ref) {
        this.jobXMLLoaderService = ref;
    }

    @Override
    public IJobXMLLoaderService getDelegatingJobXMLLoaderService() {
        return jobXMLLoaderService;
    }

    @Override
    public IBatchArtifactFactory getPreferredArtifactFactory() {
        return (IBatchArtifactFactory) getService(Name.CONTAINER_ARTIFACT_FACTORY_SERVICE);
    }

    private IBatchArtifactFactory batchArtifactFactory;

    @Reference
    protected void setIBatchArtifactFactory(IBatchArtifactFactory ref) {
        this.batchArtifactFactory = ref;
    }

    @Override
    public IBatchArtifactFactory getDelegatingArtifactFactory() {
        return batchArtifactFactory;
    }

    private class ServiceLoader {

        volatile IBatchServiceBase service = null;
        private Name serviceType = null;

        private ServiceLoader(Name name) {
            this.serviceType = name;
        }

        private IBatchServiceBase getService() {
            service = serviceRegistry.get(serviceType);
            if (service == null) {
                // Probably don't want to be loading two on two different threads so lock the whole table.
                synchronized (serviceRegistry) {
                    if (service == null) {
                        service = _loadServiceHelper(serviceType);
                        service.init(batchRuntimeConfig);
                        serviceRegistry.putIfAbsent(serviceType, service);
                    }
                }
            }
            return service;
        }

        /**
         * Try to load the IGridContainerService given by the className. If it fails
         * to load, default to the defaultClass. If the default fails to load, then
         * blow out of here with a RuntimeException.
         */
        private IBatchServiceBase _loadServiceHelper(Name serviceType) {
            IBatchServiceBase service = null;

            String className = serviceImplClassNames.get(serviceType);
            try {
                if (className != null)
                    service = _loadService(className);
            } catch (PersistenceException pe) {
                // Don't rewrap to make it a bit clearer
                throw pe;
            } catch (Throwable e) {
                throw new RuntimeException("Could not instantiate service " + className, e);
            }

            if (service == null) {
                throw new RuntimeException("Instantiate of service=: " + className + " returned null. Aborting...");
            }

            return service;
        }

        private IBatchServiceBase _loadService(String className) throws Exception {

            IBatchServiceBase service = null;

            Class cls = Class.forName(className);

            if (cls != null) {
                Constructor ctor = cls.getConstructor();
                if (ctor != null) {
                    service = (IBatchServiceBase) ctor.newInstance();
                } else {
                    throw new Exception("Service class " + className + " should  have a default constructor defined");
                }
            } else {
                throw new Exception("Exception loading Service class " + className + " make sure it exists");
            }

            return service;
        }
    }

    /**
     * Use to publish JMS events
     */
    private BatchEventsPublisher eventsPublisher = null;

    /**
     * DS injection
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setEventsPublisher(BatchEventsPublisher publisher) {
        eventsPublisher = publisher;
    }

    protected void unsetEventsPublisher(BatchEventsPublisher publisher) {
        if (this.eventsPublisher == publisher) {
            eventsPublisher = publisher;
        }
    }

    @Override
    public BatchEventsPublisher getBatchEventsPublisher() {
        return eventsPublisher;
    }

    /**
     * Used by PartitionedStepControllerImpl to send remote "startPartition" messages
     * over JMS.
     */
    private BatchDispatcher batchJmsDispatcher;

    /**
     * DS injection
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, target = "(type=JMS)")
    protected void setBatchJmsDispatcher(BatchDispatcher ref) {
        this.batchJmsDispatcher = ref;
    }

    /**
     * DS un-inject.
     */
    protected void unsetBatchJmsDispatcher(BatchDispatcher ref) {
        if (this.batchJmsDispatcher == ref) {
            this.batchJmsDispatcher = null;
        }
    }

    /**
     * @return the BatchJmsDispatcher
     */
    @Override
    public BatchDispatcher getBatchJmsDispatcher() {
        return batchJmsDispatcher;
    }

}
