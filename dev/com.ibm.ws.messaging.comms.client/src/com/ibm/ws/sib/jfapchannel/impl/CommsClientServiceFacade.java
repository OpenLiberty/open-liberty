/*******************************************************************************
 * Copyright (c) 2011, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.concurrent.ExecutorService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.util.am.AlarmManager;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.messaging.lifecycle.Singleton;
import com.ibm.ws.messaging.lifecycle.SingletonsReady;
import com.ibm.ws.sib.common.service.CommonServiceFacade;
import com.ibm.ws.sib.comms.ClientConnectionFactory;
import com.ibm.ws.sib.comms.CommsClientServiceFacadeInterface;
import com.ibm.ws.sib.comms.client.ClientConnectionFactoryImpl;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.richclient.impl.JFapChannelFactory;
import com.ibm.ws.sib.mfp.JsDestinationAddressFactory;
import com.ibm.ws.sib.mfp.trm.TrmMessageFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;

/**
 * Start JFAP out-bound chain and out-bound Secure chain inline to Liberty profile Server start.
 * Can consider starting both chains lazily .. this would reduce Liberty profile server start time.
 */
@Component(name = "com.ibm.ws.messaging.comms.client", configurationPolicy = IGNORE, immediate = true, property = "service.vendor=IBM")
public class CommsClientServiceFacade implements CommsClientServiceFacadeInterface, Singleton {
    private static final TraceComponent tc = Tr.register(CommsClientServiceFacade.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);
  
    private final ExecutorService executorService;   
    private final AlarmManager alarmManager;
    private final ClientConnectionFactory clientConnectionFactory = new ClientConnectionFactoryImpl();
    
    @Activate
    public CommsClientServiceFacade(@Reference ExecutorService executorService, 
                                    @Reference CommonServiceFacade commonServiceFacade, // Required but not used directly.
                                    @Reference AlarmManager alarmManager) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "<init>", new Object[] {executorService, commonServiceFacade, alarmManager});
        
        this.executorService = executorService;
        this.alarmManager = alarmManager;
        
        getChannelFramewrok().registerFactory("JFapChannelOutbound", JFapChannelFactory.class);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "<init>");
    }

    public static TrmMessageFactory getTrmMessageFactory() {
        // Fail-fast if called out of sequence.
        SingletonsReady.requireService(CommsClientServiceFacade.class);
        return CommonServiceFacade.getTrmMessageFactory();
    }

    public static JsDestinationAddressFactory getJsDestinationAddressFactory() {
        // Fail-fast if called out of sequence.
        SingletonsReady.requireService(CommsClientServiceFacade.class);
        return CommonServiceFacade.getJsDestinationAddressFactory();
    }

    public static SelectionCriteriaFactory getSelectionCriteriaFactory() {
        // Fail-fast if called out of sequence.
        SingletonsReady.requireService(CommsClientServiceFacade.class);
        return CommonServiceFacade.getSelectionCriteriaFactory();
    }

    public static AlarmManager getAlarmManager() {
        // Fail-fast if called out of sequence.
        return SingletonsReady.requireService(CommsClientServiceFacade.class).alarmManager;
    }

    public static ChannelFramework getChannelFramewrok() {      
        return ChannelFrameworkFactory.getChannelFramework();
    }

    public static ExecutorService getExecutorService() {
        // Fail-fast if called out of sequence.
        return SingletonsReady.requireService(CommsClientServiceFacade.class).executorService;
    }

    /**
     * Access the current reference to the bytebuffer pool manager from channel frame work.
     */
    public static WsByteBufferPoolManager getBufferPoolManager() {  
        return ChannelFrameworkFactory.getBufferManager();
    }

    @Override
    public ClientConnectionFactory getClientConnectionFactory() {     
        return clientConnectionFactory;
    }
}
