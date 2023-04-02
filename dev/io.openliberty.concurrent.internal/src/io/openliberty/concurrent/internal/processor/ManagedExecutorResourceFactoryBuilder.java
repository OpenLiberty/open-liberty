/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
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
package io.openliberty.concurrent.internal.processor;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.concurrency.policy.ConcurrencyPolicy;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.resource.ResourceFactory;
import com.ibm.ws.resource.ResourceFactoryBuilder;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;

@Component(service = ResourceFactoryBuilder.class,
           property = "creates.objectClass=jakarta.enterprise.concurrent.ManagedExecutorService") //  TODO more types?
/**
 * Creates, modifies, and removes ManagedExecutorService resource factories that are defined via ManagedExecutorDefinition.
 */
public class ManagedExecutorResourceFactoryBuilder implements ResourceFactoryBuilder {
    private static final TraceComponent tc = Tr.register(ManagedExecutorResourceFactoryBuilder.class);

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
    private final AtomicServiceReference<ConfigurationAdmin> configAdminRef = new AtomicServiceReference<ConfigurationAdmin>("ConfigAdmin");

    /**
     * Variable registry.
     */
    private final AtomicServiceReference<VariableRegistry> variableRegistryRef = new AtomicServiceReference<VariableRegistry>("VariableRegistry");

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
            Tr.entry(tc, "createResourceFactory", props);

        Hashtable<String, Object> execSvcProps = new Hashtable<String, Object>();
        Hashtable<String, Object> concurrencyPolicyProps = new Hashtable<String, Object>();

        // Initially copy everything into managedExecutorService properties and expand any variables
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
            execSvcProps.put(prop.getKey(), value);
        }

        String declaringApplication = (String) execSvcProps.remove(DECLARING_APPLICATION);
        String application = (String) execSvcProps.get("application");
        String module = (String) execSvcProps.get("module");
        String component = (String) execSvcProps.get("component");
        String jndiName = (String) execSvcProps.get(ResourceFactory.JNDI_NAME);
        String contextSvcJndiName = (String) execSvcProps.remove("context");

        Long hungTaskThreshold = (Long) execSvcProps.remove("hungTaskThreshold");
        Integer maxAsync = (Integer) execSvcProps.remove("maxAsync");
        String[] properties = (String[]) execSvcProps.remove("properties"); // TODO use these properties?

        String managedExecutorServiceID = getManagedExecutorServiceID(application, module, component, jndiName);
        String concurrencyPolicyId = managedExecutorServiceID + "/concurrencyPolicy";
        String contextServiceId = contextSvcJndiName == null || "java:comp/DefaultContextService".equals(contextSvcJndiName) //
                        ? "DefaultContextService" //
                        : ContextServiceResourceFactoryBuilder.getContextServiceID(application, module, component, contextSvcJndiName);

        StringBuilder filter = new StringBuilder(FilterUtils.createPropertyFilter(ID, managedExecutorServiceID));
        filter.insert(filter.length() - 1, '*');
        // Fail if server.xml is already using the id
        if (!removeExistingConfigurations(filter.toString()))
            throw new IllegalArgumentException(managedExecutorServiceID); // internal error, shouldn't ever have been permitted in server.xml

        execSvcProps.put(ID, managedExecutorServiceID);
        execSvcProps.put(CONFIG_DISPLAY_ID, managedExecutorServiceID);
        // Use the unique identifier because jndiName is not always unique for app-defined data sources
        execSvcProps.put(UNIQUE_JNDI_NAME, managedExecutorServiceID);

        execSvcProps.put("ConcurrencyPolicy.target", FilterUtils.createPropertyFilter(ID, concurrencyPolicyId));
        execSvcProps.put("ConcurrencyPolicy.cardinality.minimum", 1);

        String contextSvcFilter = FilterUtils.createPropertyFilter(ID, contextServiceId);
        execSvcProps.put("ContextService.target", contextSvcFilter);
        execSvcProps.put("ContextService.cardinality.minimum", 1);

        // When unspecified, long running task run on the same concurrency policy as other tasks
        execSvcProps.put("LongRunningPolicy.target", "(service.pid=unbound)");
        execSvcProps.put("LongRunningPolicy.cardinality.minimum", 0);

        // jndiName is included in the filter to avoid matching similar services reregistered
        // as non-ResourceFactories by the JNDI implementation
        StringBuilder managedExecutorSvcFilter = new StringBuilder(200);
        managedExecutorSvcFilter.append("(&").append(FilterUtils.createPropertyFilter(ID, managedExecutorServiceID));
        managedExecutorSvcFilter.append("(component.name=com.ibm.ws.concurrent.internal.ManagedExecutorServiceImpl)(jndiName=*))");

        concurrencyPolicyProps.put(ID, concurrencyPolicyId);
        concurrencyPolicyProps.put(CONFIG_DISPLAY_ID, concurrencyPolicyId);
        concurrencyPolicyProps.put("expedite", 0);

        if (maxAsync != null)
            if (maxAsync > 0)
                concurrencyPolicyProps.put("max", maxAsync);
            else if (maxAsync != -1) // unbounded
                throw new IllegalArgumentException(jndiName + " maxAsync=" + maxAsync);

        concurrencyPolicyProps.put("maxPolicy", "loose");
        concurrencyPolicyProps.put("maxWaitForEnqueue", 0L);
        concurrencyPolicyProps.put("runIfQueueFull", false);

        BundleContext concurrencyBundleCtx = ContextServiceDefinitionProvider.priv.getBundleContext(FrameworkUtil.getBundle(WSManagedExecutorService.class));
        BundleContext concurrencyPolicyBundleCtx = ContextServiceDefinitionProvider.priv.getBundleContext(FrameworkUtil.getBundle(ConcurrencyPolicy.class));

        ResourceFactory factory = new AppDefinedResourceFactory(this, concurrencyBundleCtx, declaringApplication, //
                        managedExecutorServiceID, jndiName, managedExecutorSvcFilter.toString(), //
                        contextSvcJndiName, contextSvcFilter);
        try {
            String concurrencyBundleLocation = concurrencyBundleCtx.getBundle().getLocation();
            String concurrencyPolicyBundleLocation = concurrencyPolicyBundleCtx.getBundle().getLocation();

            ConfigurationAdmin configAdmin = configAdminRef.getService();

            Configuration concurrencyPolicyConfig = configAdmin.createFactoryConfiguration("com.ibm.ws.concurrency.policy.concurrencyPolicy",
                                                                                           concurrencyPolicyBundleLocation);
            concurrencyPolicyConfig.update(concurrencyPolicyProps);

            Configuration managedExecutorSvcConfig = configAdmin.createFactoryConfiguration("com.ibm.ws.concurrent.managedExecutorService",
                                                                                            concurrencyBundleLocation);
            managedExecutorSvcConfig.update(execSvcProps);
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
    private static final String getManagedExecutorServiceID(String application, String module, String component, String jndiName) {
        StringBuilder sb = new StringBuilder(jndiName.length() + 80);
        if (application != null) {
            sb.append("application[").append(application).append("]/");
            if (module != null) {
                sb.append("module[").append(module).append("]/");
                if (component != null)
                    sb.append("component[").append(component).append("]/");
            }
        }
        return sb.append("managedExecutorService").append('[').append(jndiName).append(']').toString();
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
    @Reference(service = ConfigurationAdmin.class)
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
    @Reference(service = VariableRegistry.class)
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
}