/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.app.deploy.ConnectorModuleInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.jca.metadata.ConnectorModuleMetaData;
import com.ibm.ws.jca.utils.metagen.MetaGenConstants;
import com.ibm.ws.jca.utils.xml.ra.RaConnector;
import com.ibm.ws.kernel.service.util.PrivHelper;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;

/**
 * This class contains the metadata for the resource adapter
 */
public class ConnectorModuleMetaDataImpl extends MetaDataImpl implements ConnectorModuleMetaData {
    private static final String RA_CONSTANT = "ResourceAdapter";

    static final String RA_MODULE_CONSTANT = "ResourceAdapterModule";

    /**
     * Id for WebSphere JMS resource adapter
     */
    private static final String WASJMS = "wasJms";

    /**
     * Id for WebSphere MQ resource adapter
     */
    private static final String WMQJMS = "wmqJms";

    private final ApplicationMetaData applicationMetaData;
    private Boolean autoStart = null;
    final Dictionary<String, Object> embeddedRAConfig;
    private final String identifier;
    private final boolean isEmbedded;
    private boolean hasConfig = true;
    private final J2EEName ivJ2EEName;
    private final String moduleName;
    private final Map<String, Object> metagenConfig = new HashMap<String, Object>();
    private final ComponentMetaData metadata;
    final NestedConfigHelper config;
    final String resourceAdapterPid;
    private final String specVersion;

    ConnectorModuleMetaDataImpl(ExtendedApplicationInfo appInfo, ConnectorModuleInfo cmInfo, RaConnector connector, J2EENameFactory j2eeNameFactory,
                                Container containerToAdapt) throws UnableToAdaptException {
        super(0);

        // appInfo will be null for embedded rars, so we need to get from the cmInfo
        if (isEmbedded = appInfo == null) {
            appInfo = (ExtendedApplicationInfo) cmInfo.getApplicationInfo();
        }
        applicationMetaData = appInfo.getMetaData();
        config = appInfo.getConfigHelper();
        moduleName = cmInfo.getName(); // get the unique module name. This is handled by app manager

        embeddedRAConfig = isEmbedded ? getConfigForEmbeddedResourceAdapter() : null;
        String id = (String) get("id");
        if (id != null && id.startsWith("default-")) // ignore generated id
            id = null;
        // get the moduleName if you do not specify the id of the ra.
        if (isEmbedded) {
            String nameOfAppThatEmbedsRAR = applicationMetaData.getName();
            String alias = (String) get("alias");
            if (alias == null)
                alias = id == null || id.length() == 0 ? moduleName.replace('/', '.') : id;
            id = nameOfAppThatEmbedsRAR + '.' + alias;
            metagenConfig.put(MetaGenConstants.KEY_APP_THAT_EMBEDS_RAR, nameOfAppThatEmbedsRAR);
            ivJ2EEName = j2eeNameFactory.create(applicationMetaData.getJ2EEName().getApplication(), moduleName, null);
        } else {
            if (id == null || "".equals(id))
                id = moduleName.replace('/', '.');
            // Standalone RA same as app, so use a constant for module
            ivJ2EEName = j2eeNameFactory.create(applicationMetaData.getJ2EEName().getApplication(), RA_MODULE_CONSTANT, null);
        }
        // id consists only of supported characters?
        if (!id.matches("[0-9a-zA-Z.\\-_]*"))
            throw new UnableToAdaptException(Utils.getMessage("J2CA8814.resource.adapter.install.failed", id));
        // verify that id isn't one of the reserved ids for wmq or was jms adapter
        if (WMQJMS.equals(id) || WASJMS.equals(id))
            throw new UnableToAdaptException(Utils.getMessage("J2CA8816.reserved.resource.adapter.id", moduleName, WMQJMS, WASJMS));

        processConfigElementCustomizations();
        metagenConfig.put(MetaGenConstants.KEY_ADAPTER_NAME, id);
        metagenConfig.put(MetaGenConstants.KEY_GENERATION_MODE, MetaGenConstants.VALUE_GENERATION_MODE_RAR);
        metagenConfig.put(MetaGenConstants.KEY_USE_ANNOTATIONS, true);
        // contextServiceRef
        String[] refs = (String[]) get("contextServiceRef");
        String filter = refs == null || refs.length == 0 ? "(service.pid=com.ibm.ws.context.manager)" : FilterUtils.createPropertyFilter(Constants.SERVICE_PID, refs[0]);
        metagenConfig.put("contextService.target", filter);
        // .installedByDropins
        Boolean installedByDropins = (Boolean) config.get(".installedByDropins");
        if (installedByDropins != null)
            metagenConfig.put(".installedByDropins", installedByDropins);

        // set the rar container
        metagenConfig.put(MetaGenConstants.RAR_DEPLOYMENT_DESCRIPTOR, connector);
        metagenConfig.put(MetaGenConstants.KEY_TRANSLATE, false);
        metagenConfig.put(MetaGenConstants.KEY_MODULE_NAME, moduleName);
        metagenConfig.put(MetaGenConstants.KEY_RAR_CONTAINER, containerToAdapt);

        // Connector can be null at this point if there is no ra.xml specified which is allowed
        // by JCA 1.6 if annotations are used
        Boolean autoStart = (Boolean) get("autoStart");
        this.autoStart = autoStart == null && connector != null && connector.getResourceAdapter() != null ? connector.getResourceAdapter().getAutoStart() : autoStart;

        this.identifier = id;

        // service.pid of the configured <resourceAdapter> element. Null for resource adapters embedded in applications.
        this.resourceAdapterPid = isEmbedded ? null : (String) config.get(Constants.SERVICE_PID);

        // TODOCJN autostart setting in wlp-ra.xml will not be honored if just annotations are being used
        // and wlp-ra.xml is specified, does this need to be fixed or just documented?
        // Could parse wlp-ra.xml here and extract the value if ra.xml was not provided.
        // Or could parse wlp-ra.xml in ConnectorAdapter and save autoStart value, if specified
        this.specVersion = connector != null ? connector.getVersion() : "1.6";
        J2EEName cmdName = j2eeNameFactory.create(ivJ2EEName.getApplication(), ivJ2EEName.getModule(), RA_CONSTANT);
        this.metadata = new ResourceAdapterMetaData(this, id, moduleName, cmdName, isEmbedded);
    }

    /**
     * Get the server.xml snippet that specifies default instances for the resource adapter.
     *
     * For standalone resource adapters,
     * <server>
     * . <resourceAdapter id="{ra.config.id}">
     * . . <properties.MyAdapter/>
     * . </resourceAdapter>
     * </server>
     *
     * For resource adapters embedded in applications (if a resourceAdapter config element exists for it),
     * <server>
     * . <application id="{app.config.id}">
     * . . <resourceAdapter id="{ra.config.id}">
     * . . . <properties.MyApp.MyAdapter/>
     * . . </resourceAdapter>
     * . </application>
     * </server>
     *
     * In all other cases (dropins or embedded resource adapter without config),
     * <server>
     * . <properties.MyAdapter/>
     * </server>
     *
     * @param alias for the bootstrap context/resource adapter vendor properties element
     * @return a server.xml snippet for default instances. Null if default instances do not need to be nested.
     * @throws Exception
     * @throws UnableToAdaptException
     */
    String getDefaultInstancesXML(String alias) throws Exception {
        StringBuilder xml = new StringBuilder(200).append("\r\n<server>\r\n");

        String extendsPid = (String) config.get(SOURCE_PID);

        if ((extendsPid != null) && (extendsPid.startsWith("com.ibm.ws.jca.resourceAdapter"))) {
            // com.ibm.ws.jca.resourceAdapter[default-0]
            String id = getIDFromSupertype(extendsPid, 31);
            xml.append(" <resourceAdapter id=\"").append(id).append("\">\r\n");
            xml.append("  <").append(alias).append("/>\r\n");
            xml.append(" </resourceAdapter>\r\n");
        } else if (embeddedRAConfig != null && alias != null) {
            String appId;
            String appElementName;
            if (extendsPid != null && extendsPid.startsWith("com.ibm.ws.app.manager.earappcfg")) {
                appId = getIDFromSupertype(extendsPid, 33);
                appElementName = "enterpriseApplication";
            } else {
                String topLevelConfigId = (String) config.get("config.id");
                appId = topLevelConfigId.substring(23, topLevelConfigId.length() - 1);
                appElementName = "application";
            }
            String raId = (String) embeddedRAConfig.get("id");
            xml.append(" <").append(appElementName).append(" id=\"").append(appId).append("\">\r\n");
            xml.append("  <resourceAdapter id=\"").append(raId).append("\">\r\n");
            xml.append("   <").append(alias).append("/>\r\n");
            xml.append("  </resourceAdapter>\r\n");
            xml.append(" </").append(appElementName).append(">\r\n");
        } else {
            hasConfig = false;
            return null;
        }

        xml.append("</server>");
        return xml.toString();
    }

    static final String SOURCE_PID = "ibm.extends.source.pid";
    static final String SOURCE_FACTORY_PID = "ibm.extends.source.factoryPid";

    /**
     * @param extendsPid
     * @return
     * @throws UnableToAdaptException
     */
    private String getIDFromSupertype(String raPid, int startIndex) throws Exception {

        BundleContext bundleContext = PrivHelper.getBundleContext(FrameworkUtil.getBundle(getClass()));
        ServiceReference<ConfigurationAdmin> configAdminRef = PrivHelper.getServiceReference(bundleContext, ConfigurationAdmin.class);
        try {
            String filter = "(" + Constants.SERVICE_PID + "=" + raPid + ")";

            ConfigurationAdmin configAdmin = PrivHelper.getService(bundleContext, configAdminRef);
            Configuration[] configurations = configAdmin.listConfigurations(filter);
            if (configurations != null) {
                String id = (String) configurations[0].getProperties().get("id");
                if (id != null)
                    return id;
                String configId = (String) configurations[0].getProperties().get("config.id");

                return configId.substring(startIndex, configId.length() - 1);
            }

            // This should never happen. If it does, something is wrong with config supertype processing.
            throw new IllegalStateException("Resource Adapter " + raPid + " not found");

        } finally {
            bundleContext.ungetService(configAdminRef);
        }

    }

    /**
     * Returns the id of the top level element under which the properties.* element is nested. Null if not nested.
     *
     * @return the id of the top level element under which the properties.* element is nested. Null if not nested.
     */
    String getDefaultInstanceTopLevelId() {
        String topLevelConfigId = (String) config.get("config.id");
        if (topLevelConfigId != null)
            if (topLevelConfigId.startsWith("com.ibm.ws.jca.resourceAdapter["))
                return topLevelConfigId.substring(31, topLevelConfigId.length() - 1);
            else if (isEmbedded && topLevelConfigId.startsWith("com.ibm.ws.app.manager["))
                return topLevelConfigId.substring(23, topLevelConfigId.length() - 1);
        return null;
    }

    /**
     * Returns the value of a configured property of the resourceAdapter element.
     *
     * @param name property name.
     * @return the value of a configured property of the resourceAdapter element.
     */
    private Object get(String name) {
        if (isEmbedded)
            return embeddedRAConfig == null ? null : embeddedRAConfig.get(name);
        else
            return config.get(name);
    }

    /**
     * Returns the configured value for autoStart.
     *
     * @return the configured value for autoStart.
     */
    final Boolean getAutoStart() {
        return autoStart;
    }

    /**
     * Returns configured properties for an embedded resource adapter, if any, by matching the id.
     *
     * @return configured properties for an embedded resource adapter, if any.
     */
    private final Dictionary<String, Object> getConfigForEmbeddedResourceAdapter() throws UnableToAdaptException {
        BundleContext bundleContext = PrivHelper.getBundleContext(FrameworkUtil.getBundle(getClass()));
        ServiceReference<ConfigurationAdmin> configAdminRef = PrivHelper.getServiceReference(bundleContext, ConfigurationAdmin.class);
        ConfigurationAdmin configAdmin = PrivHelper.getService(bundleContext, configAdminRef);
        try {
            // go through all child resourceAdapter configurations until we find the right one, if any
            String parentPid = (String) config.get(SOURCE_PID); // TODO remove once the supertype of application is gone
            if (parentPid == null)
                parentPid = (String) config.get(Constants.SERVICE_PID);
            String filter = "(&(" + ConfigurationAdmin.SERVICE_FACTORYPID +
                            "=com.ibm.ws.jca.embeddedResourceAdapter)(config.parentPID=" + parentPid + "))";
            Configuration[] configurations = configAdmin.listConfigurations(filter);
            if (configurations != null)
                for (Configuration config : configAdmin.listConfigurations(filter)) {
                    Dictionary<String, Object> props = config.getProperties();
                    String id = (String) props.get("id");
                    if (moduleName.equals(id))
                        return props;
                }

            return null;
        } catch (Exception x) {
            throw new UnableToAdaptException(x);
        } finally {
            bundleContext.ungetService(configAdminRef);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.runtime.metadata.MetaData#getName()
     */
    @Override
    public String getName() {
        return moduleName;
    }

    /**
     * Return the J2EE name of this module
     *
     * @see com.ibm.ws.runtime.metadata.ModuleMetaData
     */
    @Override
    public J2EEName getJ2EEName() {
        return ivJ2EEName;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.runtime.metadata.ModuleMetaData#getApplicationMetaData()
     */
    @Override
    public ApplicationMetaData getApplicationMetaData() {
        return applicationMetaData;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.runtime.metadata.ModuleMetaData#getComponentMetaDatas()
     */
    @Override
    public ComponentMetaData[] getComponentMetaDatas() {
        return new ComponentMetaData[] { metadata };
    }

    /**
     * Returns the unique identifier for the RAR module
     *
     * @return the unique identifier for the RAR module
     */
    @Override
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the configured value or default value of maxWaitForResources,
     * which determines how long we should wait for services to be registered for all of the
     * configured instances of resources provided by this resource adapter to appear.
     *
     * @return the maximum amount of time to wait.
     */
    final long getMaxWaitForResources() {
        Long maxWait = (Long) get("maxWaitForResources");
        return maxWait == null ? 20000L : maxWait;
    }

    /**
     * @return the metatype
     */
    Map<String, Object> getMetaGenConfig() {
        return metagenConfig;
    }

    /**
     * Returns the JCA specification version with which the resource adapter claims compliance.
     *
     * @return the JCA specification version with which the resource adapter claims compliance.
     */
    @Override
    public String getSpecVersion() {
        return specVersion;
    }

    /**
     * Indicates whether or not the RAR module is embedded in an application.
     *
     * @return true if the RAR module is embedded in an application. Otherwise false.
     */
    @Override
    public boolean isEmbedded() {
        return isEmbedded;
    }

    @Trivial
    public boolean hasConfig() {
        return hasConfig;
    }

    /**
     * Look for config element customizations and add them to the metatype generator config.
     * For example, if the following is configured,
     * <resourceAdapter id="rar1" ...
     * <customize interface="javax.sql.DataSource" suffix="SQLConnectionFactory"/>
     * </resourceAdapter>
     * Then you can use
     * <connectionFactory ...
     * <properties.rar1.SQLConnectionFactory .../>
     * </connectionFactory>
     * instead of
     * <connectionFactory ...
     * <properties.rar1.DataSource .../>
     * </connectionFactory>
     */
    private final void processConfigElementCustomizations() {
        Map<String, String> suffixOverridesByIntf = new HashMap<String, String>();
        Map<String, String> suffixOverridesByImpl = new HashMap<String, String>();
        Map<String, String> suffixOverridesByBoth = new HashMap<String, String>();

        String prefix = "customize.";
        for (int count = 0; get(prefix + count + ".config.referenceType") != null; count++) {
            String intf = (String) get(prefix + count + ".interface");
            String impl = (String) get(prefix + count + ".implementation");
            String suffix = (String) get(prefix + count + ".suffix");
            suffix = suffix == null ? "" : suffix;
            if (intf == null)
                if (impl == null)
                    ; // ignore
                else
                    suffixOverridesByImpl.put(impl, suffix);
            else if (impl == null)
                suffixOverridesByIntf.put(intf, suffix);
            else
                suffixOverridesByBoth.put(intf + '-' + impl, suffix);
        }

        metagenConfig.put(MetaGenConstants.KEY_SUFFIX_OVERRIDES_BY_INTERFACE, suffixOverridesByIntf);
        metagenConfig.put(MetaGenConstants.KEY_SUFFIX_OVERRIDES_BY_IMPL, suffixOverridesByImpl);
        metagenConfig.put(MetaGenConstants.KEY_SUFFIX_OVERRIDES_BY_BOTH, suffixOverridesByBoth);
    }
}
