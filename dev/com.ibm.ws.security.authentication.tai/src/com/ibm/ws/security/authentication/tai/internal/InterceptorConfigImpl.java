/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.tai.internal;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.security.authentication.tai.TAIConfig;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

/**
 * SampleTAI configuration. The interceptor can be implemented as shared library or user feature. The attribute specified in the interceptor element
 * will overwrite the one in trustAssociation element.
 *
 *      <trustAssociation id="myTrustAssociation" invokeForUnprotectedURI="false" failOverToAppAuthType="false" disableLtpaCookie="true">
 *                      <interceptors id="simpleTAI" enabled="true" className="com.ibm.websphere.security.sample.SimpleTAI"
 *                                              invokeBeforeSSO="true" invokeAfterSSO="false" disableLtpaCookie="true" libraryRef="simpleTAI">
 *                                      <properties hostName="machine1" application="test1"/>
 *                      </interceptors>
 *      </trustAssociation>
 *
 *      <library id="simpleTAI">
 *              <fileset dir="${server.config.dir}" includes="simpleTAI.jar"/>
 *      </library>
 **/

/**
 * This class will process the interceptor elemement configuration for shared library, load and initialize the shared library TAI.
 **/
public class InterceptorConfigImpl implements TrustAssociationInterceptor, ConfigurationListener {
    private static final TraceComponent tc = Tr.register(InterceptorConfigImpl.class);

    static final String KEY_ID = "id";
    static final String KEY_CLASS_NAME = "className";
    static final String KEY_ENABLED = "enabled";
    static final String CFG_KEY_PROPERTIES_PID = "propertiesRef";

    static final String KEY_CONFIGURATION_ADMIN = "configurationAdmin";
    private final AtomicServiceReference<ConfigurationAdmin> configAdminRef = new AtomicServiceReference<ConfigurationAdmin>(KEY_CONFIGURATION_ADMIN);

    private String id = null;
    private String className = null;
    private boolean invokeBeforeSSO = false;
    private boolean invokeAfterSSO = false;
    private boolean disableLtpaCookie = false;
    private TrustAssociationInterceptor interceptorInstance = null;
    private final Properties properties = new Properties();

    /** The required shared library */
    private volatile Library sharedLibrary = null;

    private volatile String pid;

    private volatile TAIServiceImpl taiService = null;

    private String bundleLocation;

    public InterceptorConfigImpl() {}

    public InterceptorConfigImpl(Map<String, Object> props) {
        processConfig(props);
    }

    protected void activate(ComponentContext cc, Map<String, Object> props) {
        configAdminRef.activate(cc);
        this.bundleLocation = cc.getBundleContext().getBundle().getLocation();
        processConfig(props);
    }

    protected void modified(Map<String, Object> props) {
        processConfig(props);
    }

    protected void deactivate(ComponentContext cc) {
        configAdminRef.deactivate(cc);
    }

    protected void setConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        configAdminRef.setReference(ref);
    }

    protected void unsetConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        configAdminRef.unsetReference(ref);
    }

    /** Set required service, will be called before activate */
    protected void setSharedLib(Library svc) {
        sharedLibrary = svc;
    }

    /** This is a required service, unset will be called after deactivate */
    protected void unsetSharedLib(Library ref) {}

    /**
     * @param props
     */
    private void processConfig(Map<String, Object> props) {
        if (props == null || props.isEmpty())
            return;
        if ((Boolean) props.get(KEY_ENABLED)) {
            id = (String) props.get(KEY_ID);
            className = (String) props.get(KEY_CLASS_NAME);
            invokeBeforeSSO = (Boolean) props.get(TAIConfig.KEY_INVOKE_BEFORE_SSO);
            invokeAfterSSO = (Boolean) props.get(TAIConfig.KEY_INVOKE_AFTER_SSO);
            disableLtpaCookie = (Boolean) props.get(TAIConfig.KEY_DISABLE_LTPA_COOKIE);
            pid = (String) props.get(CFG_KEY_PROPERTIES_PID);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Shared library interceptor config:  ");
                Tr.debug(tc, "  invokeBeforeSSO=" + invokeBeforeSSO + " invokeAfterSSO=" + invokeAfterSSO + " disableLtpaCookie=" + disableLtpaCookie);
            }
            processProperties();
            initInterceptor();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Skipping TAI that is not enable: " + props);
            }
        }
    }

    private void processProperties() {
        properties.clear();

        if (pid == null)
            return;

        ConfigurationAdmin configAdmin = configAdminRef.getServiceWithException();

        Configuration config = null;
        try {
            // We do not want to create a missing pid, only find one that we were told exists
            Configuration[] configList = configAdmin.listConfigurations(FilterUtils.createPropertyFilter("service.pid", pid));
            if (configList != null && configList.length > 0) {
                //bind the config to this bundle so no one else can steal it
                config = configAdmin.getConfiguration(pid, bundleLocation);
            }
        } catch (InvalidSyntaxException e) {
        } catch (IOException e) {
        }

        if (config != null) {
            // Just get the first one (there should only ever be one.. )
            Dictionary<String, ?> cProps = config.getProperties();
            Enumeration<String> keys = cProps.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                // Skip certain keys
                if (key.startsWith(".")
                    || key.startsWith("config.")
                    || key.startsWith("service.")
                    || key.equals("id")) {
                    continue;
                }
                properties.put(key, cProps.get(key));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Effective share library interceptor properties: " + properties.toString());
        }

    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    public boolean isInvokeBeforeSSO() {
        return invokeBeforeSSO;
    }

    /** {@inheritDoc} */
    public boolean isInvokeAfterSSO() {
        return invokeAfterSSO;
    }

    /** {@inheritDoc} */
    public boolean isDisableLtpaCookie() {
        return disableLtpaCookie;
    }

    /** {@inheritDoc} */
    public Properties getProperties() {
        return properties;
    }

    private void initInterceptor() {
        try {
            interceptorInstance = loadInterceptor();
            int initResult = interceptorInstance.initialize(properties);
            if (initResult == 0) {
                Tr.info(tc, "SEC_TAI_INIT_SIGNATURE", className);
            } else {
                Tr.error(tc, "SEC_TAI_INIT_CLASS_LOAD_ERROR", initResult);
            }
        } catch (Exception e) {
            Tr.error(tc, "SEC_TAI_INIT_CLASS_LOAD_ERROR", e.getMessage());
        }
//        finally {
//            if (taiService != null) {
//                taiService.initTAIs();
//            }
//        }
    }

    private TrustAssociationInterceptor loadInterceptor() throws Exception {
        Tr.info(tc, "SEC_TAI_LOAD_INIT", id);
        ClassLoader sharedLibClassLoader = sharedLibrary.getClassLoader();
        TrustAssociationInterceptor tai = null;
        try {
            Class<?> myClass = sharedLibClassLoader.loadClass(className);
            tai = (TrustAssociationInterceptor) myClass.newInstance();
        } catch (Exception e) {
            throw e;
        }
        return tai;
    }

    public TrustAssociationInterceptor getInterceptorInstance(TAIServiceImpl taiServiceImpl) {
        taiService = taiServiceImpl;
        return interceptorInstance;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.security.tai.TrustAssociationInterceptor#isTargetInterceptor(javax.servlet.http.HttpServletRequest)
     */
    @Override
    public boolean isTargetInterceptor(HttpServletRequest req) throws WebTrustAssociationException {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.security.tai.TrustAssociationInterceptor#negotiateValidateandEstablishTrust(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public TAIResult negotiateValidateandEstablishTrust(HttpServletRequest req, HttpServletResponse res) throws WebTrustAssociationFailedException {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.security.tai.TrustAssociationInterceptor#initialize(java.util.Properties)
     */
    @Override
    public int initialize(Properties props) throws WebTrustAssociationFailedException {
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.security.tai.TrustAssociationInterceptor#getVersion()
     */
    @Override
    public String getVersion() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.security.tai.TrustAssociationInterceptor#getType()
     */
    @Override
    public String getType() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.security.tai.TrustAssociationInterceptor#cleanup()
     */
    @Override
    public void cleanup() {}

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.cm.ConfigurationListener#configurationEvent(org.osgi.service.cm.ConfigurationEvent)
     */
    @Override
    public void configurationEvent(ConfigurationEvent event) {
        if (event.getType() == ConfigurationEvent.CM_UPDATED && event.getPid().equals(pid)) {
            processProperties();
            initInterceptor();
        }

    }
}
