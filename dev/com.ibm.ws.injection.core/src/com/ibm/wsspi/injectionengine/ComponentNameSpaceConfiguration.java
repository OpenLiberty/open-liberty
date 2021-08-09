/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.Context;

import com.ibm.ejs.util.Util;
import com.ibm.ejs.util.dopriv.SystemGetPropertyPrivileged;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.javaee.dd.common.AdministeredObject;
import com.ibm.ws.javaee.dd.common.ConnectionFactory;
import com.ibm.ws.javaee.dd.common.DataSource;
import com.ibm.ws.javaee.dd.common.EJBRef;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.common.JMSConnectionFactory;
import com.ibm.ws.javaee.dd.common.JMSDestination;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.ws.javaee.dd.common.MessageDestinationRef;
import com.ibm.ws.javaee.dd.common.PersistenceContextRef;
import com.ibm.ws.javaee.dd.common.PersistenceUnitRef;
import com.ibm.ws.javaee.dd.common.ResourceEnvRef;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.injectionengine.factory.EJBLinkReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.IndirectJndiLookupReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.MBLinkReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.ResAutoLinkReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.ResRefReferenceFactory;

/**
 * Defines the 'component' configuration information required for
 * processing of injection and binding directives to populate the
 * java: name space <p>
 *
 * An implementation of this class will need to be provided by each of the
 * different 'containers' that require injection and/or name space
 * population. <p>
 */
public class ComponentNameSpaceConfiguration
{
    // F743-17630CodRv
    /**
     * Indicates the type of flow that is driving the reference processing
     */
    public enum ReferenceFlowKind
    {
        EJB, // for pure ejb content
        WEB, // for pure web content
        CLIENT, // for pure client container content
        MANAGED_BEAN, // for managed bean
        HYBRID // for hybrid content containing both war and ejb
    }

    final String ivDisplayName;
    String ivModuleDisplayName; // d696076
    String ivApplicationDisplayName; // d696076
    final J2EEName ivJ2eeName;
    String ivLogicalAppName; // F743-26137
    String ivLogicalModuleName; // F743-26137
    Context ivJavaColonContext; // F743-17630CodRv
    Map<String, InjectionBinding<?>> ivJavaColonCompEnvMap; // d473811 F743-17630CodRv
    Properties ivEnvProperties;
    ClassLoader ivClassLoader; // d432816
    boolean ivMetadataComplete;
    boolean ivIsSFSB; // d416151.3.1
    Map<Class<?>, Collection<String>> ivClassesToComponents; // F743-30682
    Map<String, Collection<String>> ivPersistenceRefsToComponents; // F743-30682
    List<Class<?>> ivInjectionClasses; // d643600
    Class<?> ivClientMainClass;

    private Map<Class<? extends JNDIEnvironmentRef>, List<? extends JNDIEnvironmentRef>> ivJNDIEnvironmentRefs; // RTC114863
    private Map<Class<? extends JNDIEnvironmentRef>, Map<String, String>> ivJNDIEnvironmentRefBindings; // RTC114863
    private Map<Class<? extends JNDIEnvironmentRef>, Map<String, String>> ivJNDIEnvironmentRefValues; // RTC114863
    private List<? extends EJBRef> ivEJBRefs;
    private List<? extends EJBRef> ivEJBLocalRefs;

    protected IndirectJndiLookupReferenceFactory ivIndirectJndiLookupReferenceFactory; // d440604
    protected ResRefReferenceFactory ivResRefReferenceFactory; // d440604
    protected IndirectJndiLookupReferenceFactory ivResIndirectJndiLookupReferenceFactory; // d440604
    protected EJBLinkReferenceFactory ivEJBLinkReferenceFactory; // d440604
    protected MBLinkReferenceFactory ivMBLinkReferenceFactory; // d698540.1
    protected ResAutoLinkReferenceFactory ivResAutoLinkReferenceFactory; // d455334

    protected ResourceRefConfigList ivResRefList; // d455618
    protected ComponentMetaData ivComponentMetaData; // d487943
    protected ModuleMetaData ivModuleMetaData; // d487943
    protected ApplicationMetaData ivApplicationMetaData; // F743-31682
    protected Set<String> ivManagedBeanClassNames; // F46946.1
    protected Object ivLoadStrategy; // LIDB4511-45.2

    // F743-17630CodRv
    /**
     * Indicates what flow (pure ejb, pure web, client container, hybrid, etc)
     * is requesting the reference processing. Used by the reference processing
     * service to decide which flow specific logic it should perform.
     */
    protected ReferenceFlowKind ivOwningFlow;

    /**
     * Indicates if the ejb component uses activity sessions. Used to perform
     * ejb specific binding logic. Not applicable to non ejb components.
     */
    protected boolean ivEJBCompUsesActivitySessions = false;

    // F743-17630.1
    /**
     * The EJB specific flavor of transaction. It must be a
     * <code>UserTransactionWrapper</code> instance.
     *
     * This is only used in the pure EJB flow.
     */
    protected Object ivEJBUserTransaction = null;

    /**
     * true if this application has been configured for extra
     * configuration checking. Default is false.
     */
    // F743-33178
    private boolean ivCheckAppConfig = false;

    /**
     * Internal metadata required by the injection engine.
     */
    private Object ivInjectionProcessorContext;

    public ComponentNameSpaceConfiguration(String displayName, J2EEName j2eeName) // F743-33811.2
    {
        ivDisplayName = displayName;
        ivJ2eeName = j2eeName;
    }

    /**
     * Returns a meaningful/helpful 'display name' for use in messages
     * logged and exceptions thrown by the Injection Engine. <p>
     *
     * The Injection Engine uses the 'display name' in serveral messages
     * and exception text, so it is important that it provides a name
     * that is helpful to the customer is diagnosing their problem. <p>
     *
     * However, the 'displayName' provided to the Component Configuration
     * is not always set, or set to something useful. This method will
     * determine the most useful 'name' to use for the 'display name'. <p>
     **/
    // d531837
    private static String getDisplayName(String displayName,
                                         J2EEName j2eeName,
                                         boolean isClient)
    {
        String rtnName = displayName;

        if (rtnName == null ||
            rtnName.equals(""))
        {
            if (j2eeName != null)
            {
                rtnName = j2eeName.getComponent();

                if (rtnName == null ||
                    rtnName.equals(""))
                {
                    rtnName = j2eeName.getModule();

                    if (rtnName != null &&
                        (rtnName.endsWith(".war") ||
                        rtnName.endsWith(".jar")))
                    {
                        rtnName = rtnName.substring(0, rtnName.length() - 4);
                    }
                }

                if (rtnName == null ||
                    rtnName.equals(""))
                {
                    rtnName = j2eeName.getApplication();
                }
            }

            if (rtnName == null ||
                rtnName.equals(""))
            {
                if (isClient)
                {
                    rtnName = "CLIENT";
                }
                else
                {
                    rtnName = "SERVLET";
                }
            }
        }

        return rtnName;
    }

    /**
     * Returns the display name (display-name) assigned to the component. This
     * method should be used instead of {@link #getJ2EEName} for error messages.
     **/
    public String getDisplayName()
    {
        return getDisplayName(ivDisplayName, ivJ2eeName, isClientContainer()); // F48603.7
    }

    /**
     * Returns the WebSphere unique Java EE name, composed of the application,
     * module and component names. This method should not be used for error
     * messages; use {@link #getDisplayName}, {@link #getModuleName}, and {@link #getApplicationName} instead.
     *
     * <p>For java:global, this method returns null. For java:app, this method
     * returns the ApplicationMetaData J2EEName. For java:module, this method
     * returns the ModuleMetaData J2EEName. Otherwise, this method returns
     * the ComponentMetaData J2EEName.
     **/
    public J2EEName getJ2EEName()
    {
        return ivJ2eeName;
    }

    /**
     * Returns the Application display name for the component. This method
     * should be used instead of {@link #getJ2EEName} for error messages.
     **/
    public String getApplicationName()
    {
        return ivApplicationDisplayName != null ? ivApplicationDisplayName :
                        ivJ2eeName != null ? ivJ2eeName.getApplication() : null;
    }

    public void setApplicationDisplayName(String name) // d696076
    {
        ivApplicationDisplayName = name;
    }

    /**
     * Returns the Module display name for the component. This method should be
     * used instead of {@link #getJ2EEName} for error messages.
     **/
    public String getModuleName()
    {
        return ivModuleDisplayName != null ? ivModuleDisplayName :
                        ivJ2eeName != null ? ivJ2eeName.getModule() : null;
    }

    public void setModuleDisplayName(String name) // d696076
    {
        ivModuleDisplayName = name;
    }

    /**
     * Sets the logical module name.
     *
     * @param logicalAppName the logical application name, or <tt>null</tt> if
     *            the application represents a standalone module
     * @param logicalModuleName the logical module name
     */
    public void setLogicalModuleName(String logicalAppName, String logicalModuleName) // F743-23167
    {
        ivLogicalAppName = logicalAppName;
        ivLogicalModuleName = logicalModuleName;
    }

    /**
     * Returns the logical application name used for java:global support, or
     * null if the application is a standalone module.
     */
    public String getLogicalApplicationName() // F743-23167
    {
        return ivLogicalAppName;
    }

    /**
     * Returns the logical module name used for java:global support.
     */
    public String getLogicalModuleName() // F743-23167
    {
        return ivLogicalModuleName;
    }

    /**
     * Parses the logical module name from a module path. The module path is a
     * relative path name using forward slashes. The logical module name is the
     * base name of the module name with the suffix removed.
     *
     * @param path the module path
     * @return the logical module name of the module name
     */
    public static String getLogicalModuleName(String path) // F743-23167
    {
        int lastForwardSlash = path.lastIndexOf('/');
        if (lastForwardSlash != -1)
        {
            path = path.substring(lastForwardSlash + 1);
        }

        int lastDot = path.lastIndexOf('.');
        if (lastDot != -1)
        {
            path = path.substring(0, lastDot);
        }

        return path;
    }

    /**
     * Returns the 'java:' name space context associated with the component. <p>
     *
     * The injection engine will populate this context as determined by the
     * metadata from both xml and annotations. <p>
     **/
    public Context getJavaColonContext()
    {
        return ivJavaColonContext;
    }

    /**
     * Returns a map of jndi name to InjectionBinding for the 'java:comp/env'
     * name space context associated with the component. <p>
     *
     * The injection engine will populate this context with all InjectionBinding
     * objects that contribute to the 'java:comp/env' name space context.
     * The jndi name is relative to 'java:comp/env' (i.e. does NOT include
     * 'java:comp/env'). <p>
     *
     * May return 'null' if the component does not need this information. <p>
     **/
    // d473811
    public Map<String, InjectionBinding<?>> getJavaColonCompEnvMap()
    {
        return ivJavaColonCompEnvMap;
    }

    /**
     * Returns the classloader to be used when loading classes
     * referenced by this component. <p>
     *
     * This method may return null to indicate that no class loader is
     * available:
     * <ol>
     * <li>Federated client modules. These modules are started on the server
     * in order to create a java:comp namespace, but the client classpath is
     * unavailable, so the class loader is unavailable.
     * <li>java:global, java:app, and java:module. In these scenarios, the
     * class loader of the caller looking up a reference may be different from
     * the component that declared the reference.
     * </ol>
     *
     * In these cases:
     * <ul>
     * <li>{@link #getInjectionClasses()} must be empty and {@link #getJavaColonCompEnvMap()} must be null
     * <li>References (from annotations and XML) must have already been merged
     * <li>Injection targets will not be processed
     * <li>Some error handling will be deferred until references are used
     * </ul>
     **/
    // d432816
    public ClassLoader getClassLoader()
    {
        return ivClassLoader;
    }

    public void setClassLoader(ClassLoader classLoader) // F743-33811.2
    {
        ivClassLoader = classLoader;
    }

    /**
     * Returns true if the deployment descriptor is marked metadata-complete or
     * the module level is prior to java EE 1.5. <p>
     **/
    public boolean isMetaDataComplete()
    {
        return ivMetadataComplete;
    }

    public void setMetaDataComplete(boolean metaDataComplete) // F743-33811.2
    {
        ivMetadataComplete = metaDataComplete;
    }

    /**
     * Returns true if processing a SFSB.
     **/
    public boolean isSFSB() // d416151.3.1
    {
        return ivIsSFSB;
    }

    public void setSFSB(boolean sfsb) // F48603.7
    {
        ivIsSFSB = sfsb;
    }

    /**
     * Returns the list of classes associated with the component that may define,
     * or be the target of injection. <p>
     **/
    public List<Class<?>> getInjectionClasses()
    {
        return ivInjectionClasses;
    }

    public void setInjectionClasses(List<Class<?>> injectionClasses) // d678836
    {
        ivInjectionClasses = injectionClasses;
        if (isClientContainer() && injectionClasses != null && !injectionClasses.isEmpty()) {
            ivClientMainClass = injectionClasses.get(0);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends JNDIEnvironmentRef> List<? extends T> getJNDIEnvironmentRefs(Class<T> type)
    {
        if (type == EJBRef.class)
        {
            if (ivEJBRefs != null)
            {
                if (ivEJBLocalRefs != null)
                {
                    List<T> result = new ArrayList<T>(ivEJBRefs.size() + ivEJBLocalRefs.size());
                    result.addAll((List) ivEJBRefs);
                    result.addAll((List) ivEJBLocalRefs);
                    return result;
                }
                return (List) ivEJBRefs;
            }

            if (ivEJBLocalRefs != null)
            {
                return (List) ivEJBLocalRefs;
            }
        }

        return ivJNDIEnvironmentRefs == null ? null : (List) ivJNDIEnvironmentRefs.get(type);
    }

    public <T extends JNDIEnvironmentRef> void setJNDIEnvironmentRefs(Class<T> type, List<? extends T> refs) // RTC114863
    {
        if (type == EJBRef.class && (ivEJBRefs != null || ivEJBLocalRefs != null))
        {
            throw new IllegalStateException();
        }
        if (ivJNDIEnvironmentRefs == null)
        {
            ivJNDIEnvironmentRefs = new HashMap<Class<? extends JNDIEnvironmentRef>, List<? extends JNDIEnvironmentRef>>();
        }
        ivJNDIEnvironmentRefs.put(type, refs);
    }

    public <T extends JNDIEnvironmentRef> Map<String, String> getJNDIEnvironmentRefBindings(Class<T> type) // RTC114863
    {
        return ivJNDIEnvironmentRefBindings == null ? null : ivJNDIEnvironmentRefBindings.get(type);
    }

    public <T extends JNDIEnvironmentRef> void setJNDIEnvironmentRefBindings(Class<T> type, Map<String, String> bindings) // RTC114863
    {
        if (ivJNDIEnvironmentRefBindings == null)
        {
            ivJNDIEnvironmentRefBindings = new HashMap<Class<? extends JNDIEnvironmentRef>, Map<String, String>>();
        }
        ivJNDIEnvironmentRefBindings.put(type, bindings);
    }

    public <T extends JNDIEnvironmentRef> Map<String, String> getJNDIEnvironmentRefValues(Class<T> type) // RTC114863
    {
        return ivJNDIEnvironmentRefValues == null ? null : ivJNDIEnvironmentRefValues.get(type);
    }

    public <T extends JNDIEnvironmentRef> void setJNDIEnvironmentRefValues(Class<T> type, Map<String, String> bindings) // RTC114863
    {
        if (ivJNDIEnvironmentRefValues == null)
        {
            ivJNDIEnvironmentRefValues = new HashMap<Class<? extends JNDIEnvironmentRef>, Map<String, String>>();
        }
        ivJNDIEnvironmentRefValues.put(type, bindings);
    }

    /**
     * Returns a list of Environment Entries (&lt;env-entry>) configured
     * for the component.
     **/
    public List<? extends EnvEntry> getEnvEntries()
    {
        return getJNDIEnvironmentRefs(EnvEntry.class);
    }

    public void setEnvEntries(List<? extends EnvEntry> envEntries) // F743-31682
    {
        setJNDIEnvironmentRefs(EnvEntry.class, envEntries);
    }

    /**
     * Sets the list of environment entry value overrides specified by the
     * deployer.
     */
    public void setEnvEntryValues(Map<String, String> envEntryValues) // F743-29779
    {
        setJNDIEnvironmentRefValues(EnvEntry.class, envEntryValues);
    }

    /**
     * Returns a list of environment entry value overrides as specified by the
     * deployer.
     */
    public Map<String, String> getEnvEntryValues() // F743-29779
    {
        return getJNDIEnvironmentRefValues(EnvEntry.class);
    }

    /**
     * Sets the list of environment entry binding overrides specified by the
     * deployer.
     */
    public void setEnvEntryBindings(Map<String, String> envEntryBindings) // F743-29779
    {
        setJNDIEnvironmentRefBindings(EnvEntry.class, envEntryBindings);
    }

    /**
     * Returns a list of environment entry binding overrides as specified by the
     * deployer.
     */
    public Map<String, String> getEnvEntryBindings() // F743-29779
    {
        return getJNDIEnvironmentRefBindings(EnvEntry.class);
    }

    /**
     * Returns the Properties object that will support the EJB 1.0 style
     * environment properties. <p>
     *
     * The injection engine will populate this properties object as determined
     * by the metadata from both xml and annotations. <p>
     **/
    public Properties getEnvProperties()
    {
        return ivEnvProperties;
    }

    /**
     * Returns a list of EJB (Remote) References (&lt;ejb-ref>) configured
     * for the component.
     **/
    public List<? extends EJBRef> getEJBRefs()
    {
        if (ivJNDIEnvironmentRefs != null && ivJNDIEnvironmentRefs.containsKey(EJBRef.class))
        {
            throw new IllegalStateException();
        }

        return ivEJBRefs;
    }

    public void setEJBRefs(List<? extends EJBRef> ejbRefs) // F743-31682
    {
        if (ivJNDIEnvironmentRefs != null && ivJNDIEnvironmentRefs.containsKey(EJBRef.class))
        {
            throw new IllegalStateException();
        }

        ivEJBRefs = ejbRefs;
    }

    /**
     * Returns a list of EJB Local References (&lt;ejb-local-ref>) configured
     * for the component.
     **/
    public List<? extends EJBRef> getEJBLocalRefs()
    {
        if (ivJNDIEnvironmentRefs != null && ivJNDIEnvironmentRefs.containsKey(EJBRef.class))
        {
            throw new IllegalStateException();
        }
        return ivEJBLocalRefs;
    }

    public void setEJBLocalRefs(List<? extends EJBRef> ejbLocalRefs) // F48603.7
    {
        if (ivJNDIEnvironmentRefs != null && ivJNDIEnvironmentRefs.containsKey(EJBRef.class))
        {
            throw new IllegalStateException();
        }
        ivEJBLocalRefs = ejbLocalRefs;
    }

    /**
     * Returns a list of EJB Reference Bindings (&lt;ejbBindings>) configured
     * for the ejb remote and local references of the component.
     **/
    public Map<String, String> getEJBRefBindings()
    {
        return getJNDIEnvironmentRefBindings(EJBRef.class);
    }

    public void setEJBRefBindings(Map<String, String> ejbRefBindings) // F743-33811.2
    {
        setJNDIEnvironmentRefBindings(EJBRef.class, ejbRefBindings);
    }

    /**
     * Returns a list of Web Service References (&lt;service-ref>) configured
     * for the component.
     **/
    public List<? extends ServiceRef> getWebServiceRefs()
    {
        return getJNDIEnvironmentRefs(ServiceRef.class);
    }

    public void setWebServiceRefs(List<? extends ServiceRef> webServiceRefs) // F743-31682
    {
        setJNDIEnvironmentRefs(ServiceRef.class, webServiceRefs);
    }

    /**
     * Returns a list of Resource References (&lt;resource-ref>) configured
     * for the component.
     **/
    public List<? extends ResourceRef> getResourceRefs()
    {
        return getJNDIEnvironmentRefs(ResourceRef.class);
    }

    public void setResourceRefs(List<? extends ResourceRef> resourceRefs) // F743-31682
    {
        setJNDIEnvironmentRefs(ResourceRef.class, resourceRefs);
    }

    /**
     * Returns a list of Resource Reference Bindings (&lt;resRefBindings>)
     * configured for the resource references of the component.
     **/
    public Map<String, String> getResourceRefBindings()
    {
        return getJNDIEnvironmentRefBindings(ResourceRef.class);
    }

    public void setResourceRefBindings(Map<String, String> resRefBindings) // F743-33811.2
    {
        setJNDIEnvironmentRefBindings(ResourceRef.class, resRefBindings);
    }

    /**
     * Returns a list of Resource Environment References (&lt;resource-env-ref>)
     * configured for the component.
     **/
    public List<? extends ResourceEnvRef> getResourceEnvRefs()
    {
        return getJNDIEnvironmentRefs(ResourceEnvRef.class);
    }

    public void setResourceEnvRefs(List<? extends ResourceEnvRef> resourceEnvRefs) // F743-31682
    {
        setJNDIEnvironmentRefs(ResourceEnvRef.class, resourceEnvRefs);
    }

    /**
     * Returns a list of Resource Environment Reference Bindings
     * (&lt;resEnvRefBindings>) configured for the resource environment
     * references of the component.
     **/
    public Map<String, String> getResourceEnvRefBindings()
    {
        return getJNDIEnvironmentRefBindings(ResourceEnvRef.class);
    }

    public void setResourceEnvRefBindings(Map<String, String> resourceEnvRefBindings) // F743-33811.2
    {
        setJNDIEnvironmentRefBindings(ResourceEnvRef.class, resourceEnvRefBindings);
    }

    /**
     * Returns a list of Message Destination References
     * (&lt;message-destination-ref>) configured for the component.
     **/
    public List<? extends MessageDestinationRef> getMsgDestRefs()
    {
        return getJNDIEnvironmentRefs(MessageDestinationRef.class);
    }

    public void setMsgDestRefs(List<? extends MessageDestinationRef> msgDestRefs) // F743-31682
    {
        setJNDIEnvironmentRefs(MessageDestinationRef.class, msgDestRefs);
    }

    /**
     * Returns a list of Message Destination Reference Bindings
     * (<xxx-xxxBindings>) configured for the message destination
     * references of the component.
     **/
    public Map<String, String> getMsgDestRefBindings()
    {
        return getJNDIEnvironmentRefBindings(MessageDestinationRef.class);
    }

    public void setMsgDestRefBindings(Map<String, String> msgDestRefBindings) // F743-33811.2
    {
        setJNDIEnvironmentRefBindings(MessageDestinationRef.class, msgDestRefBindings);
    }

    /**
     * Returns a list of Persistence Unit References (&lt;persistence-unit-ref>) configured
     * for the component.
     **/
    public List<? extends PersistenceUnitRef> getPersistenceUnitRefs() // d431638
    {
        return getJNDIEnvironmentRefs(PersistenceUnitRef.class);
    }

    public void setPersistenceUnitRefs(List<? extends PersistenceUnitRef> puRefs) // F743-31682
    {
        setJNDIEnvironmentRefs(PersistenceUnitRef.class, puRefs);
    }

    /**
     * Returns a list of Persistence Context References (&lt;persistence-context-ref>) configured
     * for the component.
     **/
    public List<? extends PersistenceContextRef> getPersistenceContextRefs() // d431638
    {
        return getJNDIEnvironmentRefs(PersistenceContextRef.class);
    }

    public void setPersistenceContextRefs(List<? extends PersistenceContextRef> pcRefs) // F743-31682
    {
        setJNDIEnvironmentRefs(PersistenceContextRef.class, pcRefs);
    }

    /**
     * Returns a list of data source resource definitions (&lt;data-source>)
     * configured for the component.
     */
    public List<? extends DataSource> getDataSourceDefinitions() // F743-29246
    {
        return getJNDIEnvironmentRefs(DataSource.class);
    }

    public void setDataSourceDefinitions(List<? extends DataSource> dsDefs) // F743-29246
    {
        setJNDIEnvironmentRefs(DataSource.class, dsDefs);
    }

    /**
     * Returns a list of Connection factory definitions (&lt;connection-factory>)
     * configured for the component.
     */
    public List<? extends ConnectionFactory> getConnectionFactoryDefinitions()
    {
        return getJNDIEnvironmentRefs(ConnectionFactory.class);
    }

    public void setConnectionFactoryDefinitions(List<? extends ConnectionFactory> cfDefs)
    {
        setJNDIEnvironmentRefs(ConnectionFactory.class, cfDefs);
    }

    /**
     * Returns a list of Administered Object definitions (&lt;administered-object>)
     * configured for the component.
     */
    public List<? extends AdministeredObject> getAdministeredObjectDefinitions()
    {
        return getJNDIEnvironmentRefs(AdministeredObject.class);
    }

    public void setAdministeredObjectDefinitions(List<? extends AdministeredObject> aoDefs)
    {
        setJNDIEnvironmentRefs(AdministeredObject.class, aoDefs);
    }

    /**
     * Returns a list of JMSConnectionFactory definitions (&lt;jms-connection-factory>)
     * configured for the component.
     */
    public List<? extends JMSConnectionFactory> getJMSConnectionFactoryDefinitions()
    {
        return getJNDIEnvironmentRefs(JMSConnectionFactory.class);
    }

    public void setJMSConnectionFactoryDefinitions(List<? extends JMSConnectionFactory> jmsCfDefs)
    {
        setJNDIEnvironmentRefs(JMSConnectionFactory.class, jmsCfDefs);
    }

    /**
     * Returns a list of JMSDestination definitions (&lt;jms-destination>)
     * configured for the component.
     */
    public List<? extends JMSDestination> getJMSDestinationDefinitions()
    {
        return getJNDIEnvironmentRefs(JMSDestination.class);
    }

    public void setJMSDestinationFactoryDefinitions(List<? extends JMSDestination> jmsDestinationDefs)
    {
        setJNDIEnvironmentRefs(JMSDestination.class, jmsDestinationDefs);
    }

    /**
     * Returns a map of data source resource definition names to resource
     * names.
     */
    public Map<String, String> getDataSourceDefinitionBindings() // F743-33811
    {
        return getJNDIEnvironmentRefBindings(DataSource.class);
    }

    public void setDataSourceDefinitionBindings(Map<String, String> dsDefBindings) // F743-33811
    {
        setJNDIEnvironmentRefBindings(DataSource.class, dsDefBindings);
    }

    /**
     * Returns the IndirectJndiLookupReferenceFactory to be used by the
     * injection processors when resolving binding objects, where an indirect
     * jndi lookup is required.
     **/
    // d440604.2
    public IndirectJndiLookupReferenceFactory getIndirectJndiLookupReferenceFactory()
    {
        return ivIndirectJndiLookupReferenceFactory;
    }

    public IndirectJndiLookupReferenceFactory getResIndirectJndiLookupReferenceFactory()
    {
        return ivResIndirectJndiLookupReferenceFactory;
    }

    public void setResIndirectJndiLookupReferenceFactory(IndirectJndiLookupReferenceFactory factory)
    {
        ivResIndirectJndiLookupReferenceFactory = factory;
    }

    /**
     * Returns the Resource Ref jndi lookup factory to be used by the
     * injection processors when a resource reference binding object
     * is required.
     **/
    // d440604.2
    public ResRefReferenceFactory getResRefReferenceFactory()
    {
        return ivResRefReferenceFactory;
    }

    public ResAutoLinkReferenceFactory getResAutoLinkReferenceFactory() // d455334
    {
        return ivResAutoLinkReferenceFactory;
    }

    public void setResAutoLinkReferenceFactory(ResAutoLinkReferenceFactory ralrFactory) // F48603.7
    {
        ivResAutoLinkReferenceFactory = ralrFactory;
    }

    /**
     * Returns the EJBLink Reference factory to be used by the injection
     * processors when resolving binding objects for an EJB Reference
     * that does not have a binding.
     **/
    // d440604.2
    public EJBLinkReferenceFactory getEJBLinkReferenceFactory()
    {
        return ivEJBLinkReferenceFactory;
    }

    /**
     * Returns the MBLink Reference factory to be used by the injection
     * processors when resolving binding objects for a managed bean reference
     * that does not have a binding.
     **/
    // d698540.1
    public MBLinkReferenceFactory getMBLinkReferenceFactory()
    {
        return ivMBLinkReferenceFactory;
    }

    /**
     * Return ComponentMetaData object to populate with annotation metadata to
     * be used at a later time. This method will return null during
     * java:global, java:app, and java:module processing.
     */
    public ComponentMetaData getComponentMetaData() //d487943
    {
        return ivComponentMetaData;
    }

    public void setComponentMetaData(ComponentMetaData cmd) // F743-31658
    {
        ivComponentMetaData = cmd;
    }

    /**
     * Return ModuleMetaData object to populate with annotation metadata to be
     * used at a later time. This method will return null during java:global
     * and java:app processing.
     */
    public ModuleMetaData getModuleMetaData() //d487943
    {
        return ivModuleMetaData;
    }

    public void setModuleMetaData(ModuleMetaData mmd) // F743-31682
    {
        ivModuleMetaData = mmd;
    }

    /**
     * Returns ApplicationMetaData for the component configuration. This method
     * will return null during java:global processing.
     */
    public ApplicationMetaData getApplicationMetaData() // F743-31682
    {
        return ivApplicationMetaData != null || ivModuleMetaData == null ?
                        ivApplicationMetaData : ivModuleMetaData.getApplicationMetaData();
    }

    public void setApplicationMetaData(ApplicationMetaData amd) // F743-31682
    {
        ivApplicationMetaData = amd;
    }

    /**
     * Returns the set of classes with the javax.annotation.ManagedBean
     * annotation that exist in the application of the component. This value
     * only needs to be set if {@link #getClassLoader()} is null.
     */
    public Set<String> getManagedBeanClassNames() // F46946.1
    {
        return ivManagedBeanClassNames;
    }

    public void setManagedBeanClassNames(Set<String> mbClassNames) // F46946.1
    {
        ivManagedBeanClassNames = mbClassNames;
    }

    /**
     * Sets the IndirectJndiLookupReferenceFactory to be used by the injection
     * processors when resolving binding objects, where an indirect jndi lookup
     * is required. <p>
     *
     * Called by the Injection Engine when getIndirectJndiLookupReferenceFactory
     * returns null. This enables the Injection Engine to provode the
     * default factory to all Injection Processors. <p>
     *
     * @param factory IndirectJndiLookupReferenceFactory to be used by the
     *            injection processors.
     **/
    // d442047
    public void setIndirectJndiLookupReferenceFactory(IndirectJndiLookupReferenceFactory factory)
    {
        if (ivIndirectJndiLookupReferenceFactory != null)
            throw new IllegalStateException("IndirectJndiLookupReferenceFactory already set");

        ivIndirectJndiLookupReferenceFactory = factory;
    }

    /**
     * Sets the Resource Ref jndi lookup factory to be used by the
     * injection processors when a resource reference binding object
     * is required. <p>
     *
     * Called by the Injection Engine when getResRefReferenceFactory
     * returns null. This enables the Injection Engine to provide the
     * default factory to all Injection Processors. <p>
     *
     * @param factory ResRefReferenceFactory to be used by the
     *            injection processors.
     **/
    // d442047
    public void setResRefReferenceFactory(ResRefReferenceFactory factory)
    {
        if (ivResRefReferenceFactory != null)
            throw new IllegalStateException("ResRefReferenceFactory already set");

        ivResRefReferenceFactory = factory;
    }

    /**
     * Sets the EJBLink Reference factory to be used by the injection
     * processors when resolving binding objects for an EJB Reference
     * that does not have a binding. <p>
     *
     * Called by the Injection Engine when getEJBLinkReferenceFactory
     * returns null. This enables the Injection Engine to provide the
     * default factory to all Injection Processors. <p>
     *
     * @param factory EJBLinkReferenceFactory to be used by the
     *            injection processors.
     **/
    // d442047
    public void setEJBLinkReferenceFactory(EJBLinkReferenceFactory factory)
    {
        if (ivEJBLinkReferenceFactory != null)
            throw new IllegalStateException("EJBLinkReferenceFactory already set");

        ivEJBLinkReferenceFactory = factory;
    }

    /**
     * Sets the MBLink Reference factory to be used by the injection
     * processors when resolving binding objects for a managed bean reference
     * that does not have a binding. <p>
     *
     * Called by the Injection Engine when getMBLinkReferenceFactory
     * returns null. This enables the Injection Engine to provide the
     * default factory to all Injection Processors. <p>
     *
     * @param factory EJBLinkReferenceFactory to be used by the
     *            injection processors.
     **/
    // d698540.1
    public void setMBLinkReferenceFactory(MBLinkReferenceFactory factory)
    {
        if (ivMBLinkReferenceFactory != null)
            throw new IllegalStateException("MBLinkReferenceFactory already set");

        ivMBLinkReferenceFactory = factory;
    }

    /**
     * Overrides to provide a meaningful String useful for trace.
     **/
    @Override
    public String toString()
    {
        return super.toString() + '[' + ivJ2eeName + ']';
    }

    private static Comparator<Class<?>> CLASS_NAME_COMPARATOR = new Comparator<Class<?>>()
    {
        @Override
        public int compare(Class<?> class1, Class<?> class2)
        {
            return class1.getName().compareTo(class2.getName());
        }
    };

    public String toDumpString()
    {
        StringBuilder sb = new StringBuilder(toString());
        String separator = AccessController.doPrivileged(new SystemGetPropertyPrivileged("line.separator", "\n"));
        String indent = "                                 ";

        sb.append(separator + indent + "*** ComponentNameSpaceConfiguration Fields ***");
        sb.append(separator + indent + "DisplayName            = " + ivDisplayName + ", " +
                  ivModuleDisplayName + ", " +
                  ivApplicationDisplayName);
        sb.append(separator + indent + "Logical Names          = " + ivLogicalModuleName + ", " +
                  ivLogicalAppName);
        sb.append(separator + indent + "OwningFlow             = " + ivOwningFlow); // F743-17630
        sb.append(separator + indent + "JavaColonContext       = " + Util.identity(ivJavaColonContext));

        String nsID = getNameSpaceID(ivJavaColonContext);
        if (nsID != null)
        {
            sb.append(separator + indent + "JavaColonContext nsID  = " + nsID);
        }

        sb.append(separator + indent + "MetadataComplete       = " + ivMetadataComplete);
        sb.append(separator + indent + "InjectionClasses       = " + ivInjectionClasses);
        sb.append(separator + indent + "IsBMAS                 = " + ivEJBCompUsesActivitySessions);
        sb.append(separator + indent + "IsSFSB                 = " + ivIsSFSB);
        sb.append(separator + indent + "ClassLoader            = " + Util.identity(ivClassLoader));
        sb.append(separator + indent + "ManagedBeanClassNames  = " + ivManagedBeanClassNames);

        sb.append(separator + indent + "*** ComponentNameSpaceConfiguration References ***");
        Set<Class<? extends JNDIEnvironmentRef>> refTypes = new TreeSet<Class<? extends JNDIEnvironmentRef>>(CLASS_NAME_COMPARATOR);

        if (ivJNDIEnvironmentRefs != null)
        {
            refTypes.addAll(ivJNDIEnvironmentRefs.keySet());
        }
        if (ivJNDIEnvironmentRefValues != null)
        {
            refTypes.addAll(ivJNDIEnvironmentRefValues.keySet());
        }
        if (ivJNDIEnvironmentRefBindings != null)
        {
            refTypes.addAll(ivJNDIEnvironmentRefBindings.keySet());
        }
        if (ivEJBRefs != null || ivEJBLocalRefs != null)
        {
            refTypes.add(EJBRef.class);
        }

        for (Class<? extends JNDIEnvironmentRef> refType : refTypes)
        {
            if (refType == EJBRef.class && (ivEJBRefs != null || ivEJBLocalRefs != null))
            {
                sb.append(separator).append(indent).append(
                                                           String.format("%-30s = %s", "EJBRef", ivEJBRefs));
                sb.append(separator).append(indent).append(
                                                           String.format("%-30s = %s", "EJBLocalRef", ivEJBLocalRefs));
            }
            else if (ivJNDIEnvironmentRefs != null)
            {
                sb.append(separator).append(indent).append(
                                                           String.format("%-30s = %s",
                                                                         refType.getSimpleName(), ivJNDIEnvironmentRefs.get(refType)));
            }

            Map<String, String> values = ivJNDIEnvironmentRefValues == null ? null : ivJNDIEnvironmentRefValues.get(refType);
            if (values != null)
            {
                sb.append(separator).append(indent).append(
                                                           String.format("%-30s = %s", refType.getSimpleName() + "Values", values));
            }
            Map<String, String> bindings = ivJNDIEnvironmentRefBindings == null ? null : ivJNDIEnvironmentRefBindings.get(refType);
            if (bindings != null)
            {
                sb.append(separator).append(indent).append(
                                                           String.format("%-30s = %s", refType.getSimpleName() + "Bindings", bindings));
            }
        }

        sb.append(separator + indent + String.format("%-30s = %s", "EnvProperties", ivEnvProperties));
        sb.append(separator + indent + String.format("%-30s = %s", "ResRefList", ivResRefList));

        sb.append(separator + indent + "*** ComponentNameSpaceConfiguration MetaData ***");
        sb.append(separator + indent + "ComponentMetaData      = " + ivComponentMetaData);
        sb.append(separator + indent + "ModuleMetaData         = " + ivModuleMetaData);
        sb.append(separator + indent + "ApplicationMetaData    = " + ivApplicationMetaData);
        sb.append(separator + indent + "*** ComponentNameSpaceConfiguration Internal ***");
        sb.append(separator + indent + "IndirectJndiLookupReferenceFactory    = " + ivIndirectJndiLookupReferenceFactory);
        sb.append(separator + indent + "ResIndirectJndiLookupReferenceFactory = " + ivResIndirectJndiLookupReferenceFactory);
        sb.append(separator + indent + "ResRefReferenceFactory                = " + ivResRefReferenceFactory);
        sb.append(separator + indent + "ResAutoLinkReferenceFactory           = " + ivResAutoLinkReferenceFactory);
        sb.append(separator + indent + "EJBLinkReferenceFactory               = " + ivEJBLinkReferenceFactory);
        sb.append(separator + indent + "MBLinkReferenceFactory                = " + ivMBLinkReferenceFactory);
        sb.append(separator + indent + "EJBTransactionWrapper                 = " + ivEJBUserTransaction); // F743-17630.1
        sb.append(ivInjectionProcessorContext);

        return sb.toString();
    }

    /**
     * Return a ResourceRefConfigList that provides resource reference binding
     * and extension configuration data that will be updated with resource
     * reference data from XML and annotations.
     */
    // d455618
    public ResourceRefConfigList getResourceRefConfigList()
    {
        return ivResRefList;
    }

    public void setResourceRefConfigList(ResourceRefConfigList resRefList) // F743-33811.2
    {
        ivResRefList = resRefList;
    }

    /**
     * Return the LoadStrategy associated with the application
     * module that is being processed.
     */
    public Object getModuleLoadStrategy()
    {
        return ivLoadStrategy;
    }

    public void setModuleLoadStrategy(Object loadStrategy) // F48603.7
    {
        ivLoadStrategy = loadStrategy;
    }

    /**
     * Returns true if this injection is occurring in the client
     * container.
     */
    public boolean isClientContainer()
    {
        return getOwningFlow() == ReferenceFlowKind.CLIENT;
    }

    /**
     * Indicates injection processing is occurring in the client container. <p>
     *
     * When processing is performed for the client container, the
     * first class in the list of injection classes must be the
     * client application main class.
     */
    public void setClientContainer(boolean clientContainer) // F48603.7
    {
        setOwningFlow(clientContainer ? ReferenceFlowKind.CLIENT : null);
        if (isClientContainer() && ivInjectionClasses != null && !ivInjectionClasses.isEmpty()) {
            ivClientMainClass = ivInjectionClasses.get(0);
        }
    }

    /**
     * Returns true if the specified class is the client main class,
     * or a parent of the client main class.
     */
    public boolean isClientMain(Class<?> injectionClass)
    {
        return ivClientMainClass != null && injectionClass.isAssignableFrom(ivClientMainClass);
    }

    // F743-17630 F743-17630CodRv
    /**
     * Gets the flow (ejb, web, client, hybrid module, etc) that created
     * this ComponentNameSpaceConfiguration object.
     */
    public ReferenceFlowKind getOwningFlow()
    {
        return ivOwningFlow;
    }

    public void setOwningFlow(ReferenceFlowKind owningFlow) // F743-33811.2
    {
        ivOwningFlow = owningFlow;
    }

    // F743-17630
    /**
     * Gets flag that indicates if the bean component represented by this
     * CompNameSpaceconfigImpl info object uses activity sessions, or not.
     *
     * Does not apply to non bean components.
     */
    public boolean usesActivitySessions()
    {
        return ivEJBCompUsesActivitySessions;
    }

    public void setUsesActivitySessions(boolean usesActivitySessions) // F48603.7
    {
        ivEJBCompUsesActivitySessions = usesActivitySessions;
    }

    // F743-17630CodRv
    public void setJavaColonContext(Context context)
    {
        ivJavaColonContext = context;
    }

    // F743-17630CodRv
    public void setJavaColonCompEnvMap(Map<String, InjectionBinding<?>> ejbContextDataStructure)
    {
        ivJavaColonCompEnvMap = ejbContextDataStructure;
    }

    // F743-17630CodRv
    public void setEnvironmentProperties(Properties ejbContext10DataStructure)
    {
        ivEnvProperties = ejbContext10DataStructure;
    }

    // F743-17630.1
    /**
     * Returns the ejb specific flavor of transaction.
     *
     * This will be a <code>UserTransactionWrapper</code> instance.
     *
     * This should only be used by the EJB container, as the
     * <code>UserTransactionWrapper</code> is specific to EJB.
     *
     * This object should be null in flow that is not pure-EJB,
     * and in a pure EJB flow when container managed transaction
     * is being used. In other words, this object should only
     * exist when we are in a pure-EJB flow, and the ejb component
     * uses bean managed transactions.
     */
    public Object getEJBTransaction()
    {
        return ivEJBUserTransaction;
    }

    public void setEJBTransaction(Object userTran) // F48603.7
    {
        ivEJBUserTransaction = userTran;
    }

    public void setPersistenceMaps(Map<Class<?>, Collection<String>> classesToComponents,
                                   Map<String, Collection<String>> persistenceRefsToComponents) // F743-30682
    {
        ivClassesToComponents = classesToComponents;
        ivPersistenceRefsToComponents = persistenceRefsToComponents;
    }

    /**
     * A mapping of classes to the list of declaring components. This method
     * may return null if this configuration object represents the data for a
     * single component.
     */
    public Map<Class<?>, Collection<String>> getClassesToComponents() // F743-30682
    {
        return ivClassesToComponents;
    }

    /**
     * A mapping of persistence-context-ref and persistence-unit-ref names to
     * a list of declaring components. This method may return null if this
     * configuration object represents the data for a single component.
     */
    public Map<String, Collection<String>> getPersistenceRefsToComponents() // F743-30682
    {
        return ivPersistenceRefsToComponents;
    }

    /**
     * Returns true if this application has been configured for extra
     * configuration checking.
     */
    // F743-33178
    public boolean isCheckApplicationConfiguration()
    {
        return ivCheckAppConfig;
    }

    /**
     * Set whether or not this application has been configured for extra
     * configuration checking.
     */
    // F743-33178
    public void setCheckApplicationConfiguration(boolean checkAppConfig)
    {
        ivCheckAppConfig = checkAppConfig;
    }

    /**
     * Returns context data that is internal to the injection engine.
     */
    public Object getInjectionProcessorContext()
    {
        return ivInjectionProcessorContext;
    }

    /**
     * Set context data that is internal to the injection engine. This method
     * should not be called by clients.
     */
    public void setInjectionProcessorContext(Object context)
    {
        ivInjectionProcessorContext = context;
    }

    /**
     * Returns a name space identifier useful for trace purposes only. <p>
     *
     * Null may be returned, or a String that may be useful in trace. The
     * String is not guaranteed to be unique, nor have a well defined format. <p>
     */
    // F46994.3
    private String getNameSpaceID(Context javaColonContext)
    {
        String nsID = null;
        String context = javaColonContext != null ? javaColonContext.toString() : null;

        if (context != null)
        {
            nsID = "UNKNOWN";
            int idx = context.indexOf("_nameSpaceID=");
            if (idx != -1)
            {
                nsID = context.substring(idx + 13);
                idx = nsID.indexOf(',');
                if (idx != -1)
                {
                    nsID = nsID.substring(0, idx);
                }
            }
        }
        return nsID;
    }

}
