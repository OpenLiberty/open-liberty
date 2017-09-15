/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.processor.jms.service;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.resource.ResourceException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

import com.ibm.websphere.config.ConfigEvaluatorException;
import com.ibm.websphere.config.WSConfigurationHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jca.cm.AppDefinedResource;
import com.ibm.ws.jca.cm.AppDefinedResourceFactory;
import com.ibm.ws.jca.processor.jms.util.JMSResourceDefinitionConstants;
import com.ibm.ws.jca.processor.jms.util.JMSResourceDefinitionHelper;
import com.ibm.ws.jca.service.AdminObjectService;
import com.ibm.ws.resource.ResourceFactory;
import com.ibm.ws.resource.ResourceFactoryBuilder;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;

/**
 * Creates, modifies, and removes Admin Objects(JMS Queue/Topic) that are defined via @JMSDestinationDefinition/Deployment Descriptors
 */
public class JMSDestinationResourceFactoryBuilder implements ResourceFactoryBuilder {
    private static final TraceComponent tc = Tr.register(JMSDestinationResourceFactoryBuilder.class);

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

    private static final String ADMINISTERED_OBJECT_CLASS = "adminobject-class";

    private static final String BOOTSTRAP_CONTEXT = "bootstrapContext.target";

    private static final String CREATES_OBJECTCLASS = "creates.objectClass";
    private static final String RESOURCE_ADAPTER = "resourceAdapter";
    private static final String INTERFACE_NAME = "interfaceName";
    private static final String DESCRIPTION = "description";
    private static final String NAME = "name";

    private BundleContext bundleContext;

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
        bundleContext = context.getBundleContext();

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
        //Place holder for admin object properties like queue/topic
        Hashtable<String, Object> adminObjectSvcProps = new Hashtable<String, Object>();
        //Store all the props from annotation as well as from deployment descriptor
        Map<String, Object> annotationDDProps = new HashMap<String, Object>();

        //Just move all the properties from props to annotationProps after resolving strings.
        VariableRegistry variableRegistry = variableRegistryRef.getServiceWithException();
        for (Map.Entry<String, Object> prop : props.entrySet()) {
            Object value = prop.getValue();
            if (value instanceof String)
                value = variableRegistry.resolveString((String) value);
            annotationDDProps.put(prop.getKey(), value);
        }

        String application = (String) annotationDDProps.remove(AppDefinedResource.APPLICATION);
        String declaringApplication = (String) annotationDDProps.remove(DECLARING_APPLICATION);
        String module = (String) annotationDDProps.remove(AppDefinedResource.MODULE);
        String component = (String) annotationDDProps.remove(AppDefinedResource.COMPONENT);
        String jndiName = (String) annotationDDProps.remove(AdminObjectService.JNDI_NAME);
        annotationDDProps.remove(DESCRIPTION);
        annotationDDProps.remove(NAME);

        String adminObjectID = getadminObjectID(application, module, component, jndiName);

        StringBuilder filter = new StringBuilder(FilterUtils.createPropertyFilter(ID, adminObjectID));
        filter.insert(filter.length() - 1, '*');
        // Fail if server.xml is already using the id
        if (!removeExistingConfigurations(filter.toString()))
            throw new IllegalArgumentException(adminObjectID); // internal error, shouldn't ever have been permitted in server.xml

        adminObjectSvcProps.put(ID, adminObjectID);
        adminObjectSvcProps.put(CONFIG_DISPLAY_ID, adminObjectID);
        // Use the unique identifier because jndiName is not always unique for app-defined data sources
        adminObjectSvcProps.put(UNIQUE_JNDI_NAME, adminObjectID);

        adminObjectSvcProps.put(ResourceFactory.JNDI_NAME, jndiName);
        if (application != null) {
            adminObjectSvcProps.put(AppDefinedResource.APPLICATION, application);
            if (module != null) {
                adminObjectSvcProps.put(AppDefinedResource.MODULE, module);
                if (component != null)
                    adminObjectSvcProps.put(AppDefinedResource.COMPONENT, component);
            }
        }
        String resourceAdapter = ((String) annotationDDProps.remove(RESOURCE_ADAPTER));
        String destinationName = (String) annotationDDProps.get(JMSResourceDefinitionConstants.DESTINATION_NAME);
        String interfaceName = (String) annotationDDProps.remove(INTERFACE_NAME);

        //If the resource adapter type is wasJms, then the destination name has to be mapped with respective name in the resource adapter.
        if (JMSResourceDefinitionConstants.RESOURCE_ADAPTER_WASJMS.equals(resourceAdapter)) {

            // The destinationName specified by via annotation/dd has to be mapped to the respective adapter property.
            if (JMSResourceDefinitionConstants.JMS_QUEUE_INTERFACE.equals(interfaceName)) {
                annotationDDProps.put(JMSResourceDefinitionConstants.JMS_QUEUE_NAME, destinationName);
            } else if (JMSResourceDefinitionConstants.JMS_TOPIC_INTERFACE.equals(interfaceName)) {
                annotationDDProps.put(JMSResourceDefinitionConstants.JMS_TOPIC_NAME, destinationName);
            }
            //If the resource adapter type is wmqJms, then the destination name has to be mapped with respective name in the resource adapter.
        } else if (JMSResourceDefinitionConstants.RESOURCE_ADAPTER_WMQJMS.equals(resourceAdapter)) {

            // The destinationName specified by via annotation/dd has to be mapped to the respective adapter property.
            if (JMSResourceDefinitionConstants.JMS_QUEUE_INTERFACE.equals(interfaceName)) {
                annotationDDProps.put(JMSResourceDefinitionConstants.WMQ_QUEUE_NAME, destinationName);
            } else if (JMSResourceDefinitionConstants.JMS_TOPIC_INTERFACE.equals(interfaceName)) {
                annotationDDProps.put(JMSResourceDefinitionConstants.WMQ_TOPIC_NAME, destinationName);
            }
        }
        //Get props with default values only and see if the same is specified by the user in annotation/dd, then use that value otherwise set the default value.
        //Note: Its not necessary for the user to specify the props which has default value, so we set them in here.  
        Dictionary<String, Object> adminObjectDefaultProps = getDefaultProperties(resourceAdapter, interfaceName);

        for (Enumeration<String> keys = adminObjectDefaultProps.keys(); keys.hasMoreElements();)
        {
            String key = keys.nextElement();
            Object value = adminObjectDefaultProps.get(key);

            //Override the administered object default property values with values provided annotation
            if (annotationDDProps.containsKey(key))
                value = annotationDDProps.remove(key);

            if (value instanceof String)
                value = variableRegistry.resolveString((String) value);

            adminObjectSvcProps.put(JMSResourceDefinitionConstants.PROPERTIES_REF_KEY + key, value);
        }

        //Get all the properties for a given resource(get the resource by the interfaceName) from the corresponding resource adapter, 
        //then see if user specified any of these props in annotation/dd, if yes set that value otherwise ignore.
        //Note: The above section for handling default values can be eliminated by handling the same here in this section since we get all the properties. Will be taken care in future.
        AttributeDefinition[] ads = getAttributeDefinitions(resourceAdapter, interfaceName);
        for (AttributeDefinition attributeDefinition : ads) {
            Object value = annotationDDProps.remove(attributeDefinition.getID());
            if (value != null) {

                if (value instanceof String)
                    value = variableRegistry.resolveString((String) value);

                adminObjectSvcProps.put(JMSResourceDefinitionConstants.PROPERTIES_REF_KEY + attributeDefinition.getID(), value);
            }
        }

        adminObjectSvcProps.put(BOOTSTRAP_CONTEXT, "(id=" + resourceAdapter + ")");

        BundleContext bundleContext = FrameworkUtil.getBundle(AdminObjectService.class).getBundleContext();

        StringBuilder adminObjectFilter = new StringBuilder(200);
        adminObjectFilter.append("(&").append(FilterUtils.createPropertyFilter(ID, adminObjectID));
        adminObjectFilter.append(FilterUtils.createPropertyFilter(Constants.OBJECTCLASS, AdminObjectService.class.getName())).append(")");

        ResourceFactory factory = new AppDefinedResourceFactory(this, bundleContext, adminObjectID, adminObjectFilter.toString(), declaringApplication);
        try {

            String bundleLocation = bundleContext.getBundle().getLocation();
            ConfigurationAdmin configAdmin = configAdminRef.getService();

            Configuration adminObjectSvcConfig = configAdmin.createFactoryConfiguration(AdminObjectService.ADMIN_OBJECT_PID, bundleLocation);
            adminObjectSvcConfig.update(adminObjectSvcProps);
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

    private Dictionary<String, Object> getDefaultProperties(String resourceAdapter, String interfaceName) throws ConfigEvaluatorException, ResourceException
    {
        Bundle bundle;
        bundle = JMSResourceDefinitionHelper.getBundle(bundleContext, resourceAdapter);

        if (bundle != null) {

            MetaTypeInformation metaTypeInformation = metaTypeServiceRef.getService().getMetaTypeInformation(bundle);
            String[] factoryPids = metaTypeInformation.getFactoryPids();

            for (String factoryPid : factoryPids)
            {
                Dictionary<String, Object> defaultProps = wsConfigurationHelperRef.getService().getMetaTypeDefaultProperties(factoryPid);
                if (defaultProps.get(ADMINISTERED_OBJECT_CLASS) != null)
                {

                    for (String createsClass : (String[]) defaultProps.get(CREATES_OBJECTCLASS))
                    {
                        if (createsClass.equals(interfaceName))
                            return defaultProps;
                    }

                }
            }
        }
        //TODO Add meaningful exceptions statements here.
        //Like 1) Unable to find the default properties for the interface name, resource adapter specified in the annotation/dd.
        ResourceException x = new ResourceException();
        throw x;
    }

    private AttributeDefinition[] getAttributeDefinitions(String resourceAdapter, String interfaceName) throws ConfigEvaluatorException, ResourceException
    {
        Bundle bundle;
        bundle = JMSResourceDefinitionHelper.getBundle(bundleContext, resourceAdapter);

        if (bundle != null) {

            MetaTypeInformation metaTypeInformation = metaTypeServiceRef.getService().getMetaTypeInformation(bundle);
            String[] factoryPids = metaTypeInformation.getFactoryPids();

            for (String factoryPid : factoryPids)
            {
                Dictionary<String, Object> defaultProps = wsConfigurationHelperRef.getService().getMetaTypeDefaultProperties(factoryPid);
                if (defaultProps.get(ADMINISTERED_OBJECT_CLASS) != null)
                {
                    for (String createsClass : (String[]) defaultProps.get(CREATES_OBJECTCLASS))
                    {
                        if (createsClass.equals(interfaceName)) {
                            ObjectClassDefinition ocd = metaTypeInformation.getObjectClassDefinition(factoryPid, null);
                            return ocd.getAttributeDefinitions(0);
                        }
                    }

                }
            }
        }

        //TODO Add meaningful exceptions statements here.
        //Like 1) Unable to find the default properties for the interface name, resource adapter specified in the annotation/dd.
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
    private static final String getadminObjectID(String application, String module, String component, String jndiName) {
        StringBuilder sb = new StringBuilder(jndiName.length() + 80);
        if (application != null) {
            sb.append(AppDefinedResource.APPLICATION).append('[').append(application).append(']').append('/');
            if (module != null) {
                sb.append(AppDefinedResource.MODULE).append('[').append(module).append(']').append('/');
                if (component != null)
                    sb.append(AppDefinedResource.COMPONENT).append('[').append(component).append(']').append('/');
            }
        }
        return sb.append(AdminObjectService.ADMIN_OBJECT).append('[').append(jndiName).append(']').toString();
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
    //TODO move this method to JMSResoureDefinitionHelper
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