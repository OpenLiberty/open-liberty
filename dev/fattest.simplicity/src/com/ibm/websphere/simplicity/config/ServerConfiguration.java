/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.ibm.websphere.simplicity.config.wim.FederatedRepository;
import com.ibm.websphere.simplicity.config.wim.LdapFilters;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;

/**
 * Represents a server configuration document for the WAS 8.5 Liberty Profile.
 */
@XmlRootElement(name = "server")
public class ServerConfiguration implements Cloneable {

    private String description;
    @XmlElement(name = "featureManager")
    private FeatureManager featureManager;

    @XmlElement(name = "acmeCA")
    private AcmeCA acmeCA;

    @XmlElement(name = "activationSpec")
    private ConfigElementList<ActivationSpec> activationSpecs;

    @XmlElement(name = "adminObject")
    private ConfigElementList<AdminObject> adminObjects;

    @XmlElement(name = "basicRegistry")
    private ConfigElementList<BasicRegistry> basicRegistries;

    @XmlElement(name = "bell")
    private ConfigElementList<Bell> bells;

    @XmlElement(name = "httpEndpoint")
    private ConfigElementList<HttpEndpoint> httpEndpoints;

    @XmlElement(name = "virtualHost")
    private ConfigElementList<VirtualHost> virtualHosts;

    @XmlElement(name = "wasJmsEndpoint")
    private ConfigElementList<JmsEndpoint> wasJmsEndpoints;

    @XmlElement(name = "messagingEngine")
    private ConfigElementList<MessagingEngine> messagingEngines;

    @XmlElement(name = "httpSession")
    private HttpSession httpSession;

    @XmlElement(name = "httpSessionCache")
    private ConfigElementList<HttpSessionCache> httpSessionCaches;

    @XmlElement(name = "httpSessionDatabase")
    private HttpSessionDatabase httpSessionDatabase;

    @XmlElement(name = "application")
    private ConfigElementList<Application> applications;

    @XmlElement(name = "webApplication")
    private ConfigElementList<WebApplication> webApplications;

    @XmlElement(name = "springBootApplication")
    private ConfigElementList<SpringBootApplication> springBootApplications;

    @XmlElement(name = "cloudant")
    private ConfigElementList<Cloudant> cloudants;

    @XmlElement(name = "cloudantDatabase")
    private ConfigElementList<CloudantDatabase> cloudantDatabases;

    @XmlElement(name = "concurrencyPolicy")
    private ConfigElementList<ConcurrencyPolicy> concurrencyPolicies;

    @XmlElement(name = "connectionFactory")
    private ConfigElementList<ConnectionFactory> connectionFactories;

    @XmlElement(name = "contextService")
    private ConfigElementList<ContextService> contextServices;

    @XmlElement(name = "jdbcDriver")
    private ConfigElementList<JdbcDriver> jdbcDrivers;

    @XmlElement(name = "jmsActivationSpec")
    private ConfigElementList<JMSActivationSpec> jmsActivationSpecs;

    @XmlElement(name = "jmsConnectionFactory")
    private ConfigElementList<JMSConnectionFactory> jmsConnectionFactories;

    @XmlElement(name = "jmsDestination")
    private ConfigElementList<JMSDestination> jmsDestinations;

    @XmlElement(name = "jmsQueue")
    private ConfigElementList<JMSQueue> jmsQueues;

    @XmlElement(name = "jmsQueueConnectionFactory")
    private ConfigElementList<JMSQueueConnectionFactory> jmsQueueConnectionFactories;

    @XmlElement(name = "jmsTopic")
    private ConfigElementList<JMSTopic> jmsTopics;

    @XmlElement(name = "jmsTopicConnectionFactory")
    private ConfigElementList<JMSTopicConnectionFactory> jmsTopicConnectionFactories;

    @XmlElement(name = "jpa")
    private ConfigElementList<JPA> jpas;

    @XmlElement(name = "library")
    private ConfigElementList<Library> libraries;

    @XmlElement(name = "osgiLibrary")
    private ConfigElementList<OsgiLibraryElement> osgiLibraries;

    @XmlElement(name = "managedExecutorService")
    private ConfigElementList<ManagedExecutorService> managedExecutorServices;

    @XmlElement(name = "managedScheduledExecutorService")
    private ConfigElementList<ManagedScheduledExecutorService> managedScheduledExecutorServices;

    @XmlElement(name = "managedThreadFactory")
    private ConfigElementList<ManagedThreadFactory> managedThreadFactories;

    @XmlElement(name = "monitor")
    private ConfigElementList<Monitor> monitors;

    @XmlElement(name = "resourceAdapter")
    private ConfigElementList<ResourceAdapter> resourceAdapters;

    // TODO: will be moved to nested element of ResourceAdapter when we add support for config properties for embedded RARs
    @XmlElement(name = "com.ibm.ws.jca.resourceAdapter.properties.CalendarApp.CalendarRA")
    private ConfigElementList<JCAGeneratedProperties> properties_CalendarApp_CalendarRA;

    @XmlElement(name = "fileset")
    private ConfigElementList<Fileset> filesets;

    @XmlElement(name = "dataSource")
    private ConfigElementList<DataSource> dataSources;

    @XmlElement(name = "connectionManager")
    private ConfigElementList<ConnectionManager> connManagers;

    @XmlElement(name = "logging")
    private Logging logging;

    @XmlElement(name = "include")
    private ConfigElementList<IncludeElement> includeElements;

    @XmlElement(name = "applicationMonitor")
    private ApplicationMonitorElement applicationMonitor;

    @XmlElement(name = "executor")
    private ExecutorElement executor;

    @XmlElement(name = "config")
    private ConfigMonitorElement config;

    @XmlElement(name = "webContainer")
    private WebContainerElement webContainer;

    @XmlElement(name = "sslDefault")
    private SSLDefault sslDefault;

    @XmlElement(name = "ssl")
    private ConfigElementList<SSL> ssls;

    @XmlElement(name = "kerberos")
    private Kerberos kerberos;

    @XmlElement(name = "keyStore")
    private ConfigElementList<KeyStore> keyStores;

    @XmlElement(name = "jspEngine")
    private JspEngineElement jspEngine;

    @XmlElement(name = "authData")
    private ConfigElementList<AuthData> authDataElements;

    @XmlElement(name = "transaction")
    private Transaction transaction;

    @XmlElement(name = "jndiEntry")
    private ConfigElementList<JNDIEntry> jndiEntryElements;

    @XmlElement(name = "jndiURLEntry")
    private ConfigElementList<JNDIURLEntry> jndiURLEntryElements;

    @XmlElement(name = "variable")
    private ConfigElementList<Variable> variables;

    @XmlElement(name = "ejbContainer")
    private EJBContainerElement ejbContainer;

    @XmlElement(name = "couchdb")
    private ConfigElementList<CouchDBElement> couchDBs;

    @XmlElement(name = "mongo")
    private ConfigElementList<MongoElement> mongos;

    @XmlElement(name = "mongoDB")
    private ConfigElementList<MongoDBElement> mongoDBs;

    @XmlElement(name = "classloading")
    private ClassloadingElement classLoading;

    @XmlElement(name = "databaseStore")
    private ConfigElementList<DatabaseStore> databaseStores;

    @XmlElement(name = "persistentExecutor")
    private ConfigElementList<PersistentExecutor> persistentExecutors;

    @XmlElement(name = "scalingDefinitions")
    private ScalingDefinitions scalingDefinitions;

    @XmlElement(name = "remoteFileAccess")
    private ConfigElementList<RemoteFileAccess> remoteFileAccesses;

    @XmlElement(name = "apiDiscovery")
    private APIDiscoveryElement apiDiscoveryElement;

    @XmlElement(name = "mpMetrics")
    private MPMetricsElement mpMetricsElement;

    @XmlElement(name = "openapi")
    private OpenAPIElement openAPIElement;

    @XmlElement(name = "federatedRepository")
    private FederatedRepository federatedRepository;

    @XmlElement(name = "ldapRegistry")
    private ConfigElementList<LdapRegistry> ldapRegistries;

    @XmlElement(name = "activedLdapFilterProperties")
    private ConfigElementList<LdapFilters> activedLdapFilterProperties;

    @XmlElement(name = "orb")
    private ORB orb;

    @XmlAnyAttribute
    private Map<QName, Object> unknownAttributes;

    @XmlAnyElement
    private List<Element> unknownElements;

    @XmlElement(name = "samesite")
    private ConfigElementList<SameSite> samesites;

    @XmlElement(name = "javaPermission")
    private ConfigElementList<JavaPermission> javaPermissions;

    public ServerConfiguration() {
        this.description = "Generation date: " + new Date();
    }

    public ConfigElementList<ActivationSpec> getActivationSpecs() {
        if (this.activationSpecs == null)
            this.activationSpecs = new ConfigElementList<ActivationSpec>();
        return this.activationSpecs;
    }

    public ConfigElementList<AdminObject> getAdminObjects() {
        if (this.adminObjects == null)
            this.adminObjects = new ConfigElementList<AdminObject>();
        return this.adminObjects;
    }

    public boolean removeJdbcDriverById(String id) {
        if (jdbcDrivers == null)
            return false;

        for (JdbcDriver driver : jdbcDrivers)
            if (driver.getId().equals(id))
                return jdbcDrivers.remove(driver);
        return false;
    }

    public void addConnectionManager(ConnectionManager connManager) {
        if (connManagers == null)
            connManagers = new ConfigElementList<ConnectionManager>();
        connManagers.add(connManager);
    }

    public boolean removeConnectionManagerById(String id) {
        if (connManagers == null)
            return false;

        for (ConnectionManager connManager : connManagers)
            if (connManager.getId().equals(id))
                return connManagers.remove(connManager);
        return false;
    }

    public ConfigElementList<JNDIEntry> getJndiEntryElements() {
        if (this.jndiEntryElements == null) {
            this.jndiEntryElements = new ConfigElementList<JNDIEntry>();
        }
        return this.jndiEntryElements;
    }

    public ConfigElementList<JNDIURLEntry> getJndiURLEntryElements() {
        if (this.jndiURLEntryElements == null) {
            this.jndiURLEntryElements = new ConfigElementList<JNDIURLEntry>();
        }
        return this.jndiURLEntryElements;
    }

    public ConfigElementList<AuthData> getAuthDataElements() {
        if (this.authDataElements == null) {
            this.authDataElements = new ConfigElementList<AuthData>();
        }
        return this.authDataElements;
    }

    /**
     * Retrieves a description of this configuration.
     *
     * @return a description of this configuration
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Sets the description of this configuration
     *
     * @param description the description of this configuration
     */
    @XmlAttribute
    public void setDescription(String description) {
        this.description = ConfigElement.getValue(description);
    }

    /**
     * @return the featureManager for this configuration
     */
    public FeatureManager getFeatureManager() {
        if (this.featureManager == null) {
            this.featureManager = new FeatureManager();
        }
        return this.featureManager;
    }

    public ConfigElementList<BasicRegistry> getBasicRegistries() {
        if (this.basicRegistries == null) {
            this.basicRegistries = new ConfigElementList<BasicRegistry>();
        }
        return this.basicRegistries;
    }

    public ConfigElementList<Bell> getBells() {
        if (this.bells == null) {
            this.bells = new ConfigElementList<Bell>();
        }
        return this.bells;
    }

    public ConfigElementList<Cloudant> getCloudants() {
        if (this.cloudants == null)
            this.cloudants = new ConfigElementList<Cloudant>();
        return this.cloudants;
    }

    public ConfigElementList<CloudantDatabase> getCloudantDatabases() {
        if (this.cloudantDatabases == null)
            this.cloudantDatabases = new ConfigElementList<CloudantDatabase>();
        return this.cloudantDatabases;
    }

    public ConfigElementList<ConcurrencyPolicy> getConcurrencyPolicies() {
        if (this.concurrencyPolicies == null)
            this.concurrencyPolicies = new ConfigElementList<ConcurrencyPolicy>();
        return this.concurrencyPolicies;
    }

    public ConfigElementList<ContextService> getContextServices() {
        if (this.contextServices == null)
            this.contextServices = new ConfigElementList<ContextService>();
        return this.contextServices;
    }

    public ConfigElementList<ConnectionFactory> getConnectionFactories() {
        if (this.connectionFactories == null)
            this.connectionFactories = new ConfigElementList<ConnectionFactory>();
        return this.connectionFactories;
    }

    /**
     * Retrieves the list of HttpEndpoints in this configuration
     *
     * @return the list of HttpEndpoints in this configuration
     */
    public ConfigElementList<HttpEndpoint> getHttpEndpoints() {
        if (this.httpEndpoints == null) {
            this.httpEndpoints = new ConfigElementList<HttpEndpoint>();
        }
        return this.httpEndpoints;
    }

    /**
     * Retrieves the list of VirtualHosts in this configuration
     *
     * @return the list of VirtualHosts in this configuration
     */
    public ConfigElementList<VirtualHost> getVirtualHosts() {
        if (this.virtualHosts == null) {
            this.virtualHosts = new ConfigElementList<>();
        }
        return this.virtualHosts;
    }

    /**
     * Retrieves the list of wasJmsEndpoints in this configuration
     *
     * @return the list of JmsEndpoints in this configuration
     */
    public ConfigElementList<JmsEndpoint> getWasJmsEndpoints() {
        if (this.wasJmsEndpoints == null) {
            this.wasJmsEndpoints = new ConfigElementList<JmsEndpoint>();
        }
        return this.wasJmsEndpoints;
    }

    /**
     * Retrieves the list of messaging engines in this configuration
     *
     * @return the list of messagine engines in this configuration
     */
    public ConfigElementList<MessagingEngine> getMessagingEngines() {
        if (this.messagingEngines == null) {
            this.messagingEngines = new ConfigElementList<MessagingEngine>();
        }
        return this.messagingEngines;
    }

    /**
     * @return the HTTP session manager configuration for this server
     */
    public HttpSession getHttpSession() {
        if (this.httpSession == null) {
            this.httpSession = new HttpSession();
        }
        return this.httpSession;
    }

    /**
     * @return the list of httpSesssionCache configuration elements
     */
    public ConfigElementList<HttpSessionCache> getHttpSessionCaches() {
        if (this.httpSessionCaches == null)
            this.httpSessionCaches = new ConfigElementList<HttpSessionCache>();
        return this.httpSessionCaches;
    }

    /**
     * @return the HTTP session manager database configuration for this server
     */
    public HttpSessionDatabase getHttpSessionDatabase() {
        if (this.httpSessionDatabase == null) {
            this.httpSessionDatabase = new HttpSessionDatabase();
        }
        return this.httpSessionDatabase;
    }

    public ConfigElementList<JMSActivationSpec> getJMSActivationSpecs() {
        if (this.jmsActivationSpecs == null)
            this.jmsActivationSpecs = new ConfigElementList<JMSActivationSpec>();
        return this.jmsActivationSpecs;
    }

    public ConfigElementList<JMSConnectionFactory> getJMSConnectionFactories() {
        if (this.jmsConnectionFactories == null)
            this.jmsConnectionFactories = new ConfigElementList<JMSConnectionFactory>();
        return this.jmsConnectionFactories;
    }

    public ConfigElementList<JMSDestination> getJMSDestinations() {
        if (this.jmsDestinations == null)
            this.jmsDestinations = new ConfigElementList<JMSDestination>();
        return this.jmsDestinations;
    }

    public ConfigElementList<JMSQueue> getJMSQueues() {
        if (this.jmsQueues == null)
            this.jmsQueues = new ConfigElementList<JMSQueue>();
        return this.jmsQueues;
    }

    public ConfigElementList<JMSQueueConnectionFactory> getJMSQueueConnectionFactories() {
        if (this.jmsQueueConnectionFactories == null)
            this.jmsQueueConnectionFactories = new ConfigElementList<JMSQueueConnectionFactory>();
        return this.jmsQueueConnectionFactories;
    }

    public ConfigElementList<JMSTopic> getJMSTopics() {
        if (this.jmsTopics == null)
            this.jmsTopics = new ConfigElementList<JMSTopic>();
        return this.jmsTopics;
    }

    public ConfigElementList<JMSTopicConnectionFactory> getJMSTopicConnectionFactories() {
        if (this.jmsTopicConnectionFactories == null)
            this.jmsTopicConnectionFactories = new ConfigElementList<JMSTopicConnectionFactory>();
        return this.jmsTopicConnectionFactories;
    }

    public ConfigElementList<JPA> getJPAs() {
        if (this.jpas == null)
            this.jpas = new ConfigElementList<JPA>();
        return this.jpas;
    }

    public ConfigElementList<ManagedExecutorService> getManagedExecutorServices() {
        if (this.managedExecutorServices == null)
            this.managedExecutorServices = new ConfigElementList<ManagedExecutorService>();
        return this.managedExecutorServices;
    }

    public ConfigElementList<ManagedScheduledExecutorService> getManagedScheduledExecutorServices() {
        if (this.managedScheduledExecutorServices == null)
            this.managedScheduledExecutorServices = new ConfigElementList<ManagedScheduledExecutorService>();
        return this.managedScheduledExecutorServices;
    }

    public ConfigElementList<ManagedThreadFactory> getManagedThreadFactories() {
        if (this.managedThreadFactories == null)
            this.managedThreadFactories = new ConfigElementList<ManagedThreadFactory>();
        return this.managedThreadFactories;
    }

    public ConfigElementList<Monitor> getMonitors() {
        if (monitors == null)
            monitors = new ConfigElementList<Monitor>();
        return monitors;
    }

    public ConfigElementList<ResourceAdapter> getResourceAdapters() {
        if (this.resourceAdapters == null)
            this.resourceAdapters = new ConfigElementList<ResourceAdapter>();
        return this.resourceAdapters;
    }

    public ConfigElementList<JCAGeneratedProperties> getProperties_CalendarApp_CalendarRA() {
        return properties_CalendarApp_CalendarRA == null ? (properties_CalendarApp_CalendarRA = new ConfigElementList<JCAGeneratedProperties>()) : properties_CalendarApp_CalendarRA;
    }

    /**
     * @return the WebContainer configuration for this server
     */
    public WebContainerElement getWebContainer() {
        if (this.webContainer == null) {
            this.webContainer = new WebContainerElement();
        }
        return this.webContainer;
    }

    public Kerberos getKerberos() {
        if (kerberos == null)
            kerberos = new Kerberos();
        return kerberos;
    }

    /**
     * @return the KeyStore configurations for this server
     */
    public ConfigElementList<KeyStore> getKeyStores() {
        if (this.keyStores == null) {
            this.keyStores = new ConfigElementList<KeyStore>();
        }
        return this.keyStores;
    }

    /**
     * @return the ssl configurations for this server
     */
    public ConfigElementList<SSL> getSsls() {
        if (this.ssls == null) {
            this.ssls = new ConfigElementList<SSL>();
        }
        return this.ssls;
    }

    /**
     * @return the sslDefault configuration for this server
     */
    public SSLDefault getSSLDefault() {
        if (this.sslDefault == null) {
            this.sslDefault = new SSLDefault();
        }
        return this.sslDefault;
    }

    public void setSSLDefault(SSLDefault sslDflt) {
        this.sslDefault = sslDflt;
    }

    public SSL getSSLById(String sslCfgId) {
        ConfigElementList<SSL> sslCfgs = getSsls();

        for (SSL sslEntry : sslCfgs) {
            if (sslEntry.getId().equals(sslCfgId)) {
                return sslEntry;
            }
        }
        return null;
    }

    public void addSSL(SSL sslCfg) {

        ConfigElementList<SSL> sslCfgs = getSsls();

        for (SSL sslEntry : sslCfgs) {
            if (sslEntry.getId().equals(sslCfg.getId())) {
                sslCfgs.remove(sslEntry);
            }
        }
        sslCfgs.add(sslCfg);
        return;
    }

    /**
     * @return the EJB Container configuration for this server
     */
    public EJBContainerElement getEJBContainer() {
        if (this.ejbContainer == null) {
            this.ejbContainer = new EJBContainerElement();
        }
        return this.ejbContainer;
    }

    public APIDiscoveryElement getAPIDiscoveryElement() {
        if (this.apiDiscoveryElement == null) {
            this.apiDiscoveryElement = new APIDiscoveryElement();
        }

        return this.apiDiscoveryElement;
    }

    public OpenAPIElement getOpenAPIElement() {
        if (this.openAPIElement == null) {
            this.openAPIElement = new OpenAPIElement();
        }

        return this.openAPIElement;
    }

    public MPMetricsElement getMPMetricsElement() {
        if (this.mpMetricsElement == null) {
            this.mpMetricsElement = new MPMetricsElement();
        }

        return this.mpMetricsElement;
    }

    /**
     * @return the Jsp configuration for this server
     */
    public JspEngineElement getJspEngine() {
        if (this.jspEngine == null) {
            this.jspEngine = new JspEngineElement();
        }
        return this.jspEngine;
    }

    /**
     * @return the includeElement
     */
    public ConfigElementList<IncludeElement> getIncludes() {
        if (this.includeElements == null)
            this.includeElements = new ConfigElementList<IncludeElement>();
        return this.includeElements;
    }

    /**
     * @return the ExecutorElement
     */
    public ExecutorElement getExecutor() {
        if (this.executor == null)
            this.executor = new ExecutorElement();
        return this.executor;
    }

    public void setExecutorElement(ExecutorElement exec) {
        this.executor = exec;
    }

    /**
     * @return the applicationMonitor
     */
    public ClassloadingElement getClassLoadingElement() {

        if (this.classLoading == null)
            this.classLoading = new ClassloadingElement();

        return this.classLoading;
    }

    /**
     * @return the applicationMonitor
     */
    public ApplicationMonitorElement getApplicationMonitor() {
        if (this.applicationMonitor == null)
            this.applicationMonitor = new ApplicationMonitorElement();

        return this.applicationMonitor;
    }

    /**
     * @return explicitly installed applications
     */
    public ConfigElementList<Application> getApplications() {
        if (this.applications == null) {
            this.applications = new ConfigElementList<Application>();
        }
        return this.applications;
    }

    /**
     * @return explicitly installed web applications
     */
    public ConfigElementList<WebApplication> getWebApplications() {
        if (this.webApplications == null) {
            this.webApplications = new ConfigElementList<WebApplication>();
        }
        return this.webApplications;
    }

    /**
     * @return explicitly installed Spring Boot applications
     */
    public ConfigElementList<SpringBootApplication> getSpringBootApplications() {
        if (this.springBootApplications == null) {
            this.springBootApplications = new ConfigElementList<SpringBootApplication>();
        }
        return this.springBootApplications;
    }

    /**
     * @return the connection managers
     */
    public ConfigElementList<ConnectionManager> getConnectionManagers() {
        if (this.connManagers == null)
            this.connManagers = new ConfigElementList<ConnectionManager>();

        return this.connManagers;
    }

    /**
     * Removes all applications with a specific name
     *
     * @param name
     * the name of the applications to remove
     * @return the removed applications (no longer bound to the server
     * configuration)
     */
    public ConfigElementList<Application> removeApplicationsByName(String name) {
        ConfigElementList<Application> installedApps = this.getApplications();
        ConfigElementList<Application> uninstalledApps = new ConfigElementList<Application>();
        for (Application app : installedApps) {
            if (name != null && name.equals(app.getName())) {
                uninstalledApps.add(app);
            }
        }
        installedApps.removeAll(uninstalledApps);
        return uninstalledApps;
    }

    /**
     * Adds an application to the current config, or updates an application with
     * a specific name if it already exists
     *
     * @param name
     * the name of the application
     * @param path
     * the fully qualified path to the application archive on the
     * liberty machine
     * @param type
     * the type of the application (ear/war/etc)
     * @return the deployed application
     */
    public Application addApplication(String name, String path, String type) {
        ConfigElementList<Application> apps = this.getApplications();
        Application application = null;
        for (Application app : apps) {
            if (name != null && name.equals(app.getName())) {
                application = app;
            }
        }
        if (application == null) {
            application = new Application();
            apps.add(application);
        }
        application.setName(name);
        application.setId(name); // application names must be unique, just like element ID names (other config objects probably aren't sharing the app name)
        application.setType(type);
        application.setLocation(path); // assumes that archive has already been transferred; see FileSetup.java
        return application;
    }

    /**
     * @return gets all configured JDBC drivers
     */
    public ConfigElementList<JdbcDriver> getJdbcDrivers() {
        if (this.jdbcDrivers == null) {
            this.jdbcDrivers = new ConfigElementList<JdbcDriver>();
        }
        return this.jdbcDrivers;
    }

    /**
     * @return gets all configured shared libraries
     */
    public ConfigElementList<Library> getLibraries() {
        if (this.libraries == null) {
            this.libraries = new ConfigElementList<Library>();
        }
        return this.libraries;
    }

    /**
     * @return gets all configured osgi libraries
     */
    public ConfigElementList<OsgiLibraryElement> getOsgiLibraries() {
        if (this.osgiLibraries == null) {
            this.osgiLibraries = new ConfigElementList<OsgiLibraryElement>();
        }
        return this.osgiLibraries;
    }

    /**
     * @return gets all configured file sets
     */
    public ConfigElementList<Fileset> getFilesets() {
        if (this.filesets == null) {
            this.filesets = new ConfigElementList<Fileset>();
        }
        return this.filesets;
    }

    /**
     * @return get fileset by id
     */
    public Fileset getFilesetById(String id) {
        if (this.filesets != null)
            for (Fileset fileset : this.filesets)
                if (fileset.getId().equals(id))
                    return fileset;

        return null;
    }

    /**
     * @return gets all configured top level dataSource elements.
     */
    public ConfigElementList<DataSource> getDataSources() {
        if (this.dataSources == null) {
            this.dataSources = new ConfigElementList<DataSource>();
        }
        return this.dataSources;
    }

    /**
     * @return gets logging configuration
     */
    public Logging getLogging() {
        if (this.logging == null) {
            this.logging = new Logging();
        }
        return this.logging;
    }

    /**
     * @return the config
     */
    public ConfigMonitorElement getConfig() {
        if (this.config == null) {
            this.config = new ConfigMonitorElement();
        }
        return config;
    }

    public Transaction getTransaction() {
        if (this.transaction == null)
            this.transaction = new Transaction();
        return this.transaction;
    }

    /**
     * @return all configured <variable> elements
     */
    public ConfigElementList<Variable> getVariables() {
        if (this.variables == null) {
            this.variables = new ConfigElementList<Variable>();
        }
        return this.variables;
    }

    /**
     * @return all configured <mongodb> elements
     */
    public ConfigElementList<MongoDBElement> getMongoDBs() {
        if (this.mongoDBs == null) {
            this.mongoDBs = new ConfigElementList<MongoDBElement>();
        }
        return this.mongoDBs;
    }

    /**
     * @return all configured <mongo> elements
     */
    public ConfigElementList<MongoElement> getMongos() {
        if (this.mongos == null) {
            this.mongos = new ConfigElementList<MongoElement>();
        }
        return this.mongos;
    }

    /**
     * @return all configured <mongo> elements
     */
    public ConfigElementList<CouchDBElement> getCouchDBs() {
        if (this.couchDBs == null) {
            this.couchDBs = new ConfigElementList<CouchDBElement>();
        }
        return this.couchDBs;
    }

    /**
     * Returns a list of configured top level databaseStores elements.
     *
     * @return A list of configured top level databaseStores elements.
     */
    public ConfigElementList<DatabaseStore> getDatabaseStores() {
        if (this.databaseStores == null) {
            this.databaseStores = new ConfigElementList<DatabaseStore>();
        }

        return this.databaseStores;
    }

    /**
     * Returns a list of configured top level persistentExecutor elements.
     *
     * @return A list of configured top level persistentExecutor elements.
     */
    public ConfigElementList<PersistentExecutor> getPersistentExecutors() {
        if (this.persistentExecutors == null) {
            this.persistentExecutors = new ConfigElementList<PersistentExecutor>();
        }

        return this.persistentExecutors;
    }

    /**
     * Returns the configured top level scalingDefinitions element.
     *
     * @return The configured top level scalingDefinitions element.
     */
    public ScalingDefinitions getScalingDefinitions() {
        if (this.scalingDefinitions == null) {
            this.scalingDefinitions = new ScalingDefinitions();
        }

        return this.scalingDefinitions;
    }

    /**
     * Returns a list of configured top level remoteFileAccess elements.
     *
     * @return A list of configured top level remoteFileAccess elements.
     */
    public ConfigElementList<RemoteFileAccess> getRemoteFileAccess() {
        if (this.remoteFileAccesses == null) {
            this.remoteFileAccesses = new ConfigElementList<RemoteFileAccess>();
        }

        return this.remoteFileAccesses;
    }

    /**
     * @return The configured top level orb element.
     */
    public ORB getOrb() {
        if (this.orb == null) {
            this.orb = new ORB();
        }

        return this.orb;
    }

    private List<Field> getAllXmlElements() {
        List<Field> xmlElements = new ArrayList<Field>();
        for (Field field : getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(XmlElement.class))
                xmlElements.add(field);
        }
        return xmlElements;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServerConfiguration clone() throws CloneNotSupportedException {
        ServerConfiguration clone = (ServerConfiguration) super.clone();

        for (Field field : getAllXmlElements()) {
            try {
                Object val = field.get(this);
                if (val instanceof ConfigElementList) {
                    field.set(clone, ((ConfigElementList<ConfigElement>) val).clone());
                } else if (val != null) {
                    field.set(clone, ((ConfigElement) val).clone());
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (RuntimeException e) {
                throw new RuntimeException("Error on field: " + field);
            }
        }
        return clone;
    }

    @Override
    public String toString() {
        String nl = System.getProperty("line.separator");
        StringBuffer buf = new StringBuffer("ServerConfiguration" + nl);

        for (Field field : getAllXmlElements()) {
            try {
                buf.append(field.get(this).toString());
            } catch (Exception ignore) {
            }
        }
        return buf.toString();
    }

    @Override
    public boolean equals(Object otherConfig) {
        if (otherConfig == null)
            return false;
        if (!(otherConfig instanceof ServerConfiguration))
            return false;

        // Consider server configurations equal if their XmlElements match up
        for (Field field : getAllXmlElements()) {
            try {
                Object thisVal = field.get(this);
                Object otherVal = field.get(otherConfig);
                if (!(thisVal == null ? otherVal == null : thisVal.equals(otherVal)))
                    return false;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    /**
     * Calls modify() on elements in the configuration that implement the ModifiableConfigElement interface.
     *
     * No longer using bootstrap properties to update server config for database rotation.
     * Instead look at using the fattest.databases module
     */
    @Deprecated
    public void updateDatabaseArtifacts() throws Exception {
        List<ModifiableConfigElement> mofiableElementList = new ArrayList<ModifiableConfigElement>();
        findModifiableConfigElements(this, mofiableElementList);

        for (ModifiableConfigElement element : mofiableElementList) {
            element.modify(this);
        }
    }

    /**
     * Finds all of the objects in the given config element that implement the
     * ModifiableConfigElement interface.
     *
     * TODO Currently only used for method {@link componenttest.topology.impl.LibertyServer#configureForAnyDatabase()}
     * which is currently deprecated. But this method is specific to Database rotation. If we start using the
     * fat.modify tag and modifiableConfigElement interface for other modification purposes this method can be un-deprecated
     *
     * @param element The config element to check.
     * @param modifiableConfigElements The list containing all modifiable elements.
     * @throws Exception
     */
    @Deprecated
    private void findModifiableConfigElements(Object element, List<ModifiableConfigElement> modifiableConfigElements) throws Exception {

        // If the current element implements ModifiableConfigElement add it to the list.
        if (element instanceof ModifiableConfigElement) {
            modifiableConfigElements.add((ModifiableConfigElement) element);
        }

        // Iterate over all of the elements.
        for (Field field : element.getClass().getDeclaredFields()) {
            if (!field.isAccessible())
                field.setAccessible(true);

            Object fieldValue = field.get(element);

            if (fieldValue != null) {
                if (fieldValue instanceof ConfigElement) {
                    findModifiableConfigElements(fieldValue, modifiableConfigElements);
                } else if (fieldValue instanceof ConfigElementList) {
                    for (ConfigElement e : (ConfigElementList<?>) fieldValue)
                        findModifiableConfigElements(e, modifiableConfigElements);
                }
            }
        }
    }

    /**
     * Removes an unknown element, by tag name. One might use this to remove the
     * configuration for a feature which is not part of the product, for example one
     * that is built and installed by a FAT bucket.
     *
     * @param tagName The tag name that should be removed.
     *
     * @returns A list of the items that were removed.
     */
    public List<Element> removeUnknownElement(String tagName) {
        List<Element> removedElements = new LinkedList<Element>();
        Iterator<Element> i = unknownElements.iterator();
        while ((i != null) && (i.hasNext())) {
            Element e = i.next();
            if ((e != null) && (e.getTagName().equals(tagName))) {
                removedElements.add(e);
                i.remove();
            }
        }
        return removedElements;
    }

    /**
     * Adds elements previously removed from the unknown elements list. This was intended
     * to be used to add anything back to the configuration that was removed by the
     * removeUnknownElements() method.
     *
     * @param unknownElements The elements to add back to the configuration.
     */
    public void addUnknownElements(List<Element> unknownElements) {
        if (this.unknownElements == null) {
            this.unknownElements = new ArrayList<Element>(unknownElements);
        } else {
            for (Element e : unknownElements) {
                this.unknownElements.add(e);
            }
        }
    }

    /**
     * Get the 'federatedRepository' element.
     *
     * @return The {@link FederatedRepository} configuration instance.
     */
    public FederatedRepository getFederatedRepository() {
        return this.federatedRepository;
    }

    /**
     * Set the 'federatedRepository' element.
     *
     * @param federatedRepository The 'federatedRepository' configuration to set.
     */
    public void setFederatedRepositoryElement(FederatedRepository federatedRepository) {
        this.federatedRepository = federatedRepository;
    }

    /**
     * Get all 'ldapRegistry' elements.
     *
     * @return All {@link LdapRegistry} configuration instances.
     */
    public ConfigElementList<LdapRegistry> getLdapRegistries() {
        if (this.ldapRegistries == null) {
            this.ldapRegistries = new ConfigElementList<LdapRegistry>();
        }
        return this.ldapRegistries;
    }

    /**
     * Get all 'activedLdapFilterProperties' elements.
     *
     * @return All {@link LdapFilters} configuration instances.
     */
    public ConfigElementList<LdapFilters> getActivedLdapFilterProperties() {
        if (this.activedLdapFilterProperties == null) {
            this.activedLdapFilterProperties = new ConfigElementList<LdapFilters>();
        }
        return this.activedLdapFilterProperties;
    }

    /**
     * Add a SameSite configuration to this server
     *
     * @param samesite The SameSite element to be added to this server.
     */
    public void addSameSite(SameSite samesite) {

        ConfigElementList<SameSite> samesiteCfgs = getSameSites();

        for (SameSite samesiteEntry : samesiteCfgs) {
            if (samesiteEntry.getId().equals(samesite.getId())) {
                samesiteCfgs.remove(samesiteEntry);
            }
        }
        samesiteCfgs.add(samesite);
    }

    /**
     * @return the samesite configurations for this server
     */
    public ConfigElementList<SameSite> getSameSites() {
        if (this.samesites == null) {
            this.samesites = new ConfigElementList<SameSite>();
        }
        return this.samesites;
    }

    /**
     * @return the AcmeCA configuration for this server
     */
    public AcmeCA getAcmeCA() {
        if (this.acmeCA == null) {
            this.acmeCA = new AcmeCA();
        }
        return this.acmeCA;
    }

    /**
     * @return the javaPermission configurations for this server
     */
    public ConfigElementList<JavaPermission> getJavaPermissions() {
        if (this.javaPermissions == null) {
            this.javaPermissions = new ConfigElementList<JavaPermission>();
        }
        return this.javaPermissions;
    }
}
