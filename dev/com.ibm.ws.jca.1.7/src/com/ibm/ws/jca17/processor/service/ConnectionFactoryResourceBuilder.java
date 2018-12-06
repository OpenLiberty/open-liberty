/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca17.processor.service;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.resource.ResourceException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;

import com.ibm.websphere.config.ConfigEvaluatorException;
import com.ibm.websphere.config.WSConfigurationHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jca.cm.AppDefinedResource;
import com.ibm.ws.jca.cm.AppDefinedResourceFactory;
import com.ibm.ws.jca.cm.ConnectionManagerService;
import com.ibm.ws.jca.cm.ConnectorService;
import com.ibm.ws.jca.service.ConnectionFactoryService;
import com.ibm.ws.resource.ResourceFactory;
import com.ibm.ws.resource.ResourceFactoryBuilder;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;

/**
 * Creates, modifies, and removes connection factory that are defined via ConnectionFactoryDefinition.
 */
public class ConnectionFactoryResourceBuilder implements ResourceFactoryBuilder {
    /**  */
    private static final String BASE_PROPERTIES_KEY = "properties.0.";

    private static final TraceComponent tc = Tr.register(ConnectionFactoryResourceBuilder.class);

    /**
     * Name of property used by config service to uniquely identify a component instance.
     */
    private static final String CONFIG_DISPLAY_ID = "config.displayId";

    /**
     * Name of property used by config service to identify where the configuration of a component instance originates.
     */
    private static final String CONFIG_SOURCE = "config.source";

    /**
     * Property value that indicates the configuration originated in a configuration file, such as server.xml,
     * rather than being programmatically created via ConfigurationAdmin.
     */
    private static final String FILE = "file";

    /**
     * Unique identifier attribute name.
     */
    private static final String ID = "id";

    /**
     * Name of property that identifies the application for java:global data sources.
     */
    static final String DECLARING_APPLICATION = "declaringApplication";

    /**
     * Name of internal property that enforces unique JNDI names.
     */
    static final String UNIQUE_JNDI_NAME = "jndiName.unique";
    /**
     * Name of internal property that specifies the target connectionManager service
     */
    static final String TARGET_CONNECTION_MANAGER = "connectionManager.target";

    static final String CONNECTION_MANAGER_REF = "connectionManagerRef";

    private static final String BUNDLE_LOCATION = "ConnectorModuleMetatype@ConnectorModule:";
    private static final String BOOTSTRAP_CONTEXT = "bootstrapContext.target";

    private static final String MANAGED_CONNECTION_FACTORY_CLASS = "managedconnectionfactory-class";
    private static final String CREATES_OBJECTCLASS = "creates.objectClass";
    private static final String RESOURCE_ADAPTER = "resourceAdapter";
    private static final String INTERFACE_NAME = "interfaceName";

    private BundleContext bundleContext;

    private static final String EMBEDDED_RA_PREFIX = "#";

    /**
     * ConfigAdmin service.
     */
    private final AtomicServiceReference<ConfigurationAdmin> configAdminRef = new AtomicServiceReference<ConfigurationAdmin>("configAdmin");
    private final AtomicServiceReference<VariableRegistry> variableRegistryRef = new AtomicServiceReference<VariableRegistry>("variableRegistry");
    private final AtomicServiceReference<MetaTypeService> metaTypeServiceRef = new AtomicServiceReference<MetaTypeService>("metaTypeService");
    private final AtomicServiceReference<WSConfigurationHelper> wsConfigurationHelperRef = new AtomicServiceReference<WSConfigurationHelper>("wsConfigurationHelper");

    /**
     * Declarative Services method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context context for this component
     */
    protected void activate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(this, tc, "activate", context);
        configAdminRef.activate(context);
        variableRegistryRef.activate(context);
        metaTypeServiceRef.activate(context);
        wsConfigurationHelperRef.activate(context);
        bundleContext = AdministeredObjectResourceFactoryBuilder.priv.getBundleContext(context);

    }

    /**
     * Creates a resource factory that creates handles of the type specified
     * by the {@link ResourceFactory#CREATES_OBJECT_CLASS} property.
     *
     * @param props the resource-specific type information
     * @return the resource factory
     * @throws Exception a resource-specific exception
     */
    @Override
    public ResourceFactory createResourceFactory(Map<String, Object> props) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, "createResourceFactory", props);

        Hashtable<String, Object> cmSvcProps = new Hashtable<String, Object>();
        Hashtable<String, Object> connectionFactorySvcProps = new Hashtable<String, Object>();

        Map<String, Object> annotationProps = new HashMap<String, Object>();

        // Initially copy everything into vendor properties and expand any variables
        VariableRegistry variableRegistry = variableRegistryRef.getServiceWithException();
        for (Map.Entry<String, Object> prop : props.entrySet()) {
            Object value = prop.getValue();
            if (value instanceof String)
                value = variableRegistry.resolveRawString((String) value);
            annotationProps.put(prop.getKey(), value);
        }

        String application = (String) annotationProps.remove(AppDefinedResource.APPLICATION);
        String declaringApplication = (String) annotationProps.remove(DECLARING_APPLICATION);
        String module = (String) annotationProps.remove(AppDefinedResource.MODULE);
        String component = (String) annotationProps.remove(AppDefinedResource.COMPONENT);
        String jndiName = (String) annotationProps.remove(ResourceFactory.JNDI_NAME);

        String connectionFactoryID = getConnectionFactoryID(application, module, component, jndiName);
        String conManagerID = connectionFactoryID + '/' + ConnectionManagerService.CONNECTION_MANAGER;

        String conManagerFilter = FilterUtils.createPropertyFilter(ID, conManagerID);

        StringBuilder filter = new StringBuilder(FilterUtils.createPropertyFilter(ID, connectionFactoryID));
        filter.insert(filter.length() - 1, '*');
        // Fail if server.xml is already using the id
        if (!removeExistingConfigurations(filter.toString()))
            throw new IllegalArgumentException(connectionFactoryID); // internal error, shouldn't ever have been permitted in server.xml

        cmSvcProps.put(ID, conManagerID);
        cmSvcProps.put(CONFIG_DISPLAY_ID, conManagerID);

        connectionFactorySvcProps.put(ID, connectionFactoryID);
        connectionFactorySvcProps.put(CONFIG_DISPLAY_ID, connectionFactoryID);
        connectionFactorySvcProps.put(ResourceFactory.JNDI_NAME, jndiName);
        // Use the unique identifier because jndiName is not always unique for app-defined data sources
        connectionFactorySvcProps.put(UNIQUE_JNDI_NAME, connectionFactoryID);
        connectionFactorySvcProps.put(TARGET_CONNECTION_MANAGER, conManagerFilter);

        if (application != null) {
            connectionFactorySvcProps.put(AppDefinedResource.APPLICATION, application);
            if (module != null) {
                connectionFactorySvcProps.put(AppDefinedResource.MODULE, module);
                if (component != null)
                    connectionFactorySvcProps.put(AppDefinedResource.COMPONENT, component);
            }
        }
        String resourceAdapter = (String) annotationProps.remove(RESOURCE_ADAPTER);
        String interfaceName = (String) annotationProps.remove(INTERFACE_NAME);

        //Handle embedded RA as in 18.9
        if (resourceAdapter.startsWith(EMBEDDED_RA_PREFIX)) {
            resourceAdapter = declaringApplication + "." + resourceAdapter.substring(resourceAdapter.indexOf(EMBEDDED_RA_PREFIX) + 1, resourceAdapter.length());
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, "Embedded resourceAdapter name : " + resourceAdapter);
        }

        connectionFactorySvcProps.put(BOOTSTRAP_CONTEXT, "(id=" + resourceAdapter + ")");
        connectionFactorySvcProps.put(CREATES_OBJECTCLASS, interfaceName);

        connectionFactorySvcProps.put("transactionSupport", annotationProps.remove("transactionSupport"));

        Dictionary<String, Object> connectionFactoryDefaultProps = getDefaultProperties(resourceAdapter, interfaceName);

        for (Enumeration<String> keys = connectionFactoryDefaultProps.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            Object value = connectionFactoryDefaultProps.get(key);

            //Override the managed connection factory class default property values with values provided annotation
            if (annotationProps.containsKey(key))
                value = annotationProps.get(key);

            if (value instanceof String)
                value = variableRegistry.resolveString((String) value);

            connectionFactorySvcProps.put(BASE_PROPERTIES_KEY + key, value);
        }

        Object value;
        for (String name : ConnectionManagerService.CONNECTION_MANAGER_PROPS)
            if ((value = annotationProps.remove(name)) != null)
                cmSvcProps.put(name, value);

        BundleContext bundleContext = AdministeredObjectResourceFactoryBuilder.priv.getBundleContext(FrameworkUtil.getBundle(ConnectionFactoryService.class));

        StringBuilder connectionFactoryFilter = new StringBuilder(200);
        connectionFactoryFilter.append("(&").append(FilterUtils.createPropertyFilter(ID, connectionFactoryID));
        connectionFactoryFilter.append(FilterUtils.createPropertyFilter(Constants.OBJECTCLASS, ConnectionFactoryService.class.getName())).append(")");

        ResourceFactory factory = new AppDefinedResourceFactory(this, bundleContext, connectionFactoryID, connectionFactoryFilter.toString(), declaringApplication);
        try {
            String bundleLocation = bundleContext.getBundle().getLocation();
            String jcaBundleLocation = AdministeredObjectResourceFactoryBuilder.priv.getBundleContext(FrameworkUtil.getBundle(ConnectorService.class)).getBundle().getLocation();
            ConfigurationAdmin configAdmin = configAdminRef.getService();

            Configuration conMgrConfig = configAdmin.createFactoryConfiguration(ConnectionManagerService.FACTORY_PID, jcaBundleLocation);
            conMgrConfig.update(cmSvcProps);
            connectionFactorySvcProps.put(CONNECTION_MANAGER_REF, new String[] { conMgrConfig.getPid() });

            Configuration connectionFactorySvcConfig = configAdmin.createFactoryConfiguration(ConnectionFactoryService.FACTORY_PID, bundleLocation);
            connectionFactorySvcConfig.update(connectionFactorySvcProps);
        } catch (Exception x) {
            factory.destroy();
            throw x;
        } catch (Error x) {
            factory.destroy();
            throw x;
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, "createResourceFactory", factory);
        return factory;
    }

    /**
     * Declarative Services method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context context for this component
     */
    protected void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(this, tc, "deactivate", context);
        configAdminRef.deactivate(context);
        variableRegistryRef.deactivate(context);
        metaTypeServiceRef.deactivate(context);
        wsConfigurationHelperRef.deactivate(context);
    }

    private Dictionary<String, Object> getDefaultProperties(String resourceAdapter, String interfaceName) throws ConfigEvaluatorException, ResourceException {
        MetaTypeInformation metaTypeInformation = metaTypeServiceRef.getService().getMetaTypeInformation(bundleContext.getBundle(BUNDLE_LOCATION + resourceAdapter));
        String[] factoryPids = metaTypeInformation.getFactoryPids();

        for (String factoryPid : factoryPids) {
            Dictionary<String, Object> defaultProps = wsConfigurationHelperRef.getService().getMetaTypeDefaultProperties(factoryPid);
            if (defaultProps.get(MANAGED_CONNECTION_FACTORY_CLASS) != null) {
                for (String createsClass : (String[]) defaultProps.get(CREATES_OBJECTCLASS)) {
                    if (createsClass.equals(interfaceName))
                        return defaultProps;
                }
            }
        }
        ResourceException x = new ResourceException();
        throw x;
    }

    /**
     * Utility method that creates a unique identifier for an application defined data source.
     * For example,
     * application[MyApp]/module[MyModule]/connectionFactory[java:module/env/jdbc/cf1]
     *
     * @param application application name if data source is in java:app, java:module, or java:comp. Otherwise null.
     * @param module module name if data source is in java:module or java:comp. Otherwise null.
     * @param component component name if data source is in java:comp and isn't in web container. Otherwise null.
     * @param jndiName configured JNDI name for the data source. For example, java:module/env/jca/cf1
     * @return the unique identifier
     */
    private static final String getConnectionFactoryID(String application, String module, String component, String jndiName) {
        StringBuilder sb = new StringBuilder(jndiName.length() + 80);
        if (application != null) {
            sb.append(AppDefinedResource.APPLICATION).append('[').append(application).append(']').append('/');
            if (module != null) {
                sb.append(AppDefinedResource.MODULE).append('[').append(module).append(']').append('/');
                if (component != null)
                    sb.append(AppDefinedResource.COMPONENT).append('[').append(component).append(']').append('/');
            }
        }
        return sb.append(ConnectionFactoryService.CONNECTION_FACTORY).append('[').append(jndiName).append(']').toString();
    }

    /**
     * This method looks for existing configurations and removes them unless they came
     * from server.xml
     *
     * We can distinguish by checking for the presence of a property named "config.source"
     * which is set to "file" when the configuration originates from server.xml.
     *
     * @param filter used to find existing configurations.
     * @return true if all configurations were removed or did not exist to begin with, otherwise false.
     * @throws Exception if an error occurs.
     */
    @Override
    public final boolean removeExistingConfigurations(String filter) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        ConfigurationAdmin configAdmin = configAdminRef.getService();
        Configuration[] existingConfigurations = configAdmin.listConfigurations(filter);
        if (existingConfigurations != null)
            for (Configuration config : existingConfigurations) {
                Dictionary<?, ?> cfgProps = config.getProperties();
                // Don't remove configuration that came from server.xml
                if (cfgProps != null && FILE.equals(cfgProps.get(CONFIG_SOURCE))) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "configuration found in server.xml: ", config.getPid());
                    return false;
                } else {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "removing", config.getPid());
                    config.delete();
                }
            }

        return true;
    }

    /**
     * Declarative Services method for setting the ConfigurationAdmin service reference.
     *
     * @param ref reference to the service
     */
    protected void setConfigAdmin(ServiceReference<ConfigurationAdmin> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setConfigAdmin", ref);
        configAdminRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the ConfigurationAdmin service reference.
     *
     * @param ref reference to the service
     */
    protected void unsetConfigAdmin(ServiceReference<ConfigurationAdmin> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "unsetConfigAdmin", ref);
        configAdminRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for setting the VariableRegistry service reference.
     *
     * @param ref reference to the service
     */
    protected void setVariableRegistry(ServiceReference<VariableRegistry> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setVariableRegistry", ref);
        variableRegistryRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the VariableRegistry service reference.
     *
     * @param ref reference to the service
     */
    protected void unsetVariableRegistry(ServiceReference<VariableRegistry> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "unsetVariableRegistry", ref);
        variableRegistryRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for setting the WSConfigurationHelper service reference.
     *
     * @param ref reference to the service
     */
    protected void setWsConfigurationHelper(ServiceReference<WSConfigurationHelper> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setWSConfigurationHelper", ref);
        wsConfigurationHelperRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the WSConfigurationHelper service reference.
     *
     * @param ref reference to the service
     */
    protected void unsetWsConfigurationHelper(ServiceReference<WSConfigurationHelper> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "unsetVariableRegistry", ref);
        wsConfigurationHelperRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for setting the MetaTypeService service reference.
     *
     * @param ref reference to the service
     */
    protected void setMetaTypeService(ServiceReference<MetaTypeService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setMetaTypeService", ref);
        metaTypeServiceRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the MetaTypeService service reference.
     *
     * @param ref reference to the service
     */
    protected void unsetMetaTypeService(ServiceReference<MetaTypeService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "unsetMetaTypeService", ref);
        metaTypeServiceRef.unsetReference(ref);
    }
}