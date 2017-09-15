/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.processor.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.ControllableRegistrationService;
import com.ibm.ws.sib.admin.DestinationAliasDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.DestinationForeignDefinition;
import com.ibm.ws.sib.admin.ExtendedBoolean;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsEObject;
import com.ibm.ws.sib.admin.JsEngineComponent;
import com.ibm.ws.sib.admin.JsEngineComponentWithEventListener;
import com.ibm.ws.sib.admin.JsHealthState;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.LWMConfig;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.admin.MQLocalizationDefinition;
import com.ibm.ws.sib.admin.RuntimeEventListener;
import com.ibm.ws.sib.admin.SIBExceptionBase;
import com.ibm.ws.sib.admin.SIBExceptionDestinationNotFound;
import com.ibm.ws.sib.admin.VirtualLinkDefinition;
import com.ibm.ws.sib.admin.internal.JsAdminFactory;
import com.ibm.ws.sib.msgstore.Configuration;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.processor.Administrator;
import com.ibm.ws.sib.processor.SIMPAdmin;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.test.utils.ComponentList;
import com.ibm.ws.sib.processor.test.utils.DestinationInformation;
import com.ibm.ws.sib.processor.test.utils.LinkInformation;
import com.ibm.ws.sib.processor.test.utils.MPBus;
import com.ibm.ws.sib.processor.test.utils.UT_SIMPUtils;
import com.ibm.ws.sib.trm.TrmMeMain;
import com.ibm.ws.sib.unittest.UnitTestConstants;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.util.ThreadPool;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.SICoreConnectionFactory;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * This class was copied from JsStandaloneEngineImpl.
 * 
 * We are using this class because admin are extremely unwilling to update
 * JsStandaloneEngineImpl to cater for requirements (in this case the ability to
 * control message store persistence recovery for warm restart tests.
 * 
 * This is a temporary measure and it should be easy to revert to admin's
 * standalone in the future.
 */
public class SIMPJsStandaloneEngineImpl implements SIMPJsStandaloneEngine {
    // Vector of the classes which are bootstrapped
    // 174199.2.8
    private final Vector<ComponentList> jmeComponents = new Vector<ComponentList>();

    // The name of the bus to which this ME belongs
    private final String _busName;

    // The name of the bus to which this ME belongs
    private final String _name;

    // Our UUID
    private SIBUuid8 _uuid;

    // Is event notification enabled
    private boolean _isEventNotificationEnabled = false;

    private static HashMap<String, BaseDestinationDefinition> aliasDestinationsByName;
    private static HashMap<String, BaseDestinationDefinition> aliasDestinationsByUuid;
    private static HashMap<String, BaseDestinationDefinition> aliasWCCMDestinationsByName = new HashMap<String, BaseDestinationDefinition>();
    private static HashMap<String, BaseDestinationDefinition> aliasWCCMDestinationsByUuid = new HashMap<String, BaseDestinationDefinition>();

    private static HashMap<String, BaseDestinationDefinition> foreignDestinationsByName;
    private static HashMap<String, BaseDestinationDefinition> foreignDestinationsByUuid;

    // Local Destination definitions
    private static HashMap<String, DestinationInformation> localDestinationsByUuid = new HashMap<String, DestinationInformation>();
    private static HashMap<String, DestinationInformation> localDestinationsByName = new HashMap<String, DestinationInformation>();

    // Remote Destination definitions to use to create destination entries in WCCM
    // Sim
    private static HashMap<String, DestinationInformation> remoteDestinationsByName = new HashMap<String, DestinationInformation>();
    private static HashMap<String, DestinationInformation> remoteDestinationsByUuid = new HashMap<String, DestinationInformation>();

    // Local link definitions
    private static HashMap<String, LinkInformation> localLinksByUuid = new HashMap<String, LinkInformation>();

    // Remote Link definitions to use to create destination entries in WCCM Sim
    private static HashMap<String, VirtualLinkDefinition> linksByName = new HashMap<String, VirtualLinkDefinition>();

    private HashSet<String> mqServerSet = new HashSet<String>();

    public static boolean readProperties = false;
    private DestinationDefinition defaultExceptionDestDef;

    // The Message Processor object created by the instance of this class
    private JsEngineComponent _messageProcessor;

    private JsEngineComponent _wsrmEngineComponent;

    private TrmMeMain cachedTRM;

    // The Message Store object created by the instance of this class
    private MessageStore _messageStore;

    // message store state strings
    private static final String STATE_UNINITIALIZED = "Uninitialized";
    private static final String STATE_STARTED = "Started";

    // Local bus object
    private MPBus mpBus = null;

    private ControllableRegistrationService regService;

    // reasonable default values for exception destination queue
    // limits for these unit tests
    private static final int exceptionDestDefaultQueueLimits = 10000;
    private int exceptionDestHightLimit = exceptionDestDefaultQueueLimits;
    private int exceptionDestLowLimit = exceptionDestDefaultQueueLimits;

    private static ThreadPool _mediationThreadPool;

    public static String RECON_DEST_FILENAME =
                    System.getProperty("user.dir")
                                    + System.getProperty("file.separator")
                                    + "reconciliation.destinations";

    /**
     * A reference to the currently active instance of this class. Only one
     * instance of this cass is really "active" at any one time, but the mediation
     * needs to be able to use this to get a connection factory class.
     */
    private static SIMPJsStandaloneEngineImpl _currentActiveStandaloneEngineImpl = null;

    /**
     * @see java.lang.Object#Object()
     */
    public SIMPJsStandaloneEngineImpl(String busName, String name) {
        _busName = busName;
        _name = name;

        if (Boolean.getBoolean("js.test.createmeuuid"))
            _uuid = new SIBUuid8();
        else
            _uuid = new SIBUuid8("AAAAAAAAAAAAAAAA");

        resetConfig();
        reset();

        _currentActiveStandaloneEngineImpl = this;

        mpBus = new MPBus(this);
    }

    @Override
    public synchronized JsEngineComponent getTRM() {
        return cachedTRM;
    }

    @Override
    public synchronized void setTRM(TrmMeMain trm) {
        // _trm = trm;
        cachedTRM = trm;

        Enumeration<ComponentList> vEnum = jmeComponents.elements();
        while (vEnum.hasMoreElements()) {
            Object c = vEnum.nextElement();
            if (((ComponentList) c).getClassName() == JsConstants.SIB_CLASS_TO_ENGINE) {
                jmeComponents.remove(c);
            }
        }

    }

    @Override
    public synchronized MessageStore createMessageStoreOnly(boolean clean) throws Exception {
        // Instantiate the message store for my messaging engine
        if (_messageStore != null) {
            String state = _messageStore.toString();
            if (clean || !state.contains(STATE_STARTED)) {
                if (!state.contains(STATE_UNINITIALIZED)) {
                    try {
                        _messageStore.stop(JsConstants.ME_STOP_IMMEDIATE);
                    } catch (RuntimeException e) {
                        // Ignore any errors as there is no way to determine if the message
                        // store has
                        // started previously.
                    }
                }
                _messageStore = null;
            }
        }

        if (_messageStore == null) {
            _messageStore = MessageStore.createInstance();

            Configuration configuration = Configuration.createBasicConfiguration();
            configuration.setCleanPersistenceOnStart(clean);
            configuration.getDatasourceProperties().setProperty("databaseName",
                                                                System.getProperty("js.test.dbname", "msdb"));
            if (System.getProperty("js.test.dbAttributes") != null)
                configuration.getDatasourceProperties().setProperty(
                                                                    "connectionAttributes", System.getProperty("js.test.dbAttributes"));
            configuration.setObjectManagerMaximumPermanentStoreSize(41943040);
            configuration.setObjectManagerMinimumPermanentStoreSize(41943040);
            configuration.setObjectManagerMaximumTemporaryStoreSize(41943040);
            configuration.setObjectManagerMinimumTemporaryStoreSize(41943040);
            configuration.setObjectManagerLogSize(20043040);

            configuration
                            .setPersistentMessageStoreClassname(UnitTestConstants.USE_DB_CLASS);

            _messageStore.initialize(configuration);
            _messageStore.start(JsConstants.ME_START_DEFAULT);
        }

        JsHealthState state = _messageStore.getHealthState();
        if (!state.isOK())
            throw new Error("Message store failed to start " + state.isGlobalError()
                            + ":" + state.isLocalError());

        return _messageStore;
    }

    /**
     * @see com.ibm.ws.sib.admin.JsAdminComponent#initialize()
     */
    @Override
    public synchronized void initialize(JsMessagingEngine engine, boolean clean,
                                        boolean reintTRM) throws Exception {
        createMessageStoreOnly(clean);

        // Instantiate the MessageProcessor as an engine component
        _messageProcessor = loadClass(JsConstants.SIB_CLASS_MP);

        if (_messageProcessor != null
            && _messageProcessor instanceof JsEngineComponentWithEventListener) {
            RuntimeEventListener rel = ((SIMPJsMBeanFactoryImpl) regService)
                            .createListener();
            ((JsEngineComponentWithEventListener) _messageProcessor)
                            .setRuntimeEventListener(rel);
        }

        // _trm = loadClass(JsConstants.SIB_CLASS_TO_ENGINE, false);
        // cachedTRM.setTRMReference(_trm);
        // _trm = cachedTRM;
        jmeComponents.addElement(new ComponentList(
                        JsConstants.SIB_CLASS_TO_ENGINE, cachedTRM));

        // _trm.start();

        /*
         * Do not initialize MP (the single sub-component). We will do this manually
         * for unit tests.
         * 
         * 174199.2.18
         */
        // Initialize any engine components
        // Enumeration vEnum = jmeComponents.elements();
        // while (vEnum.hasMoreElements()) {
        // Object o = (Object) vEnum.nextElement();
        // Object c = ((ComponentList) o).getRef();
        // if (c instanceof JsEngineComponent)
        // ((JsEngineComponent) c).initialize(this);
        // }
        // 176658.3.8

    }

    /**
   */
    @Override
    public synchronized void start(int startMode) {
        /*
         * Do not start MP (the single sub-component). We will do this manually for
         * unit tests.
         * 
         * 174199.2.18
         */
        // Start the sub-components
        // Enumeration vEnum = jmeComponents.elements();
        // while (vEnum.hasMoreElements()) {
        // Object o = (Object) vEnum.nextElement();
        // Object c = ((ComponentList) o).getRef();
        // if (c instanceof JsEngineComponent)
        // ((JsEngineComponent) c).start();
        // }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#stop(int)
     */
    @Override
    public synchronized void stop(int mode) {
        // Stop the sub-components
        Enumeration<ComponentList> vEnum = jmeComponents.elements();
        while (vEnum.hasMoreElements()) {
            Object o = vEnum.nextElement();
            Object c = ((ComponentList) o).getRef();
            if (c instanceof JsEngineComponent) {
                ((JsEngineComponent) c).serverStopping();
                ((JsEngineComponent) c).stop(JsConstants.ME_STOP_QUIESCE);
            }
        }
    }

    /**
   */
    @Override
    public synchronized void destroy() {
        // Destroy the sub-components
        Enumeration<ComponentList> vEnum = jmeComponents.elements();
        while (vEnum.hasMoreElements()) {
            Object o = vEnum.nextElement();
            Object c = ((ComponentList) o).getRef();
            if (c instanceof JsEngineComponent)
                ((JsEngineComponent) c).destroy();
        }
    }

    @Override
    public synchronized void setCustomProperty(String name, String value) {
        // No implementation yet!
    }

    @Override
    public synchronized void setConfig(JsEObject config) {
        // Pass the attributes to all engine components
        Enumeration<ComponentList> vEnum = jmeComponents.elements();
        while (vEnum.hasMoreElements()) {
            Object o = vEnum.nextElement();
            Object c = ((ComponentList) o).getRef();
            if (c instanceof JsEngineComponent) {
                // ((JsEngineComponent) c).setConfig(config);
                // use reflection until setConfig() in interface
                try {
                    java.lang.reflect.Method setConfigMethod = c.getClass().getMethod(
                                                                                      "setConfig", new Class[] { JsEObject.class });
                    setConfigMethod.invoke(c, new Object[] { config });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized String getAttribute(String name) {
        return "";
    }

    /**
     * Method loadClass.
     * 
     * This method loads the named class and adds it to the list of process
     * components.
     * 
     * @param className
     */
    private JsEngineComponent loadClass(String className) {

        JsEngineComponent retValue = loadClass(className, true);

        return retValue;
    }

    private JsEngineComponent loadClass(String className, boolean addToList) {
        Class myClass = null;
        JsEngineComponent retValue = null;

        try {
            myClass = Class.forName(className);
            retValue = (JsEngineComponent) myClass.newInstance();
            if (addToList)
                jmeComponents.addElement(new ComponentList(className, retValue));
        } catch (InstantiationException e) {
            // No FFDC code needed
            // ...this class runs in a non-WAS environment
            //e.printStackTrace();
        } catch (Throwable e) {
            // No FFDC code needed
            // ...this class runs in a non-WAS environment
            //e.printStackTrace();
        }

        return retValue;
    }

    /**
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#datastoreExists()
     *      <p>
     *      There is never a datastore object in our test scaffold.
     *      <p>
     *      Defect 177879
     */
    @Override
    public boolean datastoreExists() {
        return false;
    }

    /**
     * Add an alias definition to simulated admin which we will get back from {@link #getSIBDestination(String, String)}. This bypasses the WCCM file.
     * 
     * @param dd
     */
    public synchronized void addAliasDestination(DestinationAliasDefinition dd) {
        String busName = dd.getBus();
        if (busName == null) {
            busName = _busName;
        }
        aliasDestinationsByName.put(busName + dd.getName(), dd);
        aliasDestinationsByUuid.put(dd.getUUID().toString(), dd);
    }

    /**
     * Add a local definition to simulated admin which we will get back from {@link #getSIBDestination(String, String)}. This bypasses the WCCM file.
     * 
     * @param dd
     */
    public synchronized void addLocalDestination(DestinationDefinition dd) {
        JsAdminFactory adminFactory = null;
        try {
            adminFactory = JsAdminFactory.getInstance();
        } catch (Exception e) {
            // No FFDC code needed
            e.printStackTrace();
        }

        Set<String> localizingSet = new HashSet<String>();
        localizingSet.add(_uuid.toString());
        LocalizationDefinition loc = adminFactory.createLocalizationDefinition(dd
                        .getName());
        DestinationInformation newDInfo = new DestinationInformation(dd,
                        localizingSet, loc);

        localDestinationsByName.put(dd.getName(), newDInfo);
        localDestinationsByUuid.put(dd.getUUID().toString(), newDInfo);
    }

    /**
     * Delete an alias definition to simulated admin which we will get back from {@link #getSIBDestination(String, String)}. This bypasses the WCCM file.
     * 
     * @param dd
     */
    public synchronized void deleteAliasDestination(String busName, String destName) {
        if (busName == null) {
            busName = _busName;
        }
        BaseDestinationDefinition dd = aliasDestinationsByName.remove(busName + destName);
        if (dd != null) {
            aliasDestinationsByUuid.remove(dd.getUUID().toString());
        }
    }

    /**
     * Add a foreign definition to simulated admin which we will get back from {@link #getSIBDestination(String, String)}. This bypasses the WCCM file.
     * 
     * @param dfd
     */
    public synchronized void addForeignDestination(DestinationForeignDefinition dfd) {
        String busName = dfd.getBus();
        if (busName == null) {
            busName = _busName;
        }
        foreignDestinationsByName.put(busName + dfd.getName(), dfd);
        foreignDestinationsByUuid.put(dfd.getUUID().toString(), dfd);
    }

    /**
     * Reset the alias destinations admin list. Reset the foreign destinations
     * admin list.
     */
    public synchronized void resetConfig() {
        aliasDestinationsByName = new HashMap<String, BaseDestinationDefinition>();
        aliasDestinationsByUuid = new HashMap<String, BaseDestinationDefinition>();
        foreignDestinationsByName = new HashMap<String, BaseDestinationDefinition>();
        foreignDestinationsByUuid = new HashMap<String, BaseDestinationDefinition>();
    }

    /**
     * Reset the registration service.
     */
    public synchronized void reset() {
        regService = new SIMPJsMBeanFactoryImpl();
    }

    /**
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getDatastore()
     *      <p>
     *      There is never a datastore object in our test scaffold.
     *      <p>
     *      Defect 177879
     */
    public JsEObject getDatastore() {
        return null;
    }

    /**
     * Method getBusName.
     * 
     * @return String
     */
    @Override
    public synchronized String getBusName() {
        return _busName;
    }

    /**
   */
    @Override
    public String getName() {
        return _name;
    }

    @Override
    public synchronized String getUuid() {
        return _uuid.toString();
    }

    @Override
    public synchronized ControllableRegistrationService getMBeanFactory() {
        return regService;
    }

    /**
     * Method getEngineComponent.
     * 
     * @param className
     * @return JsEngineComponent
     */
    @Override
    public synchronized JsEngineComponent getEngineComponent(String className) {

        Enumeration<ComponentList> vEnum = jmeComponents.elements();
        while (vEnum.hasMoreElements()) {
            Object c = vEnum.nextElement();
            if (((ComponentList) c).getClassName() == className) {
                return ((ComponentList) c).getRef();
            }
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <EngineComponent> EngineComponent getEngineComponent(Class<EngineComponent> clazz) {
        Enumeration<ComponentList> vEnum = jmeComponents.elements();
        JsEngineComponent foundEngineComponent = null;

        while (vEnum.hasMoreElements() && foundEngineComponent == null) {
            Object c = vEnum.nextElement();
            if (clazz.isInstance(((ComponentList) c).getRef())) {
                foundEngineComponent = ((ComponentList) c).getRef();
            }
        }

        return (EngineComponent) foundEngineComponent;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <EngineComponent> EngineComponent[] getEngineComponents(
                                                                                Class<EngineComponent> clazz) {
        List<EngineComponent> foundComponents = new ArrayList<EngineComponent>();

        Enumeration<ComponentList> vEnum = jmeComponents.elements();
        JsEngineComponent foundEngineComponent = null;

        while (vEnum.hasMoreElements() && foundEngineComponent == null) {
            Object c = vEnum.nextElement();
            JsEngineComponent ec = ((ComponentList) c).getRef();
            if (clazz.isInstance(ec)) {
                foundComponents.add((EngineComponent) ec);
            }
        }

        return foundComponents.toArray((EngineComponent[]) Array.newInstance(clazz,
                                                                             0));
    }

    /**
     * Method getMessageProcessor.
     * 
     * @return JsEngineComponent
     */
    @Override
    public synchronized JsEngineComponent getMessageProcessor() {
        return _messageProcessor;
    }

    public JsEngineComponent getWSRMEngineComponent() {
        return _wsrmEngineComponent;
    }

    /**
     * Method getMessageStore.
     * 
     * @return MessageStore
     */
    @Override
    public synchronized Object getMessageStore() {
        return _messageStore;
    }

    /**
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#loadLocalizedDestinations()
     */
    public synchronized void loadLocalizedDestinations() {
        loadLocalizations();
        return;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#loadLocalizations()
     */
    @Override
    public synchronized void loadLocalizations() {

        // Clear up dests
        remoteDestinationsByName.clear();
        remoteDestinationsByUuid.clear();
        localDestinationsByUuid.clear();
        localDestinationsByName.clear();
        linksByName.clear();
        localLinksByUuid.clear();
        // Read the Propertis file
        readPropertiesFile();

        try {

            Administrator theAdmin = ((SIMPAdmin) _messageProcessor)
                            .getAdministrator();
            JsAdminFactory adminFactory = null;
            try {
                adminFactory = JsAdminFactory.getInstance();
            } catch (Exception e) {
                // No FFDC code needed
                e.printStackTrace();
            }

            Iterator<DestinationInformation> iter = localDestinationsByUuid.values()
                            .iterator();
            while (iter.hasNext()) {
                DestinationInformation dInfo = iter.next();
                DestinationDefinition destDef = dInfo.getDDefinition();

                try {
                    theAdmin.createDestinationLocalization(destDef, dInfo
                                    .getQueuePointDefinition()
                                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Create any local links (not MQ ones)
            Iterator<LinkInformation> iter2 = localLinksByUuid.values().iterator();
            while (iter2.hasNext()) {
                LinkInformation lInfo = iter2.next();
                VirtualLinkDefinition linkDef = lInfo.getLDefinition();

                // Add a non-mediated destination
                if (lInfo.isMQ()) {
                    if (!lInfo.isLocalisationDeleted()) {
                        theAdmin.createMQLink(linkDef, lInfo.getMQLinkDefinition(),
                                              adminFactory.createLocalizationDefinition(linkDef.getName()));
                    }
                } else {
                    theAdmin.createGatewayLink(linkDef, linkDef.getUuid().toString());
                }
            }

        } catch (SIRollbackException e) {
            // No FFDC code needed
            e.printStackTrace();
        } catch (SIException e) {
            // No FFDC code needed
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#readPropertiesFile()
     */
    public synchronized void readPropertiesFile() {
        boolean isQueuePointLocal = true;
        boolean isQueuePointMQ = false;
        boolean isMediationPointLocal = false;
        boolean isMediationPointMQ = false;
        boolean isLinksLocal = true;

        // set of MEs which localise a queue point
        HashSet<String> localisingQueuePointMESet = new HashSet<String>();

        // Set of MEs which localise a mediation point
        HashSet<String> localisingMediationPointMESet = new HashSet<String>();

        // Set of MEs which localise a link
        HashSet<String> localisingLinkMESet = new HashSet<String>();

        // Clear up dests
        remoteDestinationsByName.clear();
        remoteDestinationsByUuid.clear();
        localDestinationsByUuid.clear();
        localDestinationsByName.clear();
        linksByName.clear();
        localLinksByUuid.clear();
        aliasWCCMDestinationsByName.clear();
        aliasWCCMDestinationsByUuid.clear();

        // Create a local bus object.
        if (mpBus == null) {
            mpBus = new MPBus(this);
        } else {
            mpBus.clear(); // we might be in a situation when a bus.getForeignDest
            // is called as part of startup processing. This might be at a point
            // before
            // loadLocalisations is called, which means that the mpBus will load the
            // dest info itself prematurely.
            // This means we should load the info into an existing mpBus rather than a
            // new one so that
            // the mpBus requiring the loading is the same as that which gets the
            // destination info loaded
            // into it.
        }

        // Create an exception destination
        JsAdminFactory adminFactory = null;
        try {
            adminFactory = JsAdminFactory.getInstance();
        } catch (Exception e) {
            // No FFDC code needed
            e.printStackTrace();
        }
        defaultExceptionDestDef = adminFactory.createDestinationDefinition(
                                                                           DestinationType.QUEUE,
                                                                           SIMPConstants.SYSTEM_DEFAULT_EXCEPTION_DESTINATION + getName());

        // Set up a suitable qos
        defaultExceptionDestDef.setMaxReliability(Reliability.ASSURED_PERSISTENT);
        defaultExceptionDestDef
                        .setDefaultReliability(Reliability.ASSURED_PERSISTENT);
        defaultExceptionDestDef.setUUID(new SIBUuid12("123456789ABC"));
        Set<String> localizingSet = new HashSet<String>();
        localizingSet.add(_uuid.toString());
        LocalizationDefinition exceptionLoc = adminFactory
                        .createLocalizationDefinition(defaultExceptionDestDef.getName());
        exceptionLoc.setDestinationHighMsgs(exceptionDestHightLimit);
        exceptionLoc.setDestinationLowMsgs(exceptionDestLowLimit);
        DestinationInformation newDInfo = new DestinationInformation(
                        defaultExceptionDestDef, localizingSet,
                        exceptionLoc);
        localDestinationsByName.put(defaultExceptionDestDef.getName(), newDInfo);
        localDestinationsByUuid.put(defaultExceptionDestDef.getUUID().toString(),
                                    newDInfo);
        localizingSet = null;

        try {
            String fn = RECON_DEST_FILENAME;

            FileInputStream in = new FileInputStream(fn);
            Properties props = new Properties();

            props.load(in);

            if (props.containsKey("DESTINATIONS")) {
                StringTokenizer st = new StringTokenizer(props
                                .getProperty("DESTINATIONS"), ",");

                while (st.hasMoreElements()) {
                    localisingQueuePointMESet.clear();
                    localisingMediationPointMESet.clear();
                    mqServerSet.clear();

                    String dest = st.nextToken();

                    DestinationType destinationType = DestinationType.QUEUE;
                    String value = null;

                    // parse topicspace flag
                    value = props.getProperty("DESTINATIONS" + "." + dest + "." + "type");
                    if (value != null) {
                        if ("topicspace".equals(value)) {
                            destinationType = DestinationType.TOPICSPACE;
                        } else if ("queue".equals(value)) {
                            destinationType = DestinationType.QUEUE;
                        } else if ("port".equals(value)) {
                            destinationType = DestinationType.PORT;
                        } else if ("service".equals(value)) {
                            destinationType = DestinationType.SERVICE;
                        }
                    }

                    DestinationDefinition destDef = adminFactory
                                    .createDestinationDefinition(destinationType, dest);

                    // parse the uuid
                    SIBUuid12 uuid = null;
                    value = props.getProperty("DESTINATIONS" + "." + dest + "." + "uuid");
                    if (value != null && value.length() == 24) // was 36
                    {
                        // behaviour is undefined if the uuid is not in the correct format
                        uuid = new SIBUuid12(value);
                    } else {
                        uuid = UT_SIMPUtils.createSIBUuid12(dest);
                    }
                    destDef.setUUID(uuid);

                    // parse qos
                    value = props.getProperty("DESTINATIONS" + "." + dest + "." + "qos");
                    if (value != null) {
                        if (value.equalsIgnoreCase("BestEffortNonPersistent")) {
                            destDef.setMaxReliability(Reliability.BEST_EFFORT_NONPERSISTENT);
                            destDef
                                            .setDefaultReliability(Reliability.BEST_EFFORT_NONPERSISTENT);
                        }
                        if (value.equalsIgnoreCase("ExpressNonPersistent")) {
                            destDef.setMaxReliability(Reliability.EXPRESS_NONPERSISTENT);
                            destDef.setDefaultReliability(Reliability.EXPRESS_NONPERSISTENT);
                        }
                        if (value.equalsIgnoreCase("ReliableNonPersistent")) {
                            destDef.setMaxReliability(Reliability.RELIABLE_NONPERSISTENT);
                            destDef.setDefaultReliability(Reliability.RELIABLE_NONPERSISTENT);
                        }
                        if (value.equalsIgnoreCase("ReliablePersistent")) {
                            destDef.setMaxReliability(Reliability.RELIABLE_PERSISTENT);
                            destDef.setDefaultReliability(Reliability.RELIABLE_PERSISTENT);
                        }
                        if (value.equalsIgnoreCase("AssuredPersistent")) {
                            destDef.setMaxReliability(Reliability.ASSURED_PERSISTENT);
                            destDef.setDefaultReliability(Reliability.ASSURED_PERSISTENT);
                        }
                    }

                    LocalizationDefinition queuePointDef = null;

                    // parse isQueuePointLocal flag
                    value = props.getProperty("DESTINATIONS" + "." + dest
                                              + ".isQueuePointLocal");
                    if (value != null) {

                        isQueuePointLocal = new Boolean(value).booleanValue();
                        if (isQueuePointLocal) {
                            queuePointDef = adminFactory.createLocalizationDefinition(destDef
                                            .getName());

                            String basePropertyName = "DESTINATIONS." + dest + ".queuepoint";
                            value = props.getProperty(basePropertyName + ".isSendAllowed");
                            if (value != null) {
                                queuePointDef.setSendAllowed(new Boolean(value).booleanValue());
                            }
                            value = props.getProperty(basePropertyName
                                                      + ".destinationHighMsgs");
                            if (value != null) {
                                queuePointDef.setDestinationHighMsgs(Long.parseLong(value));
                            }
                            value = props.getProperty(basePropertyName
                                                      + ".destinationLowMsgs");
                            if (value != null) {
                                queuePointDef.setDestinationLowMsgs(Long.parseLong(value));
                            }
                        }
                    }

                    MQLocalizationDefinition mqDef = null;

                    // parse isQueuePointMQ flag
                    value = props.getProperty("DESTINATIONS" + "." + dest
                                              + ".isQueuePointMQ");
                    if (value != null) {
                        isQueuePointMQ = new Boolean(value).booleanValue();
                        if (isQueuePointMQ) {
                            mqDef = adminFactory.createMQLocalizationDefinition(destDef
                                            .getName());

                            String basePropertyName = "DESTINATIONS." + dest + ".queuepoint";
                            value = props.getProperty(basePropertyName + ".channelName");
                            if (value != null) {
                                mqDef.setMQChannelName(value);
                            }
                            value = props.getProperty(basePropertyName + ".queueManagerName");
                            if (value != null) {
                                mqDef.setMQQueueManagerName(value);
                            }
                            value = props.getProperty(basePropertyName + ".queueName");
                            if (value != null) {
                                mqDef.setMQQueueName(value);
                            }
                            value = props.getProperty(basePropertyName + ".hostName");
                            if (value != null) {
                                mqDef.setMQHostName(value);
                            }
                            value = props.getProperty(basePropertyName + ".serverPort");
                            if (value != null) {
                                mqDef.setMQServerPort(new Integer(value).intValue());
                            }
                            value = props.getProperty(basePropertyName + ".serverUuid");
                            if (value != null) {
                                mqDef.setMQServerBusMemberUuid(new SIBUuid8(value));
                                mqServerSet = addKeyToSet(mqServerSet, value);
                            }
                            value = props.getProperty(basePropertyName
                                                      + ".nonPersistentReliability");
                            if (value != null) {
                                mqDef.setInboundNonPersistentReliability(Reliability
                                                .getReliabilityByName(value));
                            }
                            value = props.getProperty(basePropertyName
                                                      + ".persistentReliability");
                            if (value != null) {
                                mqDef.setInboundPersistentReliability(Reliability
                                                .getReliabilityByName(value));
                            }
                        }
                    }

                    // Now retrieve the locality set
                    // parse isLocal flag
                    value = props.getProperty("DESTINATIONS" + "." + dest + "."
                                              + "MEuuid");
                    if (value != null) {
                        StringTokenizer stuuid = new StringTokenizer(props
                                        .getProperty("DESTINATIONS" + "." + dest + "." + "MEuuid"), ",");
                        while (stuuid.hasMoreElements()) {
                            String stringMEuuid = stuuid.nextToken();
                            // Add remote me uuids to the localising set
                            if (stringMEuuid != null) {
                                // remoteMEuuid = new SIBUuid8(stringMEuuid);
                                localisingQueuePointMESet = addKeyToSet(
                                                                        localisingQueuePointMESet, stringMEuuid);
                            }
                        }
                    }

                    // Send Allowed set of attributes
                    value = props.getProperty("DESTINATIONS" + "." + dest + "."
                                              + "sendAllowed");
                    if (value != null) {
                        destDef.setSendAllowed(new Boolean(value).booleanValue());
                    }

                    value = props.getProperty("DESTINATIONS" + "." + dest + "."
                                              + "receiveAllowed");
                    if (value != null) {
                        destDef.setReceiveAllowed(new Boolean(value).booleanValue());
                    }

                    value = props.getProperty("DESTINATIONS." + dest + "." + "isOrdered");
                    if (value != null) {
                        destDef.maintainMsgOrder(new Boolean(value).booleanValue());
                    }

                    value = props.getProperty("DESTINATIONS." + dest + "."
                                              + "receiveExclusive");
                    if (value != null) {
                        destDef.setReceiveExclusive(new Boolean(value).booleanValue());
                    }

                    value = props.getProperty("DESTINATIONS." + dest
                                              + ".maxFailedDeliveries");
                    if (value != null) {
                        destDef.setMaxFailedDeliveries(new Integer(value).intValue());
                    }

                    // Add the exception destination
                    value = props.getProperty("DESTINATIONS." + dest + ".exceptionDest");
                    destDef.setExceptionDestination(value);

                    /*
                     * Debug if(topicspace) { System.out.println("Admin Found: Topicspace
                     * destination: " + dest + ", uuid: " + uuid); } else {
                     * System.out.println("Admin Found: Queue destination: " + dest + ",
                     * uuid: " + uuid); }
                     */

                    if (isQueuePointLocal) {
                        // If the queue point is local, add this ME to the list of MEs which
                        // localise the queue point for this destination.
                        if (mqDef != null) {
                            localisingQueuePointMESet = addKeyToSet(
                                                                    localisingQueuePointMESet, mqDef.getMQServerBusMemberUuid()
                                                                                    .toString());

                            mqServerSet = addKeyToSet(mqServerSet, mqDef
                                            .getMQServerBusMemberUuid().toString());
                        } else if (isQueuePointLocal
                                   && destinationType != DestinationType.SERVICE) {
                            localisingQueuePointMESet = addKeyToSet(
                                                                    localisingQueuePointMESet, _uuid.toString());
                        }

                        if (destinationType == DestinationType.SERVICE)
                            localisingQueuePointMESet.clear();

                        localDestinationsByUuid.put(destDef.getUUID().toString(),
                                                    new DestinationInformation(destDef, localisingQueuePointMESet,
                                                                    queuePointDef));
                        localDestinationsByName.put(destDef.getName(),
                                                    new DestinationInformation(destDef, localisingQueuePointMESet,
                                                                    queuePointDef));

                        // Need to clear the mediation point set as it isn't done elsewhere.
                        localisingMediationPointMESet.clear();
                    } else {
                        // Build up a set of remote destination definitions
                        remoteDestinationsByName
                                        .put(_busName + dest, new DestinationInformation(destDef,
                                                        localisingQueuePointMESet,
                                                        null));
                        remoteDestinationsByUuid
                                        .put(destDef.getUUID().toString(),
                                             new DestinationInformation(destDef,
                                                             localisingQueuePointMESet,
                                                             null));
                    }
                }

                // Next look for any links
                st = new StringTokenizer(props.getProperty("LINKS"), ",");

                while (st.hasMoreElements()) {
                    String link = st.nextToken();

                    String value = null;

                    // parse the uuid
                    SIBUuid12 uuid = null;
                    value = props.getProperty("LINKS" + "." + link + "." + "uuid");
                    if (value != null && value.length() == 24) // was 36
                    {
                        // behaviour is undefined if the uuid is not in the correct format
                        uuid = new SIBUuid12(value);
                    } else {
                        uuid = UT_SIMPUtils.createSIBUuid12(link);
                    }

                    // parse isLocal flag
                    value = props.getProperty("LINKS" + "." + link + "." + "islocal");
                    if (value != null) {
                        isLinksLocal = new Boolean(value).booleanValue();
                    }

                    // parse isLocal flag
                    boolean isMQ = false;
                    value = props.getProperty("LINKS" + "." + link + "." + "isMQ");
                    if (value != null) {
                        isMQ = new Boolean(value).booleanValue();
                    }

                    // Parse the mqlinkuuid
                    SIBUuid8 mqLinkuuid = null;
                    if (isMQ) {
                        value = props
                                        .getProperty("LINKS" + "." + link + "." + "mqlinkuuid");
                        if (value != null && value.length() == 16) // was 24
                        {
                            // behaviour is undefined if the uuid is not in the correct format
                            mqLinkuuid = new SIBUuid8(value);
                        } else {
                            mqLinkuuid = UT_SIMPUtils.createSIBUuid8(link);
                        }
                    }

                    // get the exception destination
                    String exceptionDestination = props.getProperty("LINKS" + "." + link + "." + "exceptionDestination");
                    if (exceptionDestination.equals("NULL"))
                        exceptionDestination = null;

                    // Now retrieve the locality set
                    // parse isLocal flag
                    value = props.getProperty("LINKS" + "." + link + "." + "MEuuid");
                    if (value != null) {
                        StringTokenizer stuuid = new StringTokenizer(props
                                        .getProperty("LINKS" + "." + link + "." + "MEuuid"), ",");
                        while (stuuid.hasMoreElements()) {
                            String stringMEuuid = stuuid.nextToken();
                            // Add me uuids to the localising set
                            if (stringMEuuid != null) {
                                localisingLinkMESet.add(stringMEuuid);
                            }
                        }
                    }

                    // Now retrieve the topicspaceNames
                    Map<String, String> topicSpaceMap = new HashMap<String, String>();
                    value = props.getProperty("LINKS" + "." + link + "."
                                              + "TopicSpaceNames");
                    if (value != null) {
                        StringTokenizer stts = new StringTokenizer(props
                                        .getProperty("LINKS" + "." + link + "." + "TopicSpaceNames"),
                                        ",");

                        while (stts.hasMoreElements()) {
                            String key = stts.nextToken();
                            String val = stts.nextToken();
                            // Add next topicSpaceName to Map
                            if (val != null) {
                                topicSpaceMap.put(key, val);
                            }
                        }
                    }

                    VirtualLinkDefinition virtualLinkDefinition = new VirtualLinkDefinitionImpl(
                                    link, uuid, topicSpaceMap);

                    // SIB0105b
                    ((VirtualLinkDefinitionImpl) virtualLinkDefinition).setExceptionDestination(exceptionDestination);

                    // If we're working with an MQLink, then set the appropriate type into the vld
                    if (isMQ)
                        ((VirtualLinkDefinitionImpl) virtualLinkDefinition).setType("SIBVirtualMQLink");

                    // Remember the links by name, till the bus definitions are created
                    // that use them.
                    linksByName.put(virtualLinkDefinition.getName(),
                                    virtualLinkDefinition);

                    // Remember the links in the local bus
                    mpBus.setLink(virtualLinkDefinition);

                    /*
                     * Debug if(topicspace) { System.out.println("Admin Found: Link: " +
                     * dest + ", uuid: " + uuid); } else { System.out.println("Admin
                     * Found: Link: " + dest + ", uuid: " + uuid); }
                     */

                    // Build up a set of localisations for the link
                    if (localisingLinkMESet.contains(_uuid.toString())) {
                        localisingLinkMESet.add(_uuid.toString());
                    }

                    // Set the locality set into the vld
                    ((VirtualLinkDefinitionImpl) virtualLinkDefinition)
                                    .setLinkLocalitySet(localisingLinkMESet);

                    if (isLinksLocal) {
                        if (isMQ) {
                            localLinksByUuid.put(virtualLinkDefinition.getUuid().toString(),
                                                 new LinkInformation(virtualLinkDefinition, mqLinkuuid));
                        } else {
                            localLinksByUuid.put(virtualLinkDefinition.getUuid().toString(),
                                                 new LinkInformation(virtualLinkDefinition));
                        }
                    }
                }

                // Now look for any foreign buses
                st = new StringTokenizer(props.getProperty("BUSES"), ",");

                while (st.hasMoreElements()) {
                    String bus = st.nextToken();

                    String value = null;

                    // parse the uuid
                    SIBUuid12 uuid = null;
                    value = props.getProperty("BUSES" + "." + bus + "." + "uuid");
                    if (value != null && value.length() == 24) // was 36
                    {
                        // behaviour is undefined if the uuid is not in the correct format
                        uuid = new SIBUuid12(value);
                    } else {
                        uuid = UT_SIMPUtils.createSIBUuid12(bus);
                    }

                    // parse qos
                    value = props.getProperty("BUSES" + "." + bus + "." + "qos");
                    Reliability reliability = null;

                    if (value != null) {
                        if (value.equalsIgnoreCase("BestEffortNonPersistent")) {
                            reliability = Reliability.BEST_EFFORT_NONPERSISTENT;
                        }
                        if (value.equalsIgnoreCase("Express")) {
                            reliability = Reliability.EXPRESS_NONPERSISTENT;
                        }
                        if (value.equalsIgnoreCase("ReliableNonPersistent")) {
                            reliability = Reliability.RELIABLE_NONPERSISTENT;
                        }
                        if (value.equalsIgnoreCase("ReliablePersistent")) {
                            reliability = Reliability.RELIABLE_PERSISTENT;
                        }
                        if (value.equalsIgnoreCase("AssuredPersistent")) {
                            reliability = Reliability.ASSURED_PERSISTENT;
                        }
                    }

                    value = props.getProperty("BUSES" + "." + bus + "." + "sendAllowed");
                    Boolean sendAllowed = new Boolean(value);

                    value = props.getProperty("BUSES" + "." + bus + "." + "linkname");
                    String linkName = value;

                    /*
                     * Debug System.out.println("Admin Found: Link: " + bus + ", uuid: " +
                     * uuid);
                     */

                }

                // Next look for any aliases
                // Ensure there are aliases in the properties file before creating.
                if (props.containsKey("ALIASES")) {
                    st = new StringTokenizer(props.getProperty("ALIASES"), ",");

                    while (st.hasMoreElements()) {
                        String alias = st.nextToken();

                        String value = null;

                        // parse the uuid
                        SIBUuid12 uuid = null;
                        value = props.getProperty("ALIASES" + "." + alias + "." + "uuid");
                        if (value != null && value.length() == 24) // was 36
                        {
                            // behaviour is undefined if the uuid is not in the correct format
                            uuid = new SIBUuid12(value);
                        } else {
                            uuid = UT_SIMPUtils.createSIBUuid12(alias);
                        }

                        DestinationType type = null;
                        value = props.getProperty("ALIASES" + "." + alias + "." + "type");
                        if (value != null && value.equals("TOPICSPACE")) {
                            type = DestinationType.TOPICSPACE;
                        } else if (value != null && value.equals("QUEUE")) {
                            type = DestinationType.QUEUE;
                        } else
                            type = null;

                        value = props
                                        .getProperty("ALIASES" + "." + alias + "." + "busName");
                        String busName = value;

                        value = props.getProperty("ALIASES" + "." + alias + "."
                                                  + "targetName");
                        String targetName = value;

                        value = props.getProperty("ALIASES" + "." + alias + "."
                                                  + "targetBus");
                        String targetBus = value;

                        try {
                            DestinationAliasDefinition add = JsAdminFactory.getInstance()
                                            .createDestinationAliasDefinition(type, alias);

                            add.setBus(busName);
                            add.setTargetName(targetName);
                            add.setTargetBus(targetBus);
                            add.setUUID(uuid);

                            add
                                            .setDefaultPriority(DestinationAliasDefinition.DEFAULT_DEFAULTPRIORITY);
                            add.setDefaultReliability(Reliability.NONE);
                            add.setMaxReliability(Reliability.NONE);
                            add.setSendAllowed(ExtendedBoolean.NONE);
                            add.setReceiveAllowed(ExtendedBoolean.NONE);
                            add.setOverrideOfQOSByProducerAllowed(ExtendedBoolean.NONE);

                            add.setDestinationContext(new HashMap());

                            aliasWCCMDestinationsByName.put(busName + alias, add);
                            aliasWCCMDestinationsByUuid.put(add.getUUID().toString(), add);
                        } catch (Exception e) {
                            // No FFDC code needed
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            // No FFDC code needed
        } catch (IOException e) {
            // No FFDC code needed
        }

        // set flag that we've read the properties
        readProperties = true;
    }

    /**
     * Adds the key to a set, creating the set if it doesn't already exist.
     * 
     * @param setOfMEsSoFar
     *            An existing set to which the key should be added, or null if no
     *            set exists yet, and on needs to be created.
     * @param key
     *            The key item to add to the set.
     * @return The new set containing the specified key.
     */
    private HashSet<String> addKeyToSet(HashSet<String> existingSet, String key) {
        HashSet<String> resultingSet = existingSet;

        // Build up a set of local destination definitions
        // and ensure that the locality set contains at least the home
        // ME.
        if (resultingSet == null) {
            resultingSet = new HashSet<String>();
            resultingSet.add(key);
        } else {
            if (!resultingSet.contains(key)) {
                resultingSet.add(key);
            }
        }
        return resultingSet;
    }

    /**
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getSIBDestination(String, String)
     */
    @Override
    public synchronized BaseDestinationDefinition getSIBDestination(String bus, String in_name)
                    throws SIBExceptionBase, SIBExceptionDestinationNotFound {

        if (!readProperties) {
            // read properties file
            readPropertiesFile();
            readProperties = true;
        }

        String busName = bus;
        if (bus == null) {
            busName = _busName;
        }

        String name = busName + in_name;
        if (localDestinationsByName.containsKey(in_name)) {
            DestinationInformation dInfo = localDestinationsByName.get(in_name);
            return dInfo.getDDefinition();
        } else if (remoteDestinationsByName.containsKey(name)) {
            DestinationInformation dInfo = remoteDestinationsByName.get(name);

            return dInfo.getDDefinition();
        } else if (aliasDestinationsByName.containsKey(name)) {
            return aliasDestinationsByName.get(name);
        } else if (aliasWCCMDestinationsByName.containsKey(name)) {
            return aliasWCCMDestinationsByName.get(name);
        } else if (foreignDestinationsByName.containsKey(name)) {
            return foreignDestinationsByName.get(name);
        } else
            throw new SIBExceptionDestinationNotFound("Destination not found: "
                                                      + name);
    }

    /**
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getSIBDestinationByUuid(String, String)
     */
    @Override
    public synchronized BaseDestinationDefinition getSIBDestinationByUuid(String bus, String in_uuid)
                    throws SIBExceptionBase, SIBExceptionDestinationNotFound {

        if (!readProperties) {
            // read properties file
            readPropertiesFile();
            readProperties = true;
        }

        String busName = bus;
        if (bus == null) {
            busName = _busName;
        }

        if (localDestinationsByUuid.containsKey(in_uuid)) {
            DestinationInformation dInfo = localDestinationsByUuid.get(in_uuid);
            return dInfo.getDDefinition();
        } else if (remoteDestinationsByUuid.containsKey(in_uuid)) {
            DestinationInformation dInfo = remoteDestinationsByUuid.get(in_uuid);

            return dInfo.getDDefinition();
        } else if (aliasDestinationsByUuid.containsKey(in_uuid)) {
            return aliasDestinationsByUuid.get(in_uuid);
        } else if (aliasWCCMDestinationsByUuid.containsKey(in_uuid)) {
            return aliasWCCMDestinationsByUuid.get(in_uuid);
        } else if (foreignDestinationsByUuid.containsKey(in_uuid)) {
            return foreignDestinationsByUuid.get(in_uuid);
        } else
            throw new SIBExceptionDestinationNotFound("Destination not found: "
                                                      + in_uuid);
    }

    @Override
    public synchronized JsEngineComponent getMQLinkEngineComponent(String name) {
        return null;
    }

    public JsEObject getGatewayLink(String name) {
        return null;
    }

    public JsEngineComponent getGatewayLinkEngineComponent(String name) {
        return null;
    }

    public JsEngineComponent getTRMComponent() {
        return getTRM();
    }

    /**
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getSIBDestination(String, String, DestinationDefinition)
     */
    @Override
    public void getSIBDestination(String bus, String name,
                                  DestinationDefinition dd) {}

    /**
     * Returns the runtime configuration object proxy for a named SIBMQClientLink
     * WCCM object, if one exists. If an object with the specified name does not
     * exist, then null is returned.
     * 
     * @param name
     * @return
     */
    public JsEObject getMQClientLink(String name) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.test.SIMPJsStandaloneEngine#initializeMessageProcessor()
     */
    @Override
    public synchronized void initializeMessageProcessor() {
        JsEngineComponent mp = getMessageProcessor();
        try {
            mp.initialize(this);
            mp.start(JsConstants.ME_START_DEFAULT);
        } catch (Exception e) {

        }
        mp.serverStarted();
    }

    @Override
    public Set<String> getSIBDestinationLocalitySet(String busName, String uuid,
                                                    boolean newCache) {
        return getSIBDestinationLocalitySet(busName, uuid);
    }

    /**
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getSIBDestinationLocalitySet(String, String)
     */
    @Override
    public synchronized Set<String> getSIBDestinationLocalitySet(String busName, String uuid) {
        Set<String> result;

        DestinationInformation dInfo = null;

        if (remoteDestinationsByUuid.containsKey(uuid)) {
            dInfo = remoteDestinationsByUuid.get(uuid);
            result = dInfo.getLocalisingQueuePointMEuuidSet();
        } else {
            if (localDestinationsByUuid.containsKey(uuid)) // local dest
            {
                dInfo = localDestinationsByUuid.get(uuid);
                result = dInfo.getLocalisingQueuePointMEuuidSet();
            } else {
                // Defect 190172, if we get here, then we'll return a locality set
                // that comprises the home ME only.

                Set<String> destinationLocalizingSet = new HashSet<String>();
                destinationLocalizingSet.add(getUuid().toString());
                result = destinationLocalizingSet;
            }
        }
        return result;
    }

    /**
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getBus()
     */
    @Override
    public synchronized LWMConfig getBus() {
        return mpBus;
    }

    @Override
    public synchronized ThreadPool getMediationThreadPool() {
        return _mediationThreadPool;
    }

    @Override
    public synchronized void setMediationThreadPool(ThreadPool pool) {
        _mediationThreadPool = pool;
    }

    @Override
    public void reportLocalError() {
        // No Implementation
    }

    @Override
    public void reportGlobalError() {
        // No implementation
    }

    /**
     * Returns a connection factory for use primarily for the mediations simualted
     * framework.
     * 
     * @return A connection factory, or null if it's not possible right now.
     */
    public synchronized static SICoreConnectionFactory getConnectionFactory() {
        SICoreConnectionFactory factory = null;

        // Get the currently active instance of this class.
        SIMPJsStandaloneEngineImpl activeEngine = _currentActiveStandaloneEngineImpl;
        if (activeEngine != null) {
            factory = (SICoreConnectionFactory) activeEngine.getMessageProcessor();
        }
        return factory;
    }

    /**
     * Indicate that the WAS server in which the object is contained has started
     */
    public void serverStarted() {}

    /**
     * Indicate that the WAS server in which the object is contained is stopping
     */
    public void serverStopping() {}

    public synchronized void setExceptionDestLimits(int highLimit, int lowLimit) {
        exceptionDestHightLimit = highLimit;
        exceptionDestLowLimit = lowLimit;
    }

    public synchronized void resetExceptionDestLimits() {
        exceptionDestHightLimit = exceptionDestDefaultQueueLimits;
        exceptionDestLowLimit = exceptionDestDefaultQueueLimits;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.RuntimeEventListener#isEventNotificationEnabled()
     */
    @Override
    public synchronized boolean isEventNotificationEnabled() {
        return _isEventNotificationEnabled;
    }

    public synchronized void setEventNotificationEnabled(boolean enabled) {
        _isEventNotificationEnabled = enabled;
    }

    @Override
    public int getMessageStoreType() {
        throw new UnsupportedOperationException();
    }

    public String expandVariables(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean filestoreExists() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LWMConfig getFilestore() {
        return null;
    }

    public synchronized Set<String> getMQServerBusMemberUuidSet() {
        return new HashSet<String>(mqServerSet);
    }

    @Override
    public LWMConfig getMQLink(String name) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public long getMEThreshold() {
        // TODO Auto-generated method stub
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void setMEUUID(SIBUuid8 uuid) {
        // TODO Auto-generated method stub

    }

}
