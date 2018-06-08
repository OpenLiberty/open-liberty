/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.clientcontainer.metadata.ClientModuleMetaData;
import com.ibm.ws.container.service.app.deploy.ClientModuleInfo;
import com.ibm.ws.container.service.config.extended.RefBndAndExtHelper;
import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.ws.javaee.dd.clientbnd.ApplicationClientBnd;
import com.ibm.ws.javaee.dd.common.AdministeredObject;
import com.ibm.ws.javaee.dd.common.ConnectionFactory;
import com.ibm.ws.javaee.dd.common.DataSource;
import com.ibm.ws.javaee.dd.common.EJBRef;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.common.JMSConnectionFactory;
import com.ibm.ws.javaee.dd.common.JMSDestination;
import com.ibm.ws.javaee.dd.common.MailSession;
import com.ibm.ws.javaee.dd.common.MessageDestinationRef;
import com.ibm.ws.javaee.dd.common.PersistenceContextRef;
import com.ibm.ws.javaee.dd.common.PersistenceUnitRef;
import com.ibm.ws.javaee.dd.common.ResourceEnvRef;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.InjectionEngineAccessor;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public class ClientModuleInjection {
    private final ClientModuleMetaData cmmd;
    private final ApplicationClient appClient;
    private final ApplicationClientBnd appClientBnd;
    private final ResourceRefConfigFactory resourceRefConfigFactory;
    private final ClientModuleInfo moduleInfo;
    private final InjectionEngine injectionEngine;
    private final AtomicServiceReference<ManagedObjectService> managedObjectServiceRef;
    private final CallbackHandler callbackHandler;
    private Class<?> mainClass;
    public Exception invokeException;
    private final boolean runningInClient;

    private static final TraceComponent tc = Tr.register(ClientModuleInjection.class, null, null);

    public ClientModuleInjection(ClientModuleMetaData cmmd, ApplicationClientBnd appClientBnd, ResourceRefConfigFactory resourceRefConfigFactory,
                                 InjectionEngine injectionEngine, AtomicServiceReference<ManagedObjectService> managedObjectServiceRef,
                                 CallbackHandler callbackHandler, boolean runningInClient) {
        this.cmmd = cmmd;
        appClient = cmmd.getAppClient();
        this.appClientBnd = appClientBnd;
        this.resourceRefConfigFactory = resourceRefConfigFactory;
        this.injectionEngine = injectionEngine;
        this.managedObjectServiceRef = managedObjectServiceRef;
        this.callbackHandler = callbackHandler;
        moduleInfo = (ClientModuleInfo) cmmd.getModuleInfo();
        this.runningInClient = runningInClient;
    }

    /**
     * @return the application client jar file's main class,
     *         which has been loaded by the class loader.
     * @throws ClassNotFoundException
     */
    public Class<?> getMainClass() throws ClassNotFoundException {
        if (mainClass == null) {
            ClassLoader cl = moduleInfo.getClassLoader();
            mainClass = cl.loadClass(getMainClassName());
        }
        return mainClass;
    }

    private String getMainClassName() {
        return moduleInfo.getMainClassName();
    }

    public void processReferences() throws ClassNotFoundException, InjectionException {

        HashMap<Class<?>, InjectionTarget[]> cookies;
        cookies = getInjectionCookies();
        if (runningInClient) {
            inject(getMainClass(), null, cookies);
        }
        if (callbackHandler != null) {
            inject(callbackHandler.getClass(), callbackHandler, cookies);
        }
    }

    @SuppressWarnings("rawtypes")
    private ManagedObject getManagedObject(final Object instance) throws Exception {
        ManagedObjectFactory<Object> mof = (ManagedObjectFactory<Object>) managedObjectServiceRef.getServiceWithException().createManagedObjectFactory(cmmd, instance.getClass(),
                                                                                                                                                       false);
        final ManagedObject mo = mof.createManagedObject(instance, null);
        return mo;
    }

    private InjectionTargetContext getInjectionTargetContext(Object instance) throws InjectionException {
        final ManagedObject<?> mo;
        try {
            mo = getManagedObject(instance);
            if (mo == null) {
                return null;
            }
        } catch (Exception e) {
            throw new InjectionException(e.getCause());
        }
        InjectionTargetContext itc = new InjectionTargetContext() {
            @Override
            public <T> T getInjectionTargetContextData(Class<T> data) {
                return mo.getContextData(data);
            }
        };
        return itc;
    }

    // 'obj' is a class and 'instance' is an instance of that class.
    // 'instance' is considered as the first injection target but may be null in which case injection is done on 'obj'.
    @Trivial
    private void inject(Object obj, final Object instance, HashMap<Class<?>, InjectionTarget[]> cookies) throws InjectionException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "inject", "obj --> [" + obj + "]");
        if (cookies.size() == 0) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "inject", "no injection cookies found");
            return;
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "inject", "injection cookies found");

        // Note that 'obj' is a class.
        if (cookies.containsKey(obj)) { // To prevent NPE from the for loop below

            if (instance != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "inject", "about to inject via managed object --> [" + obj + "]");

                final ManagedObject<?> mo;
                try {
                    mo = getManagedObject(instance);
                    mo.inject(cookies.get(obj), null);// inject callback handler
                } catch (Exception e) {
                    throw new InjectionException(e.getCause());
                }

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "inject", "injected --> [" + obj + "]");
            } else {
                for (InjectionTarget cookie : cookies.get(obj)) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "inject", "about to inject resource --> [" + cookie + "]");

                    injectionEngine.inject(obj, cookie, null); // inject main class

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "inject", "injected resource --> [" + cookie + "]");
                }
            }

        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "inject", "cookies does not contains the key --> [" + obj + "]");
        }
    }

    private HashMap<Class<?>, InjectionTarget[]> getInjectionCookies() throws InjectionException, ClassNotFoundException {
        HashMap<Class<?>, InjectionTarget[]> injectionCookies = new HashMap<Class<?>, InjectionTarget[]>();
        ComponentNameSpaceConfiguration compNSConfig = getComponentNameSpaceConfiguration();

        // Call the injection engine passing the empty cookie map and specific container information
        // used by each processor as they gather injection meta data.
        InjectionEngineAccessor.getInstance().processInjectionMetaData(injectionCookies, compNSConfig);
        return injectionCookies;
    }

    private ComponentNameSpaceConfiguration getComponentNameSpaceConfiguration() throws ClassNotFoundException {
        String displayName = "";
        J2EEName j2eeName = cmmd.getJ2EEName();
        ClassLoader classLoader = moduleInfo.getClassLoader();
        boolean metadataComplete = false;
        if (appClient.isSetMetadataComplete()) {
            metadataComplete = appClient.isMetadataComplete();
        }

        List<Class<?>> injectionClasses = new ArrayList<Class<?>>();

        // env entry and env entry values
        List<EnvEntry> envEntries = appClient.getEnvEntries();
        Map<String, String> envEntryBindings = new HashMap<String, String>();
        Map<String, String> envEntryValues = new HashMap<String, String>();
        RefBndAndExtHelper.configureEnvEntryBindings(appClientBnd, envEntryValues, envEntryBindings);

        // EJB
        List<EJBRef> ejbRefs = appClient.getEJBRefs();
        Map<String, String> ejbRefBndMap = new HashMap<String, String>();
        RefBndAndExtHelper.configureEJBRefBindings(appClientBnd, ejbRefBndMap);

        // web service
        List<ServiceRef> webServiceRefs = appClient.getServiceRefs();

        // resource
        List<ResourceRef> resourceRefs = appClient.getResourceRefs();
        ResourceRefConfigList resourceRefConfigList = resourceRefConfigFactory.createResourceRefConfigList();
        Map<String, String> resourceRefBindings = new HashMap<String, String>();
        RefBndAndExtHelper.configureResourceRefBindings(appClientBnd, resourceRefBindings, resourceRefConfigList);

        // resource env
        List<ResourceEnvRef> resourceEnvRefs = appClient.getResourceEnvRefs();
        Map<String, String> resourceEnvRefBindings = new HashMap<String, String>();
        RefBndAndExtHelper.configureResourceEnvRefBindings(appClientBnd, resourceEnvRefBindings);

        // message dest
        List<MessageDestinationRef> msgDestRefs = appClient.getMessageDestinationRefs();
        Map<String, String> msgDestValues = new HashMap<String, String>();
        RefBndAndExtHelper.configureMessageDestinationRefBindings(appClientBnd, msgDestValues);

        // persistence unit
        List<PersistenceUnitRef> persistenceUnitRefs = appClient.getPersistenceUnitRefs();

        // PersistenceContextRefs doesn't apply to Client Container
        List<PersistenceContextRef> persistenceContextRefs = null;

        // data source
        List<DataSource> dataSourceDefs = appClient.getDataSources();
        Map<String, String> dataSourceBindings = new HashMap<String, String>();
        RefBndAndExtHelper.configureDataSourceBindings(appClientBnd, dataSourceBindings);

        /*
         * New in Java EE 7: JMSConnectionFactory, JMSDestination, MailSession, J2C ConnectionFactory, and J2C AdministeredObject
         */
        List<MailSession> mailSessions = appClient.getMailSessions();
        List<JMSConnectionFactory> jmsConnectionFactories = appClient.getJMSConnectionFactories();
        List<JMSDestination> jmsDestinations = appClient.getJMSDestinations();
        List<ConnectionFactory> connectionFactories = appClient.getConnectionFactories();
        List<AdministeredObject> adminObjects = appClient.getAdministeredObjects();

        Map<String, String> mailSessionBindings = new HashMap<String, String>();
        RefBndAndExtHelper.configureResourceRefBindings(appClientBnd, mailSessionBindings, resourceRefConfigList);

        Map<String, String> jmsConnectionFactoryBindings = new HashMap<String, String>();
        RefBndAndExtHelper.configureResourceRefBindings(appClientBnd, jmsConnectionFactoryBindings, resourceRefConfigList);

        Map<String, String> jmsDestinationBindings = new HashMap<String, String>();
        RefBndAndExtHelper.configureResourceRefBindings(appClientBnd, jmsDestinationBindings, resourceRefConfigList);

        // Make an instance of List
        injectionClasses.add(getMainClass());
        if (callbackHandler != null) {
            injectionClasses.add(callbackHandler.getClass());
        }

        ComponentNameSpaceConfiguration compNSConfig = new ComponentNameSpaceConfiguration(displayName, j2eeName);
        compNSConfig.setClientContainer(true);
        compNSConfig.setClassLoader(classLoader);
        compNSConfig.setMetaDataComplete(metadataComplete);
        compNSConfig.setInjectionClasses(injectionClasses);

        compNSConfig.setEnvEntries(envEntries);
        compNSConfig.setEnvEntryBindings(envEntryBindings);
        compNSConfig.setEJBRefs(ejbRefs);
        compNSConfig.setEJBRefBindings(ejbRefBndMap);
        compNSConfig.setWebServiceRefs(webServiceRefs);
        compNSConfig.setResourceRefs(resourceRefs);
        compNSConfig.setResourceRefBindings(resourceRefBindings);
        compNSConfig.setResourceEnvRefs(resourceEnvRefs);
        compNSConfig.setResourceEnvRefBindings(resourceEnvRefBindings);
        compNSConfig.setMsgDestRefs(msgDestRefs);
        compNSConfig.setMsgDestRefBindings(msgDestValues);
        compNSConfig.setPersistenceUnitRefs(persistenceUnitRefs);
        compNSConfig.setPersistenceContextRefs(persistenceContextRefs);
        compNSConfig.setEnvEntryValues(envEntryValues);
        compNSConfig.setDataSourceDefinitions(dataSourceDefs);
        compNSConfig.setDataSourceDefinitionBindings(dataSourceBindings);

        /*
         * New in Java EE 7: JMSConnectionFactory, JMSDestination, MailSession, J2C ConnectionFactory, and J2C AdministeredObject
         */
        compNSConfig.setJNDIEnvironmentRefs(JMSConnectionFactory.class, jmsConnectionFactories);
        compNSConfig.setJNDIEnvironmentRefs(JMSDestination.class, jmsDestinations);
        compNSConfig.setConnectionFactoryDefinitions(connectionFactories);
        compNSConfig.setJNDIEnvironmentRefs(MailSession.class, mailSessions);
        compNSConfig.setAdministeredObjectDefinitions(adminObjects);

        compNSConfig.setJNDIEnvironmentRefBindings(JMSConnectionFactory.class, jmsConnectionFactoryBindings);
        compNSConfig.setJNDIEnvironmentRefBindings(JMSDestination.class, jmsDestinationBindings);
        compNSConfig.setJNDIEnvironmentRefBindings(MailSession.class, mailSessionBindings);

        compNSConfig.setModuleMetaData(cmmd);

        return compNSConfig;
    }

    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }
}
