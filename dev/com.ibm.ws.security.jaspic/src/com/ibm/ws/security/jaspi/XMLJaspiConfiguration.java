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

package com.ibm.ws.security.jaspi;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.message.config.AuthConfigFactory;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.security.jaspi.client.JaspiConfig;
import com.ibm.ws.security.jaspi.client.JaspiProvider;
import com.ibm.ws.security.jaspi.client.ObjectFactory;
import com.ibm.ws.security.jaspi.client.Option;

/**
 * This class was created because the CTS AuthConfigFactoryVerifyPersistence testcase fails
 * and we need to store persistent providers
 * 
 * An instance of this class is created when an instance of {@link ProviderRegistry} is created.
 * <p>It reads/stores representations of JASPI registrations from/into a file in XML
 * format. The name and location of the file can be specified with a property that
 * will be automatically set for client applications when they are launched using a
 * WAS launch script, e.g. launchClient.sh.
 * <p>In WAS client processes, the system property "com.ibm.websphere.jaspi.configuration"
 * is defined in a properties file used by WAS. The file is
 * cc_launcher.properties in <WAS_HOME>/lib/launchclient.jar. If the property is not
 * defined, the file location and name will be "./jaspiConfiguration.xml".
 * <p>In WAS server processes, the security configuration property
 * "com.ibm.websphere.jaspi.configuration" in security.xml is used to specify file name.
 * This property is intentionally not defined by default in security.xml because we do
 * not want to encourage use of such persistent configuration.
 * <p>The schema and the Java model for the XML representation of the registrations
 * was generated using JAXB. The generated Java classes are {@link ObjectFactory}, {@link JaspiConfig}, {@link JaspiProvider} and {@link Option}. The schema is in
 * security.impl/schemas/client_jaspi_config.xsd.
 * 
 * @author IBM Corp.
 * 
 */
public class XMLJaspiConfiguration implements PersistenceManager {

    private static final TraceComponent tc = Tr.register(XMLJaspiConfiguration.class, "Security", null);

    private AuthConfigFactory registry;
    //private SecurityConfig securityCfg;
    private File configFile;
    private JaspiConfig jaspiConfig;

    public XMLJaspiConfiguration() {
        super();
        jaspiConfig = new ObjectFactory().createJaspiConfig();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.jaspi.client.PersistenceManager#setAuthConfigFactory(javax.security.auth.message.config.AuthConfigFactory)
     */
    @Override
    public void setAuthConfigFactory(AuthConfigFactory factory) {
        this.registry = factory;
        if (tc.isDebugEnabled())
            Tr.debug(tc, "AuthConfigFactory = " + factory);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.jaspi.client.PersistenceManager#getAuthConfigFactory()
     */
    @Override
    public AuthConfigFactory getAuthConfigFactory() {
        return registry;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.jaspi.client.PersistenceManager#getFile()
     */
    @Override
    public File getFile() {
        return configFile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.jaspi.client.PersistenceManager#setFile(java.io.File)
     */
    @Override
    public void setFile(File file) {
        configFile = file;
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Jaspi configuration of persistent providers will be stored in file: " + file);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.jaspi.client.PersistenceManager#load()
     */
    /**
     * Reads the file specified with system property "com.ibm.websphere.jaspi.configuration" and registers all registrations found
     * in the file in ProviderRegistry which is our implementation of AuthConfigFactory.
     * The file will be read/written only when running in a client process. In a server process, JASPI registrations are registered
     * using configuration information read from security.xml and from ibm-application-bnd/ibm-web-bnd binding files.
     * 
     * @throws RuntimeException if an error is encountered when loading/storing the configuration file.
     * 
     */
    @Override
    public void load() {
        if (isEnabled())
            try {
                if (configFile != null && configFile.exists()) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "persistent config file found: " + configFile);
                    jaspiConfig = readConfigFile(configFile);
                    registerPersistentProviders();
                }
                else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "persistent config file not found: " + configFile + ", creating new file");
                    jaspiConfig = new JaspiConfig();
                }
            } catch (PrivilegedActionException e) {
                FFDCFilter.processException(e, this.getClass().getName() + ".load", "143", this);
                throw new RuntimeException("Unable to load " + configFile, e);
            }
    }

    @Override
    public void registerProvider(String className, Map<String, String> properties, String layer, String appContext, String description) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "registerProvider",
                     new Object[] { "className=" + className, "msgLayer=" + layer, "appContext=" + appContext, "description=" + description, "properties=" + properties });
        if (isEnabled()) {
            String providerName = layer + "_" + (appContext == null ? null : appContext.replace(' ', '_'));
            JaspiProvider provider = getJaspiProvider(layer, appContext);
            if (provider == null) {
                provider = new ObjectFactory().createJaspiProvider();
                jaspiConfig.getJaspiProvider().add(provider);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "A new provider will be added in " + configFile);
            }
            provider.setProviderName(providerName);
            provider.setClassName(className);
            provider.setMsgLayer(layer);
            provider.setAppContext(appContext);
            provider.setDescription(description);
            setProperties(provider, properties);
            writeConfigFile(jaspiConfig);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "registerProvider");
    }

    @Override
    public void removeProvider(String layer, String appContext) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeProvider", new Object[] { "msgLayer=" + layer, "appContext=" + appContext });
        if (isEnabled()) {
            JaspiProvider provider = getJaspiProvider(layer, appContext);
            if (provider != null) {
                jaspiConfig.getJaspiProvider().remove(provider);
                writeConfigFile(jaspiConfig);
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeProvider");
    }

    @Override
    public JaspiProvider getJaspiProvider(String layer, String appContext) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getJaspiProvider", new Object[] { "layer=" + layer, "appContext=" + appContext });
        JaspiProvider provider = null;
        if (jaspiConfig != null) {
            for (JaspiProvider p : jaspiConfig.getJaspiProvider()) {
                String pLayer = p.getMsgLayer();
                String pCtx = p.getAppContext();
                boolean isSameLayer = (layer != null && layer.equals(pLayer)) || (layer == null && pLayer == null);
                boolean isSameCtx = (appContext != null && appContext.equals(pCtx)) || (appContext == null && pCtx == null);
                if (isSameLayer && isSameCtx) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Found a matching provider",
                                 new Object[] { "className=" + p.getClassName(), "description=" + p.getDescription(), "properties=" + convertOptionsToMap(p.getOption()) });
                    provider = p;
                    break;
                }
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getJaspiProvider", provider);
        return provider;
    }

    private boolean isEnabled() {
        return true;
    }

    /**
     * Register in AuthConfigFactory all persistent registrations found in the JASPI configuration file.
     * <p>This method assumes the registrations have already been loaded from the file into the Java in-memory objects when the
     * registration was created in method {@link #registerProvider(String, Map, String, String, String)}.
     */
    private void registerPersistentProviders() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "registerPersistentProviders");
        if (jaspiConfig != null)
            for (JaspiProvider p : jaspiConfig.getJaspiProvider()) {
                String className = p.getClassName();
                String desc = p.getDescription();
                String layer = p.getMsgLayer();
                String appContext = p.getAppContext();
                Map<String, String> props = convertOptionsToMap(p.getOption());
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Register persistent provider",
                             new Object[] { "className=" + className, "msgLayer=" + layer, "appContext=" + appContext, "description=" + desc, "properties=" + props });
                if (registry != null) {
                    registry.registerConfigProvider(className, props, layer, appContext, desc);
                }
            }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "registerPersistentProviders");
    }

    /**
     * Return a Java representation of the JASPI persistent providers that are defined in the given configuration file or null
     * if the object returned by JAXB is not an JaspiConfig instance or an exception is thrown by method AccessController.doPrivileged.
     * 
     * @param configFile
     * @return
     * @throws PrivilegedActionException
     */
    synchronized private JaspiConfig readConfigFile(final File configFile) throws PrivilegedActionException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "readConfigFile", new Object[] { configFile });
        if (configFile == null) {
            // TODO handle persistence
            // String msg = MessageFormatHelper.getFormattedMessage(msgBundle, AdminConstants.MSG_JASPI_PERSISTENT_FILE, new Object[] { PersistenceManager.JASPI_CONFIG });
            // throw new RuntimeException(msg);
        }
        PrivilegedExceptionAction<JaspiConfig> unmarshalFile = new PrivilegedExceptionAction<JaspiConfig>() {

            @Override
            public JaspiConfig run() throws Exception {
                JaspiConfig cfg = null;
                JAXBContext jc = JAXBContext.newInstance(JaspiConfig.class);
                Object obj = jc.createUnmarshaller().unmarshal(configFile);
                if (obj instanceof JaspiConfig) {
                    cfg = (JaspiConfig) obj;
                }
                return cfg;
            }

        };
        JaspiConfig jaspi = AccessController.doPrivileged(unmarshalFile);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "readConfigFile", jaspi);
        return jaspi;
    }

    /**
     * Store the in-memory Java representation of the JASPI persistent providers into the given configuration file.
     * 
     * @param jaspiConfig
     * @throws RuntimeException if an exception occurs in method AccessController.doPrivileged.
     */
    synchronized private void writeConfigFile(final JaspiConfig jaspiConfig) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "writeConfigFile", new Object[] { jaspiConfig });
        if (configFile == null) {
            // TODO handle persistence
            //String msg = MessageFormatHelper.getFormattedMessage(msgBundle, AdminConstants.MSG_JASPI_PERSISTENT_FILE, new Object[] { PersistenceManager.JASPI_CONFIG });
            //throw new RuntimeException(msg);
        }
        PrivilegedExceptionAction<Object> marshalFile = new PrivilegedExceptionAction<Object>() {

            @Override
            public Object run() throws Exception {
                JAXBContext jc = JAXBContext.newInstance(JaspiConfig.class);
                Marshaller writer = jc.createMarshaller();
                writer.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                writer.marshal(jaspiConfig, configFile);
                return null;
            }
        };
        try {
            AccessController.doPrivileged(marshalFile);
        } catch (PrivilegedActionException e) {
            FFDCFilter.processException(e, this.getClass().getName() + ".writeConfigFile", "290", this);
            throw new RuntimeException("Unable to write " + configFile, e);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "writeConfigFile");
    }

    private Map<String, String> convertOptionsToMap(List<Option> opts) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "convertOptionsToMap", new Object[] { opts });
        Map<String, String> properties = new HashMap<String, String>();
        if (opts != null)
            for (Option opt : opts) {
                properties.put(opt.getName(), opt.getValue());
            }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "convertOptionsToMap", properties);
        return properties;
    }

    private void setProperties(JaspiProvider provider, Map<String, String> properties) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setProperties", new Object[] { provider, properties });
        List<Option> options = provider.getOption();
        options.clear();
        if (properties != null) {
            Set<String> keys = properties.keySet();
            for (Object obj : keys) {
                if (obj instanceof String) {
                    String key = (String) obj;
                    String value = properties.get(key);
                    Option prop = new ObjectFactory().createOption();
                    options.add(prop);
                    prop.setName(key);
                    prop.setValue(value);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Setting provider propperty key=" + key + ", value=" + value);
                }
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "setProperties");
    }
}
