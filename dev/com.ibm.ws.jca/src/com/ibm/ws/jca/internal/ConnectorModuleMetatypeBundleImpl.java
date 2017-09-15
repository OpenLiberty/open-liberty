/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.internal;

import static com.ibm.wsspi.classloading.ApiType.API;
import static com.ibm.wsspi.classloading.ApiType.IBMAPI;
import static com.ibm.wsspi.classloading.ApiType.SPEC;
import static com.ibm.wsspi.classloading.ApiType.STABLE;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;

import com.ibm.websphere.config.WSConfigurationHelper;
import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.classloading.ClassProvider;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ConnectorModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.dynamic.bundle.BundleFactory;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jca.utils.metagen.MetaGenConstants;
import com.ibm.ws.jca.utils.metagen.MetatypeGenerator;
import com.ibm.ws.jca.utils.xml.metatype.Metatype;
import com.ibm.ws.jca.utils.xml.metatype.MetatypeOcd;
import com.ibm.ws.kernel.service.util.PrivHelper;
import com.ibm.wsspi.classloading.ApiType;

/**
 *
 */
class ConnectorModuleMetatypeBundleImpl implements ConnectorModuleMetatype {
    private static final TraceComponent tc = Tr.register(ConnectorModuleMetatypeBundleImpl.class);

    private static final String BUNDLE_LOCATION_PREFIX = "ConnectorModuleMetatype@";
    private static final String MANIFEST_API_TYPE_VISIBILITY_KEY = "IBM-ApiTypeVisibility";
    private static final String MANIFEST_DEFAULT_CONFIG_KEY = "IBM-Default-Config";

    private final BundleContext bundleContext;
    private final ConnectorModuleInfo cmInfo;
    private final ConnectorModuleMetaDataImpl metadataImpl;
    private final String id;
    private final ConcurrentHashMap<String, String> bootstrapContextFactoryPids;

    private Metatype metatype;
    private String bootstrapContextAlias;
    private String bootstrapContextFactoryPid;
    private Boolean autoStart;
    private List<String> factoryPids;
    private String defaultInstance;
    private String metatypeXML;
    private Bundle metatypeBundle;
    private ServiceRegistration<?> registration;
    private String rarFileName;
    private Collection<URL> urls;

    public ConnectorModuleMetatypeBundleImpl(BundleContext bundleContext, ConnectorModuleInfo cmInfo,
                                             WSConfigurationHelper configurationHelper,
                                             ConcurrentHashMap<String, String> bootstrapContextFactoryPids,
                                             ConcurrentHashMap<String, CountDownLatch> metatypeRemovedLatches) {
        this.bundleContext = bundleContext;
        this.cmInfo = cmInfo;
        this.metadataImpl = (ConnectorModuleMetaDataImpl) ((ExtendedModuleInfo) cmInfo).getMetaData();
        this.id = metadataImpl.getIdentifier();
        this.bootstrapContextFactoryPids = bootstrapContextFactoryPids;
    }

    @Override
    public void generateMetatype() throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        ClassLoader raClassLoader = cmInfo.getClassLoader();
        this.rarFileName = cmInfo.getURI();
        this.urls = cmInfo.getContainer().getURLs();
        //configurationHelper

        Map<String, Object> metagenConfig = metadataImpl.getMetaGenConfig();
        metagenConfig.put(MetaGenConstants.KEY_RAR_CLASSLOADER, raClassLoader);

        MetaTypeFactory mtpService = PrivHelper.getService(bundleContext, MetaTypeFactory.class);
        metatype = MetatypeGenerator.generateMetatype(metagenConfig, mtpService);
        metatypeXML = metatype.toMetatypeString(true);

        List<MetatypeOcd> ocds = metatype.getOcds();
        factoryPids = new ArrayList<String>(ocds.size());
        bootstrapContextFactoryPid = null;
        for (MetatypeOcd ocd : ocds) {
            String pid = ocd.getId();
            factoryPids.add(pid);
            if (pid.charAt(15) == 'r') { // com.ibm.ws.jca.resourceAdapter.properties.*
                bootstrapContextFactoryPid = pid;
                bootstrapContextAlias = ocd.getChildAlias();
            }
        }

        autoStart = metadataImpl.getAutoStart();
        if (autoStart == null)
            // Default to auto start only if there are no resources (not the resource adapter config itself) that could trigger lazy start
            autoStart = ocds.size() <= 1;

        // Disallow duplicates (case insensitive)
        String previousValue = bootstrapContextFactoryPids.putIfAbsent(id.toUpperCase(), bootstrapContextFactoryPid);
        if (previousValue != null)
            throw new StateChangeException(Tr.formatMessage(tc, "J2CA8815.duplicate.resource.adapter.id", id));

        // Create the default configuration for the resource adapter config singleton
        try {
            defaultInstance = metadataImpl.getDefaultInstancesXML(bootstrapContextAlias);
            if (defaultInstance == null) {
                defaultInstance = "<server>\n  <" + bootstrapContextFactoryPid + "/>\n</server>";
            }

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "[" + id + "] defaultInstance " + defaultInstance);
        } catch (Exception x) {
            bootstrapContextFactoryPids.remove(id.toUpperCase(), bootstrapContextFactoryPid);
            throw new StateChangeException(x);
        }
    }

    /**
     * Specify the packages to be imported dynamically into all resource adapters
     */
    private static final List<String> DYNAMIC_IMPORT_PACKAGE_LIST = Collections.unmodifiableList(Arrays.asList("*"));

    private static final EnumSet<ApiType> DEFAULT_API_TYPES = EnumSet.of(SPEC, IBMAPI, API, STABLE);

    private static String getSymbolicName(String bundleId) {
        return String.format("connector.module.metatype.bundle.ConnectorModule.%s", bundleId);
    }

    private void setStartLevel(Bundle b) {
        FrameworkWiring frameworkWiring = bundleContext.getBundle(org.osgi.framework.Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class);
        FrameworkStartLevel fsl = frameworkWiring.getBundle().adapt(FrameworkStartLevel.class);
        BundleStartLevel bsl = b.adapt(BundleStartLevel.class);
        int currentStartLevel = fsl.getStartLevel();
        int neededStartLevel = bsl.getStartLevel();
        if (neededStartLevel > currentStartLevel) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "Changing the start level of bundle {0} from {1} to the current level of {2}", b, neededStartLevel, currentStartLevel);
            bsl.setStartLevel(currentStartLevel);
        }
    }

    private void start(Bundle b) throws BundleException {
        BundleException resolverException = null;
        for (int i = 0; i < 2; i++) {
            resolverException = null;
            try {
                b.start();
                return;
            } catch (BundleException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "An exception occurred while starting bundle {0}: {1}", b, e);
                }
                if (e.getType() == BundleException.RESOLVE_ERROR) {
                    // Failed to resolve;
                    // typically the bundle exception message will have some useful resolver error information
                    resolverException = e;
                }
            }
        }
        if (resolverException != null) {
            throw resolverException;
        }
    }

    @Override
    public void registerMetatype() throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        ApplicationInfo appInfo = cmInfo.getApplicationInfo();
        String appName = appInfo.getName();
        String moduleName = cmInfo.getName();
        String defaultConfig = "OSGI-INF/wlp/defaultInstances.xml";
        if (bootstrapContextAlias != null && metadataImpl.hasConfig())
            defaultConfig += "; requireExisting=true";

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "synthetic bundle info:",
                     bootstrapContextAlias, metadataImpl.hasConfig(), MANIFEST_DEFAULT_CONFIG_KEY + "=" + defaultConfig, defaultInstance);

        metatypeBundle = new BundleFactory().setBundleName("ConnectorModuleMetatype bundle for " + appName + "-" + moduleName)
                        // TODO call setBundleVersion with some more appropriate value ?
                        .setBundleSymbolicName(getSymbolicName(id)).dynamicallyImportPackages(DYNAMIC_IMPORT_PACKAGE_LIST).addManifestAttribute(MANIFEST_API_TYPE_VISIBILITY_KEY,
                                                                                                                                                DEFAULT_API_TYPES).addManifestAttribute(MANIFEST_DEFAULT_CONFIG_KEY,
                                                                                                                                                                                        Arrays.asList(defaultConfig)).setBundleLocationPrefix(BUNDLE_LOCATION_PREFIX).setBundleLocation("ConnectorModule:"
                                                                                                                                                                                                                                                                                        + id).setBundleContext(bundleContext).setDefaultInstance(defaultInstance).setMetatypeXML(metatypeXML).createBundle();

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "[" + id + "] bundle " + metatypeBundle);

        setStartLevel(metatypeBundle);
        start(metatypeBundle);

        ResourceAdapterService service = new ResourceAdapterService();
        service.setClassLoader(cmInfo.getClassLoader());
        service.setResourceAdapterMetaData((ResourceAdapterMetaData) metadataImpl.getComponentMetaDatas()[0]);
        Hashtable<String, Object> dict = new Hashtable<String, Object>();
        dict.put("id", id);
        dict.put("rarFileName", rarFileName);
        dict.put("urls", urls);
        if (metadataImpl.resourceAdapterPid != null)
            dict.put("source.pid", metadataImpl.resourceAdapterPid);
        String[] providedClassNames = new String[] { ResourceAdapterService.class.getName(),
                                                     ClassProvider.class.getName() };
        registration = bundleContext.registerService(providedClassNames, service, dict);
    }

    @Override
    @FFDCIgnore(IllegalStateException.class)
    public void removeMetatype() throws Exception {
        if (registration != null) {
            try {
                registration.unregister();
            } catch (IllegalStateException iex) {
                // Empty as its possible that the service is already unregistered as
                // part of the stopping of the jca bundle
            }
        }
        metatypeBundle.uninstall();
        bootstrapContextFactoryPids.remove(id.toUpperCase());
    }

    @Override
    public String getBootstrapContextFactoryPid() {
        return bootstrapContextFactoryPid;
    }

    @Override
    public boolean getAutoStart() {
        return autoStart.booleanValue();
    }
}
