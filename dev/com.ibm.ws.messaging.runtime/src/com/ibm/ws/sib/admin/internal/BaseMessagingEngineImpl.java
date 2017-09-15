/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.admin.internal;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.BaseDestination;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.Controllable;
import com.ibm.ws.sib.admin.ControllableRegistrationService;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.JsBus;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsEngineComponent;
import com.ibm.ws.sib.admin.JsMEConfig;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.LWMConfig;
import com.ibm.ws.sib.admin.SIBExceptionBase;
import com.ibm.ws.sib.admin.SIBExceptionDestinationNotFound;
import com.ibm.ws.sib.admin.SIBFileStore;
import com.ibm.ws.sib.admin.SIBLocalizationPoint;
import com.ibm.ws.sib.admin.SIBPersistenceException;
import com.ibm.ws.sib.admin.SIBTransactionException;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.TransactionException;
import com.ibm.ws.sib.processor.Administrator;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;


public class BaseMessagingEngineImpl implements JsEngineComponent, LWMConfig, Controllable {

    private static final TraceComponent tc = SibTr.register(BaseMessagingEngineImpl.class, JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);

    private static final String CLASS_NAME = "com.ibm.ws.sib.admin.internal.BaseMessagingEngineImpl";

    // Flag: Is SIBMessagingEngine enabled?
//    protected boolean _enabled = false;

    // Flag: Have we sent server started notification?
    private boolean _sentServerStarted = false;

    // Flag: Have we sent server stopping notification?
    private volatile boolean _sentServerStopping = false;// changed modifier to volatile because method okToSendServerStopping() is not synchornized and so that latest copy of it is always available when serverStopping() notification is sent.

    // Used to record the destination & mediation caches of this ME's bus,
    // set when we are informed that the bus config documents have changed.
    private final JsDestinationCache oldDestCache = null;
    protected SIBFileStoreImpl _fs = null;

    // Refer to STD in JS HA Design
    protected static final int STATE_UNINITIALIZED = 0;

    protected static final int STATE_INITIALIZING = 1;

    protected static final int STATE_INITIALIZED = 2;

    protected static final int STATE_JOINING = 3;

    protected static final int STATE_JOINED = 4;

    protected static final int STATE_AUTOSTARTING = 5;

    protected static final int STATE_STARTING = 6;

    protected static final int STATE_STARTED = 7;

    protected static final int STATE_STOPPING = 8;

    protected static final int STATE_STOPPING_MEMBER = 9;

    protected static final int STATE_STOPPED = 10;

    protected static final int STATE_DESTROYING = 11;

    protected static final int STATE_DESTROYED = 12;

    protected static final int STATE_FAILED = 13;

    // Strings which represent the state of this ME, in a readable, but in a
    // format which is not NLS enabled, intended for use by MBeans which do not
    // support NLS.
    protected String[] states = { JsConstants.ME_STATE_UNINITIALIZED_STR, JsConstants.ME_STATE_INITIALIZING_STR, JsConstants.ME_STATE_INITIALIZED_STR,
                                 JsConstants.ME_STATE_JOINING_STR,
                                 JsConstants.ME_STATE_JOINED_STR, JsConstants.ME_STATE_AUTOSTARTING_STR, JsConstants.ME_STATE_STARTING_STR, JsConstants.ME_STATE_STARTED_STR,
                                 JsConstants.ME_STATE_STOPPING_STR,
                                 JsConstants.ME_STATE_STOPPINGMEMBER_STR, JsConstants.ME_STATE_STOPPED_STR, JsConstants.ME_STATE_DESTROYING_STR,
                                 JsConstants.ME_STATE_DESTROYED_STR, JsConstants.ME_STATE_FAILED_STR };

    // The initial state of an ME.
    // 181851 changed name of creation state to avoid ambiguity with hamanager.
    protected int _state = STATE_UNINITIALIZED;

    // Vector of the classes which are bootstrapped.
    // Needs to be synchronized to prevent concurrent modification from dynamic config and runtime callbacks
    //protected Vector jmeComponents = new Vector();
    protected CopyOnWriteArrayList<ComponentList> jmeComponents = new CopyOnWriteArrayList<ComponentList>();

    // Used to synchronize on stateChangeLock to manage the startup of the engine
    // with potential addition of new engine components via dynamic config.

    protected Object stateChangeLock = new Object();

    // 190516
    // Array of Vectors for stopping JsEngineComponents in custom order.
    // See comment in initialize(), where array is constructed.
    protected static final int NUM_STOP_PHASES = 5;

    protected static final int STOP_PHASE_0 = 0;

    protected static final int STOP_PHASE_1 = 1;

    protected static final int STOP_PHASE_2 = 2;

    protected static final int STOP_PHASE_3 = 3;

    protected static final int STOP_PHASE_4 = 4;

    protected Vector<ComponentList>[] stopSequence = new Vector[NUM_STOP_PHASES];

    // The service in which we are contained.
    protected JsMainImpl _mainImpl;

    // Our factory class for creating Admin objects.
    protected JsAdminFactory jsaf = null;

    // The GroupManager service
    // 181851
    // private GroupManager _groupManager;

    // The bus proxy object to which this ME belongs.
    protected JsBusImpl _bus;

    // The configuration object for this ME.
    protected JsMEConfig _me = null;

    // The optional filestore object for our ME
//    protected JsFilestoreImpl _fs = null;

    // The optional datastore object for our ME
//    protected JsDatastoreImpl _ds = null;

    // The name of our ME
    protected String _name;

    // Our UUID
    protected String _uuid = null;

    // MP Administration interface
    protected Administrator _mpAdmin = null;

    // The Message Processor object created by the instance of this class
    protected JsEngineComponent _messageProcessor;
    
   // The Message Store object created by the instance of this class
    protected MessageStore _messageStore;

    // The TRM object created by the instance of this class
    protected JsEngineComponent _trm;

    // Localization
    protected JsLocalizer _localizer = null;

    // Cache of localization point config objects
    private final ArrayList _lpConfig = new ArrayList();

    protected ControllableRegistrationService _mbeanFactory;

    // end 215830
    protected String dumpDir = null; // Directory path for dump files
//
//    // Custom Properties for the ME
//    private final Properties customProperties = new Properties();

    // Object to represent a component within this ME
    class ComponentList {

        private final String _className;

        private final JsEngineComponent _componentRef;

        ComponentList(String className, JsEngineComponent c) {
            _className = className;
            _componentRef = c;
        }

        // Get the name of the class
        String getClassName() {
            return _className;
        }

        // Get a reference to the instantiated class
        JsEngineComponent getRef() {
            return _componentRef;
        }
    }
    
    /*public BaseMessagingEngineImpl()
    {
        // super(JsMessagingEngineMBean.class,false);
    }*/
    
    public BaseMessagingEngineImpl(JsMainImpl mainImpl, JsBusImpl bus, JsMEConfig me) throws Exception {
        // this();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "BaseMessagingEngineImpl", new Object[] { mainImpl, bus,
                                                                     me });

        _mainImpl = mainImpl;
        _bus = bus;
        _me = me;
        _name = me.getMessagingEngine().getName();
        // GET SIBFileStore here below and create instance of JsFileStoreImpl  TBD
        SIBFileStore filestore = me.getSibFilestore();
        if (filestore == null) {
            _fs = new SIBFileStoreImpl();
        } else {
            _fs = new SIBFileStoreImpl();
            _fs.setMaxPermanentFileStoreSize(filestore.getMaxPermanentFileStoreSize());
            _fs.setMaxTemporaryFileStoreSize(filestore.getMaxTemporaryFileStoreSize());
            _fs.setMinPermanentFileStoreSize(filestore.getMinPermanentFileStoreSize());
            _fs.setMinTemporaryFileStoreSize(filestore.getMinTemporaryFileStoreSize());
            _fs.setUnlimitedPermanentStoreSize(filestore.isUnlimitedPermanentStoreSize());
            _fs.setUnlimitedTemporaryStoreSize(filestore.isUnlimitedTemporaryStoreSize());
            _fs.setPath(filestore.getPath());
            _fs.setLogFileSize(filestore.getLogFileSize());
        }
        // We can decide on what exactly we can set for Dump Directory later, for now by default the 
        // dumps will be created in the server_output_directory.
        // dumpDir = TrConfigurator.getLogLocation() + "/";
        _localizer = new JsLocalizer(this);
        HashMap<String, SIBLocalizationPoint> lpList = me.getMessagingEngine().getSibLocalizationPointList();
//        setLPConfigObjects((List<SIBLocalizationPoint>) lpList.values());
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "BaseMessagingEngineImpl", this);
    }
    public ControllableRegistrationService getMBeanFactory() {
         
        String thisMethodName = "getMBeanFactory";
     
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
          SibTr.entry(tc, thisMethodName);
          SibTr.exit(tc, thisMethodName, _mbeanFactory);
        }
     
        return _mbeanFactory;
      }
     

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#initialize(com.ibm.ws.sib.admin.JsMessagingEngine)
     */
    public void initialize(JsMessagingEngine engine) throws Exception {

        SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", CLASS_NAME + ": a subclass has not overriden initialize()");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#start(int)
     */
    public void start(int mode) throws Exception {
        
        SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", CLASS_NAME + ": a subclass has not overriden start()");
    }

    /**
     * This is an unconditional start() which defaults "mode" to "DEFAULT".
     */
    public void start() throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "start", this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Start ME, mode defaulting to ME_START_DEFAULT");
        start(JsConstants.ME_START_DEFAULT);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "start");
    }

    /**
     * 
     * @see com.ibm.ws.sib.admin.MessagingEngineMBean.JsMessagingEngineMBean#start(java.lang.String)
     */
    public void start(String mode) throws Exception {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "start", mode);
        // Now signal the start request
        start();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "start");
    }

    /**
     * Start the Messaging Engine if it is enabled
     * 
     * @throws Exception
     */
    public void startConditional() throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "startConditional", this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Activating MBean for ME " + getName());
        setState(STATE_STARTING);
        start(JsConstants.ME_START_DEFAULT);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "startConditional");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#serverStarted()
     */
    public final void serverStarted() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "serverStarted", this);

        if (okayToSendServerStarted()) {

            Iterator<ComponentList> vIter = jmeComponents.iterator();
            while (vIter.hasNext()) {
                vIter.next().getRef().serverStarted();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "serverStarted");
    }

    private final Object lockObject = new Object() {}; // Anonymous inner class used to get the class name in the lock for javacore deadlock analysis
    protected long meHighMessageThreshold;

    /**
     * Determine whether the conditions permitting a "server started" notification
     * to be sent are met.
     * 
     * @return boolean a value indicating whether the notification can be sent
     */
    private boolean okayToSendServerStarted() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "okayToSendServerStarted", this);

        synchronized (lockObject) {
            if (!_sentServerStarted) {
                if ((_state == STATE_STARTED) && _mainImpl.isServerStarted()) {
                    _sentServerStarted = true;
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "okayToSendServerStarted", new Boolean(_sentServerStarted));
        return _sentServerStarted;
    }

    /**
     * Determine whether the conditions permitting a "server stopping"
     * notification to be sent are met.
     * The ME state can be STARTED ,AUTOSTARTING or in STARTING state , to receive "server stopping" notification
     * 
     * @return boolean a value indicating whether the notification can be sent
     */
    private boolean okayToSendServerStopping() {
        //  Removed the synchronized modifier of this method, as its only setting one private variable _sentServerStopping,
        //  If its synchronized, then notification thread gets blocked here until JsActivationThread returns and so it doesnot set _sentServerStopping to true
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())// To enable the JsActivationThread to check for serverStopping notification while its still running, this method should not be synchronized.
            SibTr.entry(tc, "okayToSendServerStopping", this);

        synchronized (lockObject) { // This should be run in a synchronized context 
            if (!_sentServerStopping) {
                if ((_state == STATE_STARTED || _state == STATE_AUTOSTARTING || _state == STATE_STARTING) && _mainImpl.isServerStopping()) { // Adding additional ME states( STARTING and AUTOSTARTING) , so that sever stopping notification is also sent when ME is in those states.
                    _sentServerStopping = true;
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "okayToSendServerStopping", new Boolean(
                            _sentServerStopping));
        return _sentServerStopping;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#serverStopping()
     */
    public final void serverStopping() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "serverStopping", this);
        if (okayToSendServerStopping()) {

            Iterator<ComponentList> vIter = jmeComponents.iterator();
            while (vIter.hasNext()) {
                vIter.next().getRef().serverStopping();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "serverStopping");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#stop(int)
     */
    public void stop(int mode) {
        SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", CLASS_NAME + ": a subclass has not overriden stop()");
    }

    public void stop() {

        String thisMethodName = "stop";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
        }

        if (_state == STATE_STOPPING) {
            SibTr.error(tc, "ME_STATE_CHECK_SIAS0028", new Object[] { _name, getState() });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "stop");
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Stop ME, mode defaulting to ME_STOP_IMMEDIATE");
        stop(JsConstants.ME_STOP_IMMEDIATE);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /**
     * 
     * This is an unconditional stop() which accepts "mode".
     */
    public void stop(String mode) {

        String thisMethodName = "stop";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, mode);
        }

        // Now signal the stop request
        stop();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /**
     */
    public void stopConditional(int mode) {

        String thisMethodName = "stopConditional";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, Integer.toString(mode));
        }
        setState(STATE_STOPPING);
        stop(mode);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Deactivating MBean for ME " + getName());
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#destroy()
     */
    public void destroy() {
        SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", CLASS_NAME + ": a subclass has not overriden destroy()");
    }

    // General query and utility methods

    /*
     * This method will return false for liberty release
     */
    public final boolean datastoreExists() {

        String thisMethodName = "datastoreExists";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, new Boolean(false));
        }
        return false;
    }

    /*
     * This will return true for liberty
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#filestoreExists()
     */
    public final boolean filestoreExists() {

        String thisMethodName = "filestoreExists";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, new Boolean(false));
        }

        return true;
    }

    /*
     * Will return null for liberty
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getBus()
     */
    public final LWMConfig getBus() {

        String thisMethodName = "getBus";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }

        return this._bus;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getBusName()
     */
    public final String getBusName() {

        String thisMethodName = "getBusName";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
        }

        String retVal = _bus.getName();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, retVal);
        }

        return retVal;
    }

    /*
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getMessageStoreType()
     */
    public final int getMessageStoreType() {

        String thisMethodName = "getMessageStoreType";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
        }

        // filestore
        int msType = JsMessagingEngine.MESSAGE_STORE_TYPE_FILESTORE;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, new Integer(msType));
        }
        return msType;
    }

    /*
     * This will return null for liberty
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getDatastore()
     */
    public final LWMConfig getDatastore() {

        String thisMethodName = "getDatastore";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
            SibTr.exit(tc, thisMethodName, null);
        }

        return null;
    }

    /*
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getFilestore()
     */
    public final LWMConfig getFilestore() {

        String thisMethodName = "getFilestore";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
            SibTr.exit(tc, thisMethodName, _fs);
        }

        return _fs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getEngineComponent(java.lang.String)
     */
    public JsEngineComponent getEngineComponent(String className) {

        String thisMethodName = "getEngineComponent";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, className);
        }

        JsEngineComponent foundEngineComponent = null;

        // No need to synchronize on jmeComponents to prevent concurrent
        // modification from dynamic config and runtime callbacks, because
        // jmeComponents is now a CopyOnWriteArrayList.
        Iterator<ComponentList> vIter = jmeComponents.iterator();

        while (vIter.hasNext() && foundEngineComponent == null) {
            ComponentList c = vIter.next();
            if (c.getClassName().equals(className)) {
                foundEngineComponent = c.getRef();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, foundEngineComponent);
        }

        return foundEngineComponent;
    }

    /* ------------------------------------------------------------------------ */
    /*
     * getEngineComponent method
     * /* ------------------------------------------------------------------------
     */
    /**
     * The purpose of this method is to get an engine component that can be cast using the
     * specified class. The EngineComponent is not a class name, but represents the classes
     * real type.
     * 
     * @param <EngineComponent> The generic type of the desired engine component.
     * @param clazz The class representing the engine component interface.
     * @return The desired engine component.
     */
    @SuppressWarnings("unchecked")
    public <EngineComponent> EngineComponent getEngineComponent(Class<EngineComponent> clazz) {
        String thisMethodName = "getEngineComponent";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, clazz);
        }

        JsEngineComponent foundEngineComponent = null;

        // No need to synchronize on jmeComponents to prevent concurrent
        // modification from dynamic config and runtime callbacks, because
        // jmeComponents is now a CopyOnWriteArrayList.
        Iterator<ComponentList> vIter = jmeComponents.iterator();

        while (vIter.hasNext() && foundEngineComponent == null) {
            ComponentList c = vIter.next();
            if (clazz.isInstance(c.getRef())) {
                foundEngineComponent = c.getRef();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, foundEngineComponent);
        }

        return (EngineComponent) foundEngineComponent;
    }

    /* ------------------------------------------------------------------------ */
    /*
     * getEngineComponents method
     * /* ------------------------------------------------------------------------
     */
    /**
     * The purpose of this method is to get an engine component that can be cast using the
     * specified class. The EngineComponent is not a class name, but represents the classes
     * real type. It is not possible to create an Array of a generic type without using
     * reflection.
     * 
     * @param <EngineComponent> The generic type of the desired engine component.
     * @param clazz The class representing the engine component interface.
     * @return An array of engine components that match the desired
     *         engine component type.
     */
    @SuppressWarnings("unchecked")
    public <EngineComponent> EngineComponent[] getEngineComponents(Class<EngineComponent> clazz) {
        String thisMethodName = "getEngineComponents";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, clazz);
        }

        List<EngineComponent> foundComponents = new ArrayList<EngineComponent>();

        // No need to synchronize on jmeComponents to prevent concurrent
        // modification from dynamic config and runtime callbacks, because
        // jmeComponents is now a CopyOnWriteArrayList.
        Iterator<ComponentList> vIter = jmeComponents.iterator();

        while (vIter.hasNext()) {
            JsEngineComponent ec = vIter.next().getRef();
            if (clazz.isInstance(ec)) {
                foundComponents.add((EngineComponent) ec);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, foundComponents);
        }

        // this code is required to get an array of the EngineComponent type.
        return foundComponents.toArray((EngineComponent[]) Array.newInstance(clazz, 0));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEObject#getEObject()
     */
    public final Object getEObject() {

        String thisMethodName = "getEObject";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
            SibTr.exit(tc, thisMethodName, _me);
        }

        return _me;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getMBeanFactory()
     */
    /*
     * public ControllableRegistrationService getMBeanFactory() {
     * 
     * String thisMethodName = "getMBeanFactory";
     * 
     * if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
     * SibTr.entry(tc, thisMethodName);
     * SibTr.exit(tc, thisMethodName, _mbeanFactory);
     * }
     * 
     * return _mbeanFactory;
     * }
     */

    /**
     * Returns a reference to this messaging engine's configuration.
     * 
     * @return a reference to this messaging engine's configuration.
     */
    public final JsMEConfig getMeConfig() {

        String thisMethodName = "getMeConfig";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
            SibTr.exit(tc, thisMethodName, _me);
        }

        return _me;
    }

    /*
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getMessageProcessor()
     */
    public JsEngineComponent getMessageProcessor() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getMessageProcessor", this);
            SibTr.exit(tc, "getMessageProcessor", _messageProcessor);
        }
        return _messageProcessor;
    }

    /*
     * This will return null for liberty
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getTRMComponent()
     */
    public JsEngineComponent getTRMComponent() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getTRMComponent", this);
            SibTr.exit(tc, "getTRMComponent", _trm);
        }
        return _trm;
    }

    /**
     * Gets the instance of the MP associated with this ME
     * 
     * @deprecated
     * @param name
     * @return JsEngineComponent
     */
    @Deprecated
    public JsEngineComponent getMessageProcessor(String name) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getMessageProcessor", this);
            SibTr.exit(tc, "getMessageProcessor", _messageProcessor);
        }

        return _messageProcessor;
    }

    /*
     * will return null for liberty
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getMessageStore()
     */
    public Object getMessageStore() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getMessageStore", this);
            SibTr.exit(tc, "getMessageStore", null);
        }

        return _messageStore;
    }

    /*
     * This will return null for liberty
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getMQClientLink(java.lang.String)
     */
    public LWMConfig getMQClientLink(String name) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getMQClientLink", name);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getMQClientLink", null);

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getMQLink(java.lang.String)
     */
    public LWMConfig getMQLink(String name) {

        String thisMethodName = "getMQLink";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, name);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, null);
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getMQLinkEngineComponent(java.lang.String)
     */
    public JsEngineComponent getMQLinkEngineComponent(String name) {

        String thisMethodName = "getMQLinkEngineComponent";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, name);
        }

        // MQ link no longer an engine component
        JsEngineComponent rc = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, rc);
        }

        return rc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getGatewayLink(java.lang.String)
     */
    public LWMConfig getGatewayLink(String name) {

        String thisMethodName = "getGatewayLink";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, name);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getGatewayLinkEngineComponent(java.lang.String)
     */
    public JsEngineComponent getGatewayLinkEngineComponent(String name) {

        String thisMethodName = "getGatewayLinkEngineComponent";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, name);
        }

        JsEngineComponent rc = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, rc);
        }

        return rc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.impl.JsObject#getName()
     */
    public String getName() {

        String thisMethodName = "getName";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
            SibTr.exit(tc, thisMethodName, _name);
        }

        return _name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.runtime.component.ComponentImpl#getService(java.lang.Class)
     */
    /*
     * public final Object getService(Class c) {
     * 
     * String thisMethodName = "getService";
     * 
     * if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
     * SibTr.entry(tc, thisMethodName, c.getClass().getName());
     * SibTr.exit(tc, thisMethodName);
     * }
     * 
     * return _mainImpl.getService(c);
     * }
     */

    /**
     * Resolve, if necessary, a variable Exception Destination name in the
     * specified DD to it's runtime value.
     * 
     * @param dd
     *            the DestinationDefinition to be checked, and modified
     */
    private void resolveExceptionDestination(BaseDestinationDefinition dd) {

        String thisMethodName = "resolveExceptionDestination";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, dd.getName());
        }

        // If variable substitution string detected, then override ED name
        if (dd.isLocal()) {
            String ed = ((DestinationDefinition) dd).getExceptionDestination();
            if ((ed != null) && (ed.equals(JsConstants.DEFAULT_EXCEPTION_DESTINATION))) {
                ((DestinationDefinition) dd).setExceptionDestination(JsConstants.EXCEPTION_DESTINATION_PREFIX + this.getName());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getSIBDestination(java.lang.String,
     * java.lang.String, com.ibm.ws.sib.admin.DestinationDefinition)
     */
    public void getSIBDestination(String busName, String name, DestinationDefinition dd) throws SIBExceptionBase, SIBExceptionDestinationNotFound {

        String thisMethodName = "getSIBDestination";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, new Object[] { busName, name, dd.getName() });
        }

        if (oldDestCache == null) {
            _bus.getDestinationCache().getSIBDestination(busName, name, dd);
        } else {
            oldDestCache.getSIBDestination(busName, name, dd);
        }

        // Resolve Exception Destination if necessary
        resolveExceptionDestination(dd);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getSIBDestination(java.lang.String,
     * java.lang.String)
     */
    public BaseDestinationDefinition getSIBDestination(String busName, String name) throws SIBExceptionBase, SIBExceptionDestinationNotFound {

        String thisMethodName = "getSIBDestination";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, new Object[] { busName, name });
        }

        BaseDestinationDefinition bdd = null;
        if (oldDestCache == null) {
            bdd = (BaseDestinationDefinition) _bus.getDestinationCache().getSIBDestination(busName, name).clone();
        } else {
            bdd = (BaseDestinationDefinition) oldDestCache.getSIBDestination(busName, name).clone();
        }

        // Resolve Exception Destination if necessary
        resolveExceptionDestination(bdd);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, bdd);
        }

        return bdd;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getSIBDestinationByUuid(java.lang.String,
     * java.lang.String)
     */
    public BaseDestinationDefinition getSIBDestinationByUuid(String busName, String uuid) throws SIBExceptionBase, SIBExceptionDestinationNotFound {

        String thisMethodName = "getSIBDestinationByUuid";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, new Object[] { busName, uuid });
        }

        BaseDestinationDefinition bdd = getSIBDestinationByUuid(busName, uuid, false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, bdd);
        }

        return bdd;
    }

    /**
     * Accessor method to return a destination definition.
     * 
     * @param bus
     * @param key
     * @param newCache
     * @return the destination cache
     */
    BaseDestinationDefinition getSIBDestinationByUuid(String bus, String key, boolean newCache) throws SIBExceptionDestinationNotFound, SIBExceptionBase {

        String thisMethodName = "getSIBDestinationByUuid";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, new Object[] { bus, key, new Boolean(newCache) });
        }

        BaseDestinationDefinition bdd = null;

        if (oldDestCache == null || newCache) {
            bdd = (BaseDestinationDefinition) _bus.getDestinationCache().getSIBDestinationByUuid(bus, key).clone();
        } else {
            bdd = (BaseDestinationDefinition) oldDestCache.getSIBDestinationByUuid(bus, key).clone();
        }

        // Resolve Exception Destination if necessary
        resolveExceptionDestination(bdd);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, bdd);
        }

        return bdd;
    }

    /**
     * Accessor method to return a Destination Locality. The newCache parameter
     * allows access to the new cache when an old one is still in use.
     * 
     * @param busName
     * @param uuid
     * @param newCache
     *            If true returns values from new cache
     */
    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getSIBDestinationLocalitySet(java.lang.String,
     * java.lang.String, boolean)
     */
    public Set getSIBDestinationLocalitySet(String busName, String uuid, boolean newCache) throws SIBExceptionBase, SIBExceptionDestinationNotFound {

        String thisMethodName = "getSIBDestinationLocalitySet";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, new Object[] { busName, uuid, new Boolean(newCache) });
        }

        if (oldDestCache == null || newCache) {

            Set retVal = _bus.getDestinationCache().getSIBDestinationLocalitySet(busName, uuid);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getSIBDestinationLocalitySet", retVal);

            return retVal;
        }

        Set retVal = oldDestCache.getSIBDestinationLocalitySet(busName, uuid);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, retVal);
        }

        return retVal;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getSIBDestinationLocalitySet(java.lang.String,
     * java.lang.String)
     */
    public Set getSIBDestinationLocalitySet(String busName, String uuid) throws SIBExceptionBase, SIBExceptionDestinationNotFound {

        String thisMethodName = "getSIBDestinationLocalitySet";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, new Object[] { busName, uuid });
        }

        Set retVal = getSIBDestinationLocalitySet(busName, uuid, false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, retVal);
        }

        return retVal;
    }

    /**
     * Get the state of this ME
     * 
     * @return String
     */
    public final String getState() {

        String thisMethodName = "getState";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
            SibTr.exit(tc, thisMethodName, states[_state]);
        }

        return states[_state];
    }

    // 215830
    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getStatsGroup()
     
    public final StatsGroup getStatsGroup() {

        String thisMethodName = "getStatsGroup";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
            SibTr.exit(tc, thisMethodName, _meStatsGroup);
        }

        return _meStatsGroup;
    }*/

    // end 215830

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#getUuid()
     */
    public String getUuid() {

        String thisMethodName = "getUuid";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
        }
//
//        if (_uuid == null) {
//            String uid =this._uuid;// _me.getMessagingEngine().getUuid();
//            if(_uuid!=null){
//                _uuid = new SIBUuid8(uid);
//            }
//            
//        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, _uuid);
        }

        return _uuid;
    }

    /**
     * Returns an indication of whether this ME is started or not
     * 
     * @return true if this messaging egine is started; else false.
     */

    public final boolean isStarted() {

        String thisMethodName = "isStarted";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
        }

        boolean retVal = (_state == STATE_STARTED);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, Boolean.toString(retVal));
        }

        return retVal;
    }

    /*
     * @see com.ibm.ws.sib.admin.JsMessagingEngine#loadLocalizations()
     */
    public final void loadLocalizations() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "loadLocalizations", this);

        _localizer.loadLocalizations();
        try {
            setJsDestinationUUIDCache(_localizer.updatedDestDefList);
        } catch (Exception e) {
            SibTr.entry(tc, "Exception while updating UUID Destination Cache in ", e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "loadLocalizations");
    }

    private int setJsDestinationUUIDCache(ArrayList list) throws Exception {
        SibTr.entry(tc, "setJsDestinationUUIDCache");
        int result = 0;
        try {
            JsDestinationCache dCache = this._bus.getDestinationCache();
            dCache.populateUuidCache(list);
            result = 1;
        } catch (Exception e) {
            e.printStackTrace();
            SibTr.error(tc, "Exception during UUID cache update after loadlocalizations");

        }
        SibTr.exit(tc, "setJsDestinationUUIDCache");
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#setConfig(JsEObject)
     */
    public void setConfig(LWMConfig me) {

        String thisMethodName = "setConfig";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, new Object[] { me });
        }
        meHighMessageThreshold=this._me.getMessagingEngine().getHighMessageThreshold();
        // No need to synchronize on jmeComponents to prevent concurrent
        // modification from dynamic config and runtime callbacks, because
        // jmeComponents is now a CopyOnWriteArrayList.
        // Pass the attributes to all engine components
        Iterator<ComponentList> vIter = jmeComponents.iterator();
        while (vIter.hasNext()) {
            vIter.next().getRef().setConfig(me);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /*
     * 181851 setter method for _state - encapsulates debug of state change.
     */
    protected final void setState(int state) {

        String thisMethodName = "setState";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, Integer.toString(state));
        }

        _state = state;
        // 212263 - State changes promoted from debug to info

        // 227363 - Only use info for certain state changes. Other state changes
        // that are less interesting to users are kept as debug only. The rationale
        // for the split is as follows:
        // STATE_UNINITIALIZED => debug, because not interesting
        // STATE_INITIALIZING => debug, because trivial change
        // STATE_INITIALIZED => debug, limited interest
        // STATE_JOINING => debug, useful when DCS/HAM problems
        // STATE_JOINED => info, because hi-lites DCS/HAM problems
        // STATE_AUTOSTARTING => debug, not interesting
        // STATE_STARTING => info, need to know ME is trying to start
        // STATE_STARTED => info, essential knowledge
        // STATE_STOPPING => info, useful indication that ME is trying
        // STATE_STOPPING_MEMBER => info, useful cos only state on deactivate path
        // STATE_STOPPED => info, essential knowledge
        // STATE_DESTROYING => debug, limited interest
        // STATE_DESTROYED => debug, limited interest

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.impl.JsMessagingEngineMBean#state()
     */
    public final String state() {

        String thisMethodName = "state";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
        }

        String state = getState();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, state);
        }

        return state;
    }

//    /**
//     * Mark the begining of a block of updates to the localizer object.
//     */
//    final void startLocalizationPointUpdates() {
//
//        String thisMethodName = "startLocalizationPointUpdates";
//
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
//            SibTr.entry(tc, thisMethodName, this);
//        }
//
//        _localizer.startUpdate();
//
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
//            SibTr.exit(tc, thisMethodName);
//        }
//    }

//    /**
//     * Mark the end of a block of updates to the localizer object.
//     */
//    final void endLocalizationPointUpdates() {
//
//        String thisMethodName = "endLocalizationPointUpdates";
//
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
//            SibTr.entry(tc, thisMethodName, this);
//        }
//
//        _localizer.endUpdate();
//
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
//            SibTr.exit(tc, thisMethodName);
//        }
//    }

    /**
     * Pass the request to add a new localization point onto the localizer object.
     * 
     * @param lp localization point definition
     * @return boolean success Whether the LP was successfully added
     */
    final boolean addLocalizationPoint(LWMConfig lp, DestinationDefinition dd) {

        String thisMethodName = "addLocalizationPoint";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, lp);
        }

        boolean success = _localizer.addLocalizationPoint(lp, dd);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, Boolean.valueOf(success));
        }
        return success;
    }

    /**
     * Pass the request to alter a localization point onto the localizer object.
     * 
     * @param lp
     *            localization point definition
     */
    final void alterLocalizationPoint(BaseDestination dest,LWMConfig lp) {

        String thisMethodName = "alterLocalizationPoint";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, lp);
        }

        try {
            _localizer.alterLocalizationPoint(dest,lp);
        } catch (Exception e) {
            SibTr.exception(tc, e);            
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /**
     * Pass the request to delete a localization point onto the localizer object.
     * 
     * @param lp localization point definition
     */
    final void deleteLocalizationPoint(JsBus bus, LWMConfig dest) {

        String thisMethodName = "deleteLocalizationPoint";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, dest);
        }

        try {
            _localizer.deleteLocalizationPoint(bus, dest);
        } catch (SIBExceptionBase ex) {
            SibTr.exception(tc, ex);

        } catch (SIException e) {
            SibTr.exception(tc, e);

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /**
     * Return the cache of localization point config objects (not a copy).
     * 
     * @return Returns the _lpConfig.
     */
    final ArrayList getLPConfigObjects() {

        String thisMethodName = "getLPConfigObjects";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
            SibTr.exit(tc, thisMethodName, _lpConfig);
        }

        return _lpConfig;
    }

    /**
     * Update the cache of localization point config objects. This method is used
     * by dynamic config to refresh the cache.
     * 
     * @param config The _lpConfig to set.
     */
    final void setLPConfigObjects(List config) {

        String thisMethodName = "setLPConfigObjects";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, "Number of LPs =" + config.size());
        }

        _lpConfig.clear();
        _lpConfig.addAll(config);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName);
        }
    }

    /**
     * Test whether the Event Notification property has been set. This method
     * resolves the setting for the specific ME and the setting for the bus.
     */
    public boolean isEventNotificationPropertySet() {

        String thisMethodName = "isEventNotificationPropertySet";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
        }

        boolean enabled = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, Boolean.toString(enabled));
        }

        return enabled;
    }

    /**
     * Returns a reference to the repository service. This is used in conjunction
     * with ConfigRoot to access EMF configuration documents.
     * 
     * @return Reference to the repository service.
     */
    public JsMainImpl getMainImpl() {

        String thisMethodName = "getMainImpl";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, this);
            SibTr.exit(tc, thisMethodName, _mainImpl);
        }

        return _mainImpl;
    }

    /**
     * To be called during modified method call to ME in liberty
     * configuration document has changed.
     * 
     * @param me New ME object read from the sib-engines.xml configuration file.
     * 
     */
//    protected void reloadEngine(JsMEConfig newMe) {
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
//            SibTr.entry(tc, "reloadEngine", newMe);
//        }

//    setEObject(newMe); TBD
//        _me = newMe;
//    _name = newMe.getString(CT_SIBMessagingEngine.NAME_NAME, CT_SIBMessagingEngine.NAME_DEFAULT); TBD

    // TBD get SIBFileSoreIMpl instance here 
    /*
     * ConfigObject filestore = newMe.getObject(CT_SIBMessagingEngine.FILESTORE_NAME);
     * 
     * if (filestore == null) {
     * _fs = null;
     * }
     * else {
     * _fs = new JsFilestoreImpl(filestore);
     * }
     */
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
//            SibTr.exit(tc, "reloadEngine");
//        }
//    }

    public void setMEUUID(SIBUuid8 uuid) {
        this._me.getMessagingEngine().setUuid(uuid.toString());
        this._uuid=uuid.toString();
    }


    /** {@inheritDoc} */
    @Override
    public void busReloaded(Object newBus, boolean busChanged, boolean destChg, boolean medChg) {
    // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void engineReloaded(Object engine) {
    // TODO Auto-generated method stub

    }

//    /** {@inheritDoc} */
//    @Override
//    public void initialize(Object engine) {
//    // TODO Auto-generated method stub
//
//    }

    /** Mock JsEngineComponent method */
    @Override
    public void setCustomProperty(String name, String value) {

    }

    /**
     * Load the named class and add it to the list of engine components.
     * 
     * @param className
     *            Fully qualified class name of class to load.
     * @param stopSeq
     * @param reportError
     *            Set to false for classes we don't care about.
     * @return
     */
    protected final JsEngineComponent loadClass(String className, int stopSeq, boolean reportError) {

        String thisMethodName = "loadClass";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, thisMethodName, new Object[] { className, Integer.toString(stopSeq), Boolean.toString(reportError) });
        }

        Class myClass = null;
        JsEngineComponent retValue = null;

        int _seq = stopSeq;
        if (_seq < 0 || _seq > NUM_STOP_PHASES) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "loadClass: stopSeq is out of bounds " + stopSeq);
        } else {
            try {
                myClass = Class.forName(className);
                retValue = (JsEngineComponent) myClass.newInstance();
                ComponentList compList = new ComponentList(className, retValue);
                // No need to synchronize on jmeComponents to prevent concurrent
                // modification from dynamic config and runtime callbacks, because
                // jmeComponents is now a CopyOnWriteArrayList.
                jmeComponents.add(compList);
                stopSequence[_seq].addElement(compList);
            } catch (ClassNotFoundException e) {
                // No FFDC code needed
                if (reportError == true) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(e, CLASS_NAME + ".loadClass", "3", this);
                    SibTr.error(tc, "CLASS_LOAD_FAILURE_SIAS0013", className);
                    SibTr.exception(tc, e);
                }

            } catch (InstantiationException e) {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, CLASS_NAME + ".loadClass", "1", this);
                if (reportError == true) {
                    SibTr.error(tc, "CLASS_LOAD_FAILURE_SIAS0013", className);
                    SibTr.exception(tc, e);
                }

            } catch (Throwable e) {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, CLASS_NAME + ".loadClass", "2", this);
                if (reportError == true) {
                    SibTr.error(tc, "CLASS_LOAD_FAILURE_SIAS0013", className);

                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (retValue == null)
                SibTr.debug(tc, "loadClass: failed");
            else
                SibTr.debug(tc, "loadClass: OK");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, thisMethodName, retValue);
        }

        return retValue;
    }
    public long getMEThreshold()
    { 
    return this.meHighMessageThreshold;
    }    
    
    public String[] listPreparedTransactions()
    {
        String thisMethodName = "listPreparedTransactions";
        if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, thisMethodName);
        String[] col = null;
        if(_messageStore != null)
            col = _messageStore.listPreparedTransactions();
        if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, thisMethodName, col);
        return col;
    }
    public void resetDestination(String name) throws Exception
    {
    String thisMethodName = "resetDestination";
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, thisMethodName, name);
    if(_messageProcessor != null && _mpAdmin != null)
        try
        {
            _mpAdmin.getMPRuntimeControl().resetDestination(name);
        }
        catch(SIMPRuntimeOperationFailedException e)
        {
            SibTr.exception(tc, e);
            if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "resetDestination", e);
            throw new Exception(e.getMessage());
        }
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, thisMethodName);
    }
    
   
    private byte[] getData(byte in[], Integer size)
    {
        if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getData", ((Object) (new Object[] {
                in, size
            })));
        byte tmp[] = (byte[])null;
        if(in != null)
        {
            int len = 1024;
            if(size.intValue() > 0)
                len = size.intValue();
            if(len > in.length)
                len = in.length;
            tmp = new byte[len];
            System.arraycopy(in, 0, tmp, 0, len);
        }
        if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getData", tmp);
        return tmp;
    }
    
  
    
    public void commitPreparedTransaction(String xid) throws SIBTransactionException, SIBPersistenceException
    {
    String thisMethodName = "commitPreparedTransaction";
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, thisMethodName, xid);
    if(_messageStore != null)
        try
        {
            _messageStore.commitPreparedTransaction(xid);
        }
        catch(TransactionException e)
        {
            FFDCFilter.processException(e, "com.ibm.ws.sib.admin.impl.BaseMessagingEngineImpl.commitPreparedTransaction", "1:3417:1.79", this);
            if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "commitPreparedTransaction", "SIBTransactionException");
            throw new SIBTransactionException(e.getMessage());
        }
        catch(PersistenceException e)
        {
            FFDCFilter.processException(e, "com.ibm.ws.sib.admin.impl.BaseMessagingEngineImpl.commitPreparedTransaction", "1:3430:1.79", this);
            if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "commitPreparedTransaction", "SIBPersistenceException");
            throw new SIBPersistenceException(e.getMessage());
        }
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, thisMethodName);
    }
    
    public void rollbackPreparedTransaction(String xid)  throws SIBTransactionException, SIBPersistenceException
    {
    String thisMethodName = "rollbackPreparedTransaction";
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, thisMethodName, xid);
    if(_messageStore != null)
        try
        {
            _messageStore.rollbackPreparedTransaction(xid);
        }
        catch(TransactionException e)
        {
            FFDCFilter.processException(e, "com.ibm.ws.sib.admin.impl.BaseMessagingEngineImpl.rollbackPreparedTransaction", "1:3476:1.79", this);
            if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "rollbackPreparedTransaction", "SIBTransactionException");
            throw new SIBTransactionException(e.getMessage());
        }
        catch(PersistenceException e)
        {
            FFDCFilter.processException(e, "com.ibm.ws.sib.admin.impl.BaseMessagingEngineImpl.rollbackPreparedTransaction", "1:3489:1.79", this);
            if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "rollbackPreparedTransaction", "SIBPersistenceException");
            throw new SIBPersistenceException(e.getMessage());
        }
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, thisMethodName);
    }
    
    
    public void dump(String dumpSpec)
    {
        String thisMethodName = "dump";
        if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, thisMethodName, dumpSpec);
        FormattedWriter fw = null;
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String dumpFile = (new StringBuilder("SIBdump-")).append(getName()).append("-").append(dateFormat.format(date)).append(".xml").toString();
        try
        {
            String dumpFilePath = dumpFile;
            if(dumpDir != null) {
                dumpFilePath = new StringBuilder(String.valueOf(dumpDir)).append(dumpFile).toString();
            }
            FileOutputStream fos = new FileOutputStream(dumpFilePath);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            fw = new FormattedWriter(osw);
        }
        catch(IOException e)
        {
            FFDCFilter.processException(e, "com.ibm.ws.sib.admin.impl.BaseMessagingEngineImpl.dump", "1:2837:1.79", this);
            SibTr.exception(tc, e);
        }
        if(fw != null)
            try
            {
                fw.introducer("xml version=\"1.0\" encoding=\"UTF-8\"");
                fw.comment((new StringBuilder(" Dump taken at ")).append((new SimpleDateFormat("HH:mm:ss:SSS")).format(date)).append(" on ").append((new SimpleDateFormat("dd MMMM yyyy")).format(date)).append(" ").toString());
                fw.newLine();
                fw.nameSpace("xmi");
                fw.startTag("XMI xmi:version=\"1.2\" xmlns:xmi=\"http://www.omg.org/XMI\" xmlns:admin=\"?\" xmlns:comms=\"?\" xmlns:mfp=\"?\" xmlns:msgstore=\"?\" xmlns:processor=\"?\" xmlns:security=\"?\" xmlns:trm=\"?\"");
                fw.indent();
                fw.newLine();
                fw.nameSpace("");
                fw.startTag("sib");
                fw.indent();
                fw.newLine();
                fw.nameSpace("msgstore");
                if(_messageStore != null)
                    dump(dumpSpec, "com.ibm.ws.sib.msgstore.impl.MessageStoreImpl", ((JsEngineComponent) (_messageStore)), fw);
                fw.nameSpace("trm");
                JsEngineComponent _trm = getEngineComponent("com.ibm.ws.sib.trm.TrmMeMainImpl");
                if(_trm != null)
                    dump(dumpSpec, "com.ibm.ws.sib.trm.TrmMeMainImpl", _trm, fw);
                fw.outdent();
                fw.newLine();
                fw.nameSpace("");
                fw.endTag("sib");
                fw.outdent();
                fw.newLine();
                fw.nameSpace("xmi");
                fw.endTag("XMI");
                fw.newLine();
                fw.flush();
                fw.close();
            }
            catch(IOException e)
            {
                FFDCFilter.processException(e, "com.ibm.ws.sib.admin.impl.BaseMessagingEngineImpl.dump", "1:2897:1.79", this);
                SibTr.exception(tc, e);
            }
        if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, thisMethodName);
    }

    private void dump(String dumpSpec, String className, JsEngineComponent comp, FormattedWriter fw)
    {
        if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "dump", ((Object) (new Object[] {
                dumpSpec, className, comp, fw
            })));
        StringTokenizer st = new StringTokenizer(dumpSpec, ":");
        int i = 0;
        for(boolean matched = false; st.hasMoreTokens() && !matched;)
        {
            i++;
            String item = st.nextToken();
            int j = item.indexOf('=');
            String pkg;
            String arg;
            if(j < 0)
            {
                pkg = item.trim();
                arg = "";
            } else
            {
                pkg = item.substring(0, j).trim();
                arg = item.substring(j + 1).trim();
            }
            boolean fullMatch = true;
            if(pkg.endsWith("*"))
            {
                fullMatch = false;
                pkg = pkg.substring(0, pkg.length() - 1);
            }
            if(fullMatch)
            {
                if(pkg.equals(className))
                    matched = true;
            } else
            {
                int keyLength = pkg.length();
                String component = className;
                if(pkg.endsWith(".") && !component.endsWith("."))
                    component = (new StringBuilder(String.valueOf(component))).append(".").toString();
                if(component.length() >= keyLength && pkg.equals(component.substring(0, keyLength)))
                    matched = true;
            }
            if(matched && (comp instanceof MessageStore))
                ((MessageStore)comp).dump(fw, arg);
        }

        if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "dump");
    }
    
    @Override
    public String getConfigId() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String getId() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String getRemoteEngineUuid() {
        // TODO Auto-generated method stub
        return null;
    }
    
}
