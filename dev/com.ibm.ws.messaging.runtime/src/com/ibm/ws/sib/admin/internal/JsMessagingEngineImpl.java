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

import java.util.ListIterator;
import java.util.Vector;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.messaging.mbean.MessagingEngineMBean;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsEngineComponent;
import com.ibm.ws.sib.admin.JsMEConfig;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.LWMConfig;
import com.ibm.ws.sib.msgstore.SeverePersistenceException;
import com.ibm.ws.sib.processor.SIMPAdmin;
import com.ibm.ws.sib.utils.ras.SibTr;

public class JsMessagingEngineImpl extends BaseMessagingEngineImpl implements JsMessagingEngine, JsEngineComponent, LWMConfig, MessagingEngineMBean
{

    private static final TraceComponent tc = SibTr.register(JsMessagingEngineImpl.class, JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);

    // 181851 - added class name to keep FFDC strings short.
    private static final String CLASS_NAME = "com.ibm.ws.sib.admin.internal.JsMessagingEngineImpl";

    private static final TraceNLS nls = TraceNLS.getTraceNLS(JsConstants.MSG_BUNDLE);

    // Flag that signals whether Event Notification is enabled.
    // By default it is not.
    private final boolean _eventNotificationEnabled = false;

    JsObject object = new JsObject(JsConstants.MBEAN_TYPE_ME, _name);

    /**
     * @see java.lang.Object#Object()
     */
    public JsMessagingEngineImpl(JsMainImpl mainImpl, JsBusImpl bus, JsMEConfig me) throws Exception {
        super(mainImpl, bus, me);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "JsMessagingEngineImpl().<init>", this);
        _mbeanFactory = new MessagingMBeanFactoryImpl(this, mainImpl.bContext);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "JsMessagingEngineImpl().<init>");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#initialize(com.ibm.ws.sib.admin.JsMessagingEngine)
     */
    @Override
    public void initialize(JsMessagingEngine engine) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "initialize", engine);

        // Get the admin factory class
        try {
            jsaf = JsAdminFactory.getInstance();
        } catch (Exception e) {
            // No FFDC code needed
        }

        // Synchronize on stateChangeLock to manage the startup of the engine with
        // potential addition of new engine components via dynamic config.
//        processor = _mainImpl.mpService.getService().getInstance();
        ListIterator<ComponentList> li = null;
        boolean initializationFailed = false;

        synchronized (stateChangeLock) {

            setState(STATE_INITIALIZING);
            for (int i = 0; i < NUM_STOP_PHASES; i++)
            {
                stopSequence[i] = new Vector<ComponentList>();
            }
            try
            {
                _messageStore = JsMainAdminComponentImpl.messageStoreRef.getService();

            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            _messageProcessor = loadClass(JsConstants.SIB_CLASS_MP, STOP_PHASE_1, true);

            // Initialize any engine components
            initializationFailed = false;

        } // synchronized(stateChangeLock)
        try {
            //Initialize the message store 
            _messageStore.initialize(this);
            //Initialize the message processor 
            _messageProcessor.initialize(this);
            setState(STATE_INITIALIZED);
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, CLASS_NAME + ".<init>", "625", this);
            SibTr.error(tc, "ME_CANNOT_BE_INITIALIZED_SIAS0033", new Object[] { _name, "MessageProcessor.java", "initialize()" });
            SibTr.exception(tc, e);
            initializationFailed = true;

        }
        if (initializationFailed) {
            destroy();
            // Cleanup finished...
            // Change state to UNINITIALIZED
            setState(STATE_UNINITIALIZED);
        } else {
            setState(STATE_INITIALIZED);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "initialize");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#start(int)
     */
    @Override
    public void start(int mode) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "start, mode ", mode);

//        _eventNotificationEnabled = isEventNotificationPropertySet();
//        notifyMessagingEngineStarting(getName(), getUuid().toString(), "STANDARD_MODE");

        boolean startFailed = false;
        ListIterator<ComponentList> li = null;
        synchronized (stateChangeLock) {

            if (_state == STATE_STOPPED)
                setState(STATE_STARTING);

        } // synchronized (stateChangeLock)

        try {
            //Start the message store 
            _messageStore.start();
            //Start the message processor
            _messageProcessor.start(0);
            setState(STATE_STARTED);
        } catch (Exception e) {
            if (!(e.getCause() instanceof SeverePersistenceException))
                com.ibm.ws.ffdc.FFDCFilter.processException(e, CLASS_NAME + ".start", "626", this);
            SibTr.error(tc, "ME_CANNOT_BE_STARTED_SIAS0034", new Object[] { _name, "", "start()" });
            SibTr.exception(tc, e);
            startFailed = true;
        }

        if (startFailed) {
            SibTr.error(tc, "ME_RESTART_CHECK_SIAS0027", getName());
            stop(JsConstants.ME_STOP_FORCE);
            destroy();
            setState(STATE_STOPPED);
        } else {
            // Find the MP administrator class
            _mpAdmin = ((SIMPAdmin) getMessageProcessor()).getAdministrator();
            setState(STATE_STARTED);
        }

        if (!startFailed) {
            serverStarted();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Messaging Engine " + _name + " with UUID " + _uuid + " has been started.");
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Messaging engine failed");

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "start");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#stop(int)
     */
    @Override
    public void stop(int mode) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "stop, mode ", mode);

        // Now force state to STOPPING - regardless of how we got here
        setState(STATE_STOPPING);

        // Reset MP Admin
        _localizer.clearMPAdmin();
        _mpAdmin = null;
        _messageProcessor.stop(mode);
        _messageStore.stop(mode);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Messaging Engine " + _name + " finished stop phases");

        setState(STATE_STOPPED);
        SibTr.debug(tc, "Messaging Engine stopped");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Messaging Engine " + _name + " has been stopped before dynamic config cycle completed.");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.debug(tc, "stop");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#destroy()
     */
    @Override
    public void destroy() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "destroy", this);

        setState(STATE_DESTROYING);
        if (_mbeanFactory != null)
            ((MessagingMBeanFactoryImpl) _mbeanFactory).deregisterAll();
        _messageProcessor.destroy();
        _messageStore.destroy();
        setState(STATE_DESTROYED);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "destroy");
    }

    /**
     * Allows a caller to query whether Event Notifications are enabled.
     */
    @Override
    public boolean isEventNotificationEnabled() {
        return _eventNotificationEnabled;
    }

    /**
     * Move state of new component to current engine state
     * 
     * @param JsEngineComponent
     * @throws Exception
     */
    protected void initializeNewEngineComponent(JsEngineComponent engineComponent) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "initializeNewEngineComponent", engineComponent);
        }

        // Synchronize on stateChangeLock to manage the startup of the engine with
        // potential addition of new engine components via dynamic config.

        synchronized (stateChangeLock) {

            if (_state != STATE_UNINITIALIZED && _state != STATE_DESTROYED) {
                // Initialize
                engineComponent.initialize(this);
                // Set ME config
                engineComponent.setConfig(this);
            }

            if (_state == STATE_STARTING || _state == STATE_STARTED) {
                // Start engine component (in recovery mode if needed)
                int mode = JsConstants.ME_START_DEFAULT;
                if (_mainImpl.isServerInRecoveryMode() == true) {
                    mode += JsConstants.ME_START_RECOVERY;
                }
                engineComponent.start(mode);
            }

        } // synchronized (stateChangeLock)

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "initializeNewEngineComponent");
        }
    }

    /**
     * Destroy an engine component that is to be deleted
     * 
     * @param JsEngineComponent
     */
    protected void destroyOldEngineComponent(ComponentList component) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "destroyOldEngineComponent", component);
        }

        boolean removed = jmeComponents.remove(component);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            SibTr.debug(tc, "destroyOldEngineComponent, component removed from jmeComponents?", removed);
        }

        JsEngineComponent engineComponent = component.getRef();

        if (_state == STATE_STARTING || _state == STATE_STARTED || _state == STATE_STOPPING || _state == STATE_STOPPING_MEMBER || _state == STATE_FAILED) {
            // Stop engine component
            engineComponent.stop(JsConstants.ME_STOP_IMMEDIATE);
        }

        if (_state != STATE_UNINITIALIZED && _state != STATE_DESTROYED) {
            // Destroy
            engineComponent.destroy();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "destroyOldEngineComponent");
        }
    }

    private void notifyMessagingEngineStarting(String meName, String meUuid, String startType) {

        // If notification is enabled, fire a notification that the MessagingEngine
        // is starting.

        if (isEventNotificationEnabled()) {

            // Build the message for the notification.

            String message = nls.getFormattedMessage("NOTIFY_MESSAGING_ENGINE_STARTING_SIAS0049", new Object[] { meName, meUuid }, null);
            // log in trace file 
        }
    }

    private void notifyMessagingEngineStarted(String meName, String meUuid, String startType) {

        // If notification is enabled, fire a notification that the MessagingEngine
        // has started.

        if (isEventNotificationEnabled()) {
            // Build the message for the notification.
            String message = "Messaging Engine " + _name + " with UUID " + _uuid + " has been started.";
            // log in trace file
        }
    }

    private void notifyMessagingEngineStopping(String meName, String meUuid, String stopReason) {

        // If notification is enabled, fire a notification that the MessagingEngine
        // is stopping.

        if (isEventNotificationEnabled()) {

            // Build the message for the notification.

            String message = nls.getFormattedMessage("NOTIFY_MESSAGING_ENGINE_STOPPING_SIAS0050", new Object[] { getName(), getUuid().toString() }, null);
            // log in trace file
        }
    }

    private void notifyMessagingEngineStopped(String meName, String meUuid, String stopReason) {

        // If notification is enabled, fire a notification that the MessagingEngine
        // has stopped.

        if (isEventNotificationEnabled()) {

            // Build the message for the notification.

            String message = nls.getFormattedMessage("NOTIFY_MESSAGING_ENGINE_STOP_SIAS0045", new Object[] { getName(), getUuid().toString() }, null);

            // log in trace file
        }
    }

    private void notifyMessagingEngineFailed(String meName, String meUuid, String failOperation, String failOperationType) {

        // If notification is enabled, fire a notification that the MessagingEngine
        // is starting.

        if (isEventNotificationEnabled()) {

            // Build the message for the notification.

            String message = nls.getFormattedMessage("NOTIFY_MESSAGING_ENGINE_STOP_FAILED_SIAS0052", new Object[] { getName(), getUuid().toString() }, null);
            // log in trace file
        }
    }

    public String getMBeanType() {

        return JsConstants.MBEAN_TYPE_ME;
    }

}
