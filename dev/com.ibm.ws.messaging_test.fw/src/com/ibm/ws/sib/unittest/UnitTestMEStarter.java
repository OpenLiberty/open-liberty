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
package com.ibm.ws.sib.unittest;

import java.util.Enumeration;
import java.util.Hashtable;

import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsEngineComponent;
import com.ibm.ws.sib.processor.test.SIMPJsStandaloneEngineImpl;
import com.ibm.ws.sib.processor.test.SIMPJsStandaloneFactory;
import com.ibm.ws.sib.processor.test.SIMPJsStandaloneFactoryImpl;

/**
 * @author caseyj
 * 
 *         This class encompasses parameters for start and restart. It should be used
 *         to control restarts of the ME.
 */
public class UnitTestMEStarter {
    public static JsEngineComponent trm;

    public static SIMPJsStandaloneEngineImpl me;

    private UnitTestMEStartCallback callback;

    public static boolean _isEventNotificationEnabled = false;

    public String busName = null;

    private UnitTestMEStarter() {

    }

    public UnitTestMEStarter(UnitTestMEStartCallback callback) {
        this.callback = callback;
    }

    public UnitTestMEStarter(UnitTestMEStartCallback callback, String busName) {
        this.callback = callback;
        this.busName = busName;
    }

    /**
     * ME objects used in tests.
     */
    private static SIMPJsStandaloneFactoryImpl factory = (SIMPJsStandaloneFactoryImpl) SIMPJsStandaloneFactory.getInstance();

    /**
     * Should we reinstantiate the TRM on the restart?
     */
    private boolean reintTRM = true;

    public void setReintTRM(boolean myReintTRM) {
        reintTRM = myReintTRM;
    }

    /**
     * Cold restart the ME. This means that existing persisted data is
     * discarded before the restart.
     * 
     * @throws Exception
     */
    public void coldRestart() throws Exception {
        // 177411
        // If teardown fails here and we are trying to cold start, we want to 
        // attempt the restart anyway.  This ensures cold starting tests will not
        // unnecessarily fail if a previous test has corrupted us to the extent
        // that we can't shutdown neatly.
        setReintTRM(false);

        try {
            stop();
        } catch (Exception e) {
            System.out.println(
                            "INFO: SIMPTestMEStarter:coldRestart() failed to stop cleanly.");
//       e.printStackTrace();
        }

        coldStart();
    }

    /**
     * Shutdown and restart the messaging engine with persisted data.
     * 
     * @param mpOnly when true will only restart the Message Processor.
     *            This is significantly quicker than restarting every component.
     *            WARNING: But be aware that since the MS is not shutdown, it will return
     *            objects on
     *            restart which still have all their references intact. So some objects
     *            may already exist which you would not expect ordinarily.
     */
    public void warmRestart(boolean mpOnly) throws Exception {
        // set up a simulated WCCM properties file to be used in reconciliation  
        //  UnitTestWCCM.store();

        _stop(mpOnly);

        start(mpOnly);
    }

    /**
     * Start the ME, discarding persisted data.
     * <p>
     * You should not normally call this method yourself - JUnit takes care of
     * starting.
     */
    public void coldStart() throws Exception {
        if (callback != null)
            callback.coldStart();

        //  UnitTestWCCM.reset();
        //  UnitTestWCCM.store();   

        _createME(true, false);

        if (_isEventNotificationEnabled)
            me.setEventNotificationEnabled(true);
        _start(true, false);
    }

    /**
     * Sets up an ME, MP, Connection and JsJmsMessageFactory.
     * <p>
     * You should not normally call this method yourself - JUnit takes care of
     * starting.
     */
    public void start(boolean mpOnly) throws Exception {
        _createME(false, mpOnly);
        if (_isEventNotificationEnabled)
            me.setEventNotificationEnabled(true);
        _start(false, mpOnly);
    }

    /**
     * Sets up an ME, MP, Connection and JsJmsMessageFactory.
     * <p>
     * You should not normally call this method yourself - JUnit takes care of
     * starting.
     */
    public void startWithoutCreate(boolean mpOnly) throws Exception {
        _start(false, mpOnly);
    }

    /**
     * Sets up an ME, MP, Connection and JsJmsMessageFactory.
     * <p>
     * You should not normally call this method yourself - JUnit takes care of
     * starting.
     */
    public void createME(boolean mpOnly) throws Exception {
        _createME(false, mpOnly);
        if (_isEventNotificationEnabled)
            me.setEventNotificationEnabled(true);
    }

    /**
     * Sets up an ME, MP, Connection and JsJmsMessageFactory.
     * <p>
     * You should not normally call this method yourself - JUnit takes care of
     * starting.
     */
    public void warmStart(boolean mpOnly, int definedSendWindow) throws Exception {
        _createME(false, mpOnly);
        if (_isEventNotificationEnabled)
            me.setEventNotificationEnabled(true);

        UnitTestMEStarter.me.getMessageProcessor().setCustomProperty("sib.processor.protocolSendWindow", "" + definedSendWindow);

        _start(false, mpOnly);
    }

    /**
     * Sets up an ME, MP, Connection and JsJmsMessageFactory.
     * Takes a set of properties in a Hashtable and sets them as CustomProperties in the
     * Messaging Engine.
     * Name value pairs should be Strings inside the Hashtable.
     * <p>
     * You should not normally call this method yourself - JUnit takes care of
     * starting.
     */
    public void warmRestartCustomProperties(boolean mpOnly, Hashtable properties) throws Exception {
        stop(mpOnly, JsConstants.ME_STOP_QUIESCE, true);
        _createME(false, mpOnly);
        if (_isEventNotificationEnabled)
            me.setEventNotificationEnabled(true);
        Enumeration keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            UnitTestMEStarter.me.getMessageProcessor().setCustomProperty(key, (String) properties.get(key));
        }

        _start(false, mpOnly);
    }

    /**
     * Stop the ME.
     * <p>
     * You should not normally call this method yourself - JUnit takes care of
     * shutdown.
     */
    public void stop() throws Exception {
        stop(false, JsConstants.ME_STOP_QUIESCE, true);
    }

    /**
     * Warm start the ME.
     * <p>
     * You should not normally call this method yourself - JUnit takes care of
     * startup.
     */
    public void start() throws Exception {
        start(false);
    }

    /**
     * This method should not be called outside this class. It recreates the
     * ME.
     * 
     * @param coldStart If true, existing persisted data is discarded before
     *            start.
     * @param mpOnly If true, only the MP component is started.
     * 
     * @throws Exception
     */
    private void _createME(boolean coldStart, boolean mpOnly) throws Exception {
        // If we are doing a quick warm restart where only the Message Processor
        // itself is being restarted, then the subcomponents (i.e. Message Store)
        // will already be active.
        if (!mpOnly) {
            if (busName == null)
                busName = System.getProperty("js.test.busname", UnitTestConstants.BUS_NAME);

            String meName = System.getProperty("js.test.mename", UnitTestConstants.ME_NAME);

            me = (SIMPJsStandaloneEngineImpl) factory.createNewMessagingEngine(
                                                                               busName, meName, coldStart, reintTRM);
        }

        if (callback != null)
            callback.createME(this, coldStart, mpOnly);
    }

    boolean _isWasOpenForEBusinessRequiredAtStartup = true;

    public void setIsWasOpenForEBusinessRequiredAtStartup(boolean isAnnounceRequired) {
        _isWasOpenForEBusinessRequiredAtStartup = isAnnounceRequired;
    }

    private boolean IsWasOpenForEBusinessRequiredAtStartup() {
        return _isWasOpenForEBusinessRequiredAtStartup;
    }

    /**
     * This start method should not be called outside this class. It handles
     * the dirty work of restarting the ME.
     * 
     * @param coldStart If true, existing persisted data is discarded before
     *            start.
     * @param mpOnly If true, only the MP component is started.
     * 
     * @throws Exception
     */
    private void _start(boolean coldStart, boolean mpOnly) throws Exception {
        me.reset();

        UnitTestMEStarter.me.getMessageProcessor().initialize(me);
        UnitTestMEStarter.me.getMessageProcessor().start(JsConstants.ME_START_DEFAULT);
        if (IsWasOpenForEBusinessRequiredAtStartup()) {
            UnitTestMEStarter.me.getMessageProcessor().serverStarted();
        }

        trm = SIMPJsStandaloneFactory.getInstance().get_me().getTRM();

//   Start the WSRM component
        if (UnitTestMEStarter.me.getWSRMEngineComponent() != null) {
            UnitTestMEStarter.me.getWSRMEngineComponent().initialize(me);
            UnitTestMEStarter.me.getWSRMEngineComponent().start(JsConstants.ME_START_DEFAULT);
        }

        if (callback != null)
            callback.start(this, coldStart, mpOnly);
    }

    /**
     * This stop method should not be called outside this class. It handles
     * the dirty work of stopping the ME.
     * 
     * @param mpOnly If true, only the MP component is started.
     * 
     * @throws Exception
     */
    private void _stop(boolean mpOnly) throws Exception {
        //normal shudown mode
        stop(mpOnly, JsConstants.ME_STOP_QUIESCE, true);
    }

    /**
     * This stop method should not be called outside this class. It handles
     * the work of stopping the ME.
     * 
     * @param mpOnly If true, only the MP component is stopped.
     * @param shutdownMode
     * 
     * @throws Exception
     */
    public void stop(boolean mpOnly, int shutdownMode, boolean closeConnection)
                    throws Exception {
        if (callback != null)
            callback.stop(mpOnly, shutdownMode, closeConnection);

        if (!mpOnly) {
            if (UnitTestMEStarter.me.getWSRMEngineComponent() != null) {
                UnitTestMEStarter.me.getWSRMEngineComponent().stop(shutdownMode);
                UnitTestMEStarter.me.getWSRMEngineComponent().destroy();
            }
        }

        UnitTestMEStarter.me.getMessageProcessor().serverStopping();
        UnitTestMEStarter.me.getMessageProcessor().stop(shutdownMode);
        UnitTestMEStarter.me.getMessageProcessor().destroy();

        if (!mpOnly) {
            ((JsEngineComponent) UnitTestMEStarter.me.getMessageStore()).stop(shutdownMode);
            ((JsEngineComponent) UnitTestMEStarter.me.getMessageStore()).destroy();

            if (reintTRM) {
                trm.stop(shutdownMode);
                trm.destroy();
            }
        }
    }
}
