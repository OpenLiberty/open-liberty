/*******************************************************************************
 * Copyright (c) 2021,2024 IBM Corporation and others.
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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

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
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.resource.ResourceFactory;
import com.ibm.ws.resource.ResourceFactoryBuilder;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil;

import io.openliberty.concurrent.internal.qualified.QualifiedResourceFactories;
import io.openliberty.concurrent.internal.qualified.QualifiedResourceFactory;
import jakarta.enterprise.concurrent.ContextServiceDefinition;

@Component(service = ResourceFactoryBuilder.class,
           property = "creates.objectClass=jakarta.enterprise.concurrent.ContextService")
/**
 * Creates, modifies, and removes ContextService resource factories that are defined via ContextServiceDefinition.
 */
public class ContextServiceResourceFactoryBuilder implements ResourceFactoryBuilder {
    private static final TraceComponent tc = Tr.register(ContextServiceResourceFactoryBuilder.class);

    private static final String CONTEXT_PID_ZOS_WLM = "com.ibm.ws.zos.wlm.context";

    /**
     * Context types that are provided by built-in components and their configuration pids.
     */
    public static final Map<String, String[]> BUILT_IN_CONTEXT_PIDS = new HashMap<String, String[]>();
    static {
        BUILT_IN_CONTEXT_PIDS.put(ContextServiceDefinition.APPLICATION, new String[] {
                                                                                       "com.ibm.ws.classloader.context",
                                                                                       "com.ibm.ws.javaee.metadata.context"
        });
        BUILT_IN_CONTEXT_PIDS.put("Classification", new String[] { CONTEXT_PID_ZOS_WLM });
        BUILT_IN_CONTEXT_PIDS.put(ContextServiceDefinition.SECURITY, new String[] { "com.ibm.ws.security.context" });
        BUILT_IN_CONTEXT_PIDS.put("SyncToOSThread", new String[] { "com.ibm.ws.security.thread.zos.context" });
        // EmptyHandleList and ContextServiceDefinition.TRANSACTION are not configurable in server.xml
    }

    /**
     * Name of property used by config service to uniquely identify a component instance.
     */
    private static final String CONFIG_DISPLAY_ID = "config.displayId";

    /**
     * Name of property used by config service to identify where the configuration of a component instance originates.
     */
    private static final String CONFIG_SOURCE = "config.source";

    /**
     * Name of property that identifies the application for java:global data sources.
     */
    static final String DECLARING_APPLICATION = "declaringApplication";

    /**
     * Name of property that identifies the class loader of the application artifact
     * that defines the context service definition.
     */
    static final String DECLARING_CLASS_LOADER = "declaringClassLoader";

    /**
     * Name of property that identifies the class loader of the application artifact
     * that defines the context service definition.
     */
    static final String DECLARING_METADATA = "declaringMetadata";

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
     * Add the specified pids as propagated context types
     * by adding flattened config for them to contextSvcProps.
     *
     * @param pids            pids of built-in context types to propagate.
     * @param propagateCount  index for flattened config.
     * @param properties      list of vendor properties from the ContextServiceDefinition, each of the form: name=value.
     * @param contextSvcProps properties for the contextService being configured.
     * @return next index to use for flattened config
     */
    @Trivial
    private int addPropagated(String[] pids, int propagateCount, String[] properties, Hashtable<String, Object> contextSvcProps) {
        for (String pid : pids) {
            String prefix = "threadContextConfigRef." + (propagateCount++);
            if (CONTEXT_PID_ZOS_WLM.equals(pid)) {
                String defaultTransactionClass = "ASYNCBN";
                String daemonTransactionClass = "ASYNCDMN";
                if (properties != null)
                    for (String prop : properties)
                        if (prop.startsWith("defaultTransactionClass="))
                            defaultTransactionClass = prop.substring(24);
                        else if (prop.startsWith("daemonTransactionClass="))
                            daemonTransactionClass = prop.substring(23);

                contextSvcProps.put(prefix + ".wlm", "Propagate");
                contextSvcProps.put(prefix + ".defaultTransactionClass", defaultTransactionClass);
                contextSvcProps.put(prefix + ".daemonTransactionClass", daemonTransactionClass);
            }
            contextSvcProps.put(prefix + ".config.referenceType", pid);
        }

        return propagateCount;
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

        ClassLoader declaringClassLoader = (ClassLoader) contextSvcProps.remove(DECLARING_CLASS_LOADER);
        MetaData declaringMetadata = (MetaData) contextSvcProps.remove(DECLARING_METADATA);
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

        String[] cleared = (String[]) contextSvcProps.remove("cleared");
        String[] propagated = (String[]) contextSvcProps.remove("propagated");
        String[] unchanged = (String[]) contextSvcProps.remove("unchanged");
        String[] properties = (String[]) contextSvcProps.remove("properties"); // TODO process these?
        String[] qualifiers = (String[]) contextSvcProps.remove("qualifiers");

        // Convert qualifier array to list attribute if present
        List<String> qualifierNames = null;
        if (qualifiers != null && qualifiers.length > 0) {
            qualifierNames = Arrays.asList(qualifiers);
            contextSvcProps.put("qualifiers", qualifierNames);
        }

        if (cleared == null)
            cleared = new String[0];
        if (propagated == null)
            propagated = new String[0];
        if (unchanged == null)
            unchanged = new String[0];

        String transaction = null;
        String remaining = null;

        // detect duplicate configuration of context types across all 3 lists
        Set<String> dups = new TreeSet<String>();
        Set<String> used = new HashSet<String>();

        // cleared
        TreeSet<String> cleared3PCtx = new TreeSet<String>();
        for (String type : cleared)
            if (used.add(type)) {
                if (ContextServiceDefinition.ALL_REMAINING.equals(type)) {
                    remaining = "cleared";
                } else if (ContextServiceDefinition.TRANSACTION.equals(type)) {
                    transaction = "cleared";
                } else {
                    String[] pids = BUILT_IN_CONTEXT_PIDS.get(type);
                    if (pids == null)
                        cleared3PCtx.add(type);
                }
            } else {
                dups.add(type);
            }

        // unchanged
        TreeSet<String> unchanged3PCtx = new TreeSet<String>();
        StringBuilder skip = new StringBuilder();
        for (String type : unchanged)
            if (used.add(type)) {
                if (ContextServiceDefinition.ALL_REMAINING.equals(type)) {
                    remaining = "unchanged";
                } else if (ContextServiceDefinition.TRANSACTION.equals(type)) {
                    transaction = "unchanged";
                } else {
                    String[] pids = BUILT_IN_CONTEXT_PIDS.get(type);
                    if (pids == null) {
                        unchanged3PCtx.add(type);
                        skip.append(skip.length() == 0 ? "" : ",").append(type);
                    } else {
                        for (String pid : pids)
                            skip.append(skip.length() == 0 ? "" : ",").append(pid).append(".provider");
                    }
                }
            } else {
                dups.add(type);
            }

        // propagated
        TreeSet<String> propagated3PCtx = new TreeSet<String>();
        int propagateCount = 0;
        used.add("EmptyHandleList"); // built-in type that is always enabled and not configurable
        for (String type : propagated)
            if (used.add(type)) {
                if (ContextServiceDefinition.ALL_REMAINING.equals(type)) {
                    remaining = "propagated";
                } else if (ContextServiceDefinition.TRANSACTION.equals(type)) {
                    transaction = "propagated";
                } else {
                    String[] pids = BUILT_IN_CONTEXT_PIDS.get(type);
                    if (pids == null)
                        propagated3PCtx.add(type);
                    else
                        propagateCount = addPropagated(pids, propagateCount, properties, contextSvcProps);
                }
            } else {
                dups.add(type);
            }

        if (remaining == null)
            remaining = "cleared";
        else if ("propagated".equals(remaining))
            for (Map.Entry<String, String[]> entry : BUILT_IN_CONTEXT_PIDS.entrySet()) {
                String type = entry.getKey();
                if (!used.contains(type)) {
                    used.add(type);
                    String[] pids = entry.getValue();
                    propagateCount = addPropagated(pids, propagateCount, properties, contextSvcProps);
                }
            }
        else if ("unchanged".equals(remaining))
            for (Map.Entry<String, String[]> entry : BUILT_IN_CONTEXT_PIDS.entrySet()) {
                String type = entry.getKey();
                if (!used.contains(type)) {
                    used.add(type);
                    String[] pids = entry.getValue();
                    for (String pid : pids)
                        skip.append(skip.length() == 0 ? "" : ",").append(pid).append(".provider");
                }
            }

        if (skip.length() > 0)
            contextSvcProps.put("context.unchanged", skip.toString());

        if (!dups.isEmpty())
            throw new IllegalArgumentException(Tr.formatMessage(tc, "CWWKC1202.context.lists.overlap",
                                                                dups,
                                                                jndiName,
                                                                Arrays.toString(cleared),
                                                                Arrays.toString(propagated),
                                                                Arrays.toString(unchanged)));

        // Add Transaction context provider
        if (transaction == null)
            transaction = remaining;
        String prefix = "threadContextConfigRef." + (propagateCount++);
        contextSvcProps.put(prefix + ".config.referenceType", "com.ibm.ws.transaction.context");
        contextSvcProps.put(prefix + ".transaction", transaction);

        // Add a ThreadContextProvider that handles third-party context types
        if (remaining.equals("propagated") || remaining.equals("unchanged") || !propagated3PCtx.isEmpty() || !unchanged3PCtx.isEmpty()) {
            prefix = "threadContextConfigRef." + (propagateCount++);
            contextSvcProps.put(prefix + ".config.referenceType", "io.openliberty.thirdparty.context");
            contextSvcProps.put(prefix + ".cleared", new Vector<String>(cleared3PCtx));
            contextSvcProps.put(prefix + ".propagated", new Vector<String>(propagated3PCtx));
            contextSvcProps.put(prefix + ".unchanged", new Vector<String>(unchanged3PCtx));
            contextSvcProps.put(prefix + ".remaining", remaining);
        } // else default behavior is to clear provider types that aren't configured

        BundleContext bundleContext = ContextServiceDefinitionProvider.priv.getBundleContext(FrameworkUtil.getBundle(WSManagedExecutorService.class));

        // jndiName is included in the filter to avoid matching similar services reregistered
        // as non-ResourceFactories by the JNDI implementation
        StringBuilder contextServiceFilter = new StringBuilder(200);
        contextServiceFilter.append("(&").append(FilterUtils.createPropertyFilter(ID, contextServiceID));
        contextServiceFilter.append("(component.name=com.ibm.ws.context.service)(jndiName=*))");

        QualifiedResourceFactory factory = new AppDefinedResourceFactory(this, bundleContext, declaringApplication, //
                        contextServiceID, jndiName, contextServiceFilter.toString(), //
                        null, null, //
                        declaringMetadata, declaringClassLoader, qualifierNames);
        try {
            String bundleLocation = bundleContext.getBundle().getLocation();
            ConfigurationAdmin configAdmin = configAdminRef.getService();

            Configuration contextServiceConfig = configAdmin.createFactoryConfiguration("com.ibm.ws.context.service", bundleLocation);
            contextServiceConfig.update(contextSvcProps);

            if (qualifierNames != null) {
                String jeeName;
                if (module == null) {
                    jeeName = application;
                } else {
                    ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
                    jeeName = cmd.getJ2EEName().toString();
                }

                ServiceReference<QualifiedResourceFactories> ref = bundleContext.getServiceReference(QualifiedResourceFactories.class);

                if (ref == null) // TODO message should include possibility of deployment descriptor element
                    throw new UnsupportedOperationException("The " + jeeName + " application artifact cannot specify the " +
                                                            qualifierNames + " qualifiers on the " +
                                                            jndiName + " " + ContextServiceDefinition.class.getSimpleName() +
                                                            " because the " + "CDI" + " feature is not enabled."); // TODO NLS

                QualifiedResourceFactories qrf = bundleContext.getService(ref);
                qrf.add(jeeName, QualifiedResourceFactory.Type.ContextService, qualifierNames, factory);
            }
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
    static final String getContextServiceID(String application, String module, String component, String jndiName) {
        StringBuilder sb = new StringBuilder(jndiName.length() + 80);
        if (application != null && !jndiName.startsWith("java:global")) {
            sb.append("application[").append(application).append("]/");
            if (module != null && !jndiName.startsWith("java:app")) {
                sb.append("module[").append(module).append("]/");
                if (component != null && !jndiName.startsWith("java:module"))
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

    /**
     * Convert map to text, fully expanding String[] values to make them visible in trace.
     */
    @Trivial
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