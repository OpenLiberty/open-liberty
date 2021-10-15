/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.concurrent.processor;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.resource.ResourceFactory;
import com.ibm.ws.resource.ResourceFactoryBuilder;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil;

/**
 * Creates, modifies, and removes ContextService resource factories that are defined via ContextServiceDefinition.
 */
public class ContextServiceResourceFactoryBuilder implements ResourceFactoryBuilder {
    private static final TraceComponent tc = Tr.register(ContextServiceResourceFactoryBuilder.class);

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
     * ConfigAdmin service.
     */
    private final AtomicServiceReference<ConfigurationAdmin> configAdminRef = new AtomicServiceReference<ConfigurationAdmin>("configAdmin");

    /**
     * Variable registry.
     */
    private final AtomicServiceReference<VariableRegistry> variableRegistryRef = new AtomicServiceReference<VariableRegistry>("variableRegistry");

    /**
     * Declarative Services method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context context for this component
     */
    protected void activate(ComponentContext context) {
        configAdminRef.activate(context);
        variableRegistryRef.activate(context);
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
            Tr.entry(tc, "createResourceFactory", toString(props));

        Hashtable<String, Object> contextSvcProps = new Hashtable<String, Object>();
        contextSvcProps.put(OnErrorUtil.CFG_KEY_ON_ERROR, "WARN"); // default value from metatype

        // Initially copy everything into contextService properties and expand any variables
        VariableRegistry variableRegistry = variableRegistryRef.getServiceWithException();
        for (Map.Entry<String, Object> prop : props.entrySet()) {
            Object value = prop.getValue();
            if (value instanceof String) {
                value = variableRegistry.resolveRawString((String) value);
            } else if (value instanceof String[]) {
                String[] v = (String[]) value;
                String[] s = new String[v.length];
                for (int i = 0; i < v.length; i++)
                    s[i] = variableRegistry.resolveRawString(v[i]);
            }
            contextSvcProps.put(prop.getKey(), value);
        }

        String declaringApplication = (String) contextSvcProps.remove(DECLARING_APPLICATION);
        String application = (String) contextSvcProps.get("application");
        String module = (String) contextSvcProps.get("module");
        String component = (String) contextSvcProps.get("component");
        String jndiName = (String) contextSvcProps.get(ResourceFactory.JNDI_NAME);

        String contextServiceID = getContextServiceID(application, module, component, jndiName);

        StringBuilder filter = new StringBuilder(FilterUtils.createPropertyFilter(ID, contextServiceID));
        filter.insert(filter.length() - 1, '*');
        // Fail if server.xml is already using the id
        if (!removeExistingConfigurations(filter.toString()))
            throw new IllegalArgumentException(contextServiceID); // internal error, shouldn't ever have been permitted in server.xml

        contextSvcProps.put(ID, contextServiceID);
        contextSvcProps.put(CONFIG_DISPLAY_ID, contextServiceID);
        // Use the unique identifier because jndiName is not always unique for app-defined data sources
        contextSvcProps.put(UNIQUE_JNDI_NAME, contextServiceID);

        // baseContextRef is not supported in app-defined context service. Avoid matching a random contextService
        contextSvcProps.put("baseInstance.target", "(service.pid=unbound)");
        contextSvcProps.put("baseInstance.cardinality.minimum", 0);

        // TODO process these
        // corresponding metatype entry for nested thread context is:
        // <AD id="threadContextConfigRef" type="String" ibm:type="pid" ibm:reference="com.ibm.wsspi.threadcontext.config" ibm:flat="true" cardinality="1000"
        String[] cleared = (String[]) contextSvcProps.remove("cleared");
        String[] propagated = (String[]) contextSvcProps.remove("propagated");
        String[] unchanged = (String[]) contextSvcProps.remove("unchanged");
        String[] properties = (String[]) contextSvcProps.remove("properties");

        BundleContext bundleContext = ContextServiceDefinitionProvider.priv.getBundleContext(FrameworkUtil.getBundle(WSManagedExecutorService.class));

        StringBuilder contextServiceFilter = new StringBuilder(200);
        contextServiceFilter.append("(&").append(FilterUtils.createPropertyFilter(ID, contextServiceID));
        contextServiceFilter.append("(component.name=com.ibm.ws.context.service))");

        ResourceFactory factory = new AppDefinedResourceFactory(this, bundleContext, contextServiceID, contextServiceFilter.toString(), declaringApplication);
        try {
            String bundleLocation = bundleContext.getBundle().getLocation();
            ConfigurationAdmin configAdmin = configAdminRef.getService();

            Configuration contextServiceConfig = configAdmin.createFactoryConfiguration("com.ibm.ws.context.service", bundleLocation);
            contextServiceConfig.update(contextSvcProps);
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
        configAdminRef.deactivate(context);
        variableRegistryRef.deactivate(context);
    }

    /**
     * Utility method that creates a unique identifier for an application defined data source.
     * For example,
     * application[MyApp]/module[MyModule]/connectionFactory[java:module/env/jdbc/cf1]
     *
     * @param application application name if data source is in java:app, java:module, or java:comp. Otherwise null.
     * @param module      module name if data source is in java:module or java:comp. Otherwise null.
     * @param component   component name if data source is in java:comp and isn't in web container. Otherwise null.
     * @param jndiName    configured JNDI name for the data source. For example, java:module/env/jca/cf1
     * @return the unique identifier
     */
    private static final String getContextServiceID(String application, String module, String component, String jndiName) {
        StringBuilder sb = new StringBuilder(jndiName.length() + 80);
        if (application != null) {
            sb.append("application[").append(application).append("]/");
            if (module != null) {
                sb.append("module[").append(module).append("]/");
                if (component != null)
                    sb.append("component[").append(component).append("]/");
            }
        }
        return sb.append("contextService").append('[').append(jndiName).append(']').toString();
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
     * Convert map to text, fully expanding String[] values to make them visible in trace.
     */
    private static final String toString(Map<String, Object> map) {
        boolean first = true;
        StringBuilder b = new StringBuilder(200).append('{');
        if (map != null)
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (first)
                    first = false;
                else
                    b.append(", ");
                b.append(entry.getKey()).append('=');
                Object val = entry.getValue();
                if (val instanceof String[])
                    b.append(Arrays.toString((String[]) val));
                else
                    b.append(val);
            }
        return b.append('}').toString();
    }
}