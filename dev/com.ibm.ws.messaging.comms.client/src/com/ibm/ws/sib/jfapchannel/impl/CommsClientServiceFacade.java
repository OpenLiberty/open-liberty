/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl;

import static com.ibm.ws.messaging.lifecycle.SingletonsReady.requireService;
import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.util.am.AlarmManager;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddressFactory;
import com.ibm.ws.messaging.lifecycle.Singleton;
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
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;

/**
 * A declarative services component can be completely POJO based
 * (no awareness/use of OSGi services).
 * 
 * OSGi methods (activate/deactivate) should be protected.
 * 
 * Start JFAP outbound chain and outbound Secure chain inline to Liberty profile Server start.
 * Can consider starting both chains lazily .. this would reduce Liberty profile server start time.
 */
@Component (
        service = {CommsClientServiceFacade.class, CommsClientServiceFacadeInterface.class, Singleton.class},
        configurationPolicy = IGNORE,
        immediate = true,
        property= {"type=messaging.comms.client.service","service.vendor=IBM"})
public class CommsClientServiceFacade implements CommsClientServiceFacadeInterface, Singleton {
    private static final TraceComponent tc = Tr.register(CommsClientServiceFacade.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

    private final CHFWBundle chfwBundle;
    private final ExecutorService executorService;
    private final AlarmManager alarmManager;
    private final ClientConnectionFactory clientConnectionFactoryInstance = new ClientConnectionFactoryImpl();

    @Activate
    public CommsClientServiceFacade(
            @Reference(name="chfwBundle")
            CHFWBundle chfwBundle,
            @Reference(name="executorService")
            ExecutorService executorService,
            @Reference(name="alarmManager")
            AlarmManager alarmManager) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "CommsClientServiceFacade",new Object[] {chfwBundle, executorService, alarmManager});

        this.chfwBundle = chfwBundle;
        this.executorService = executorService;
        this.alarmManager = alarmManager;

        chfwBundle.getFramework().registerFactory("JFapChannelOutbound", JFapChannelFactory.class);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "CommsClientServiceFacade");
    }

    public static SelectionCriteriaFactory getSelectionCriteriaFactory() {
        return SelectionCriteriaFactory.getInstance();
    }

    public AlarmManager getAlarmManager() {
        return alarmManager;
    }

    public ChannelFramework getChannelFramework() {
        return chfwBundle.getFramework();
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public WsByteBufferPoolManager getBufferPoolManager() {
        return chfwBundle.getBufferManager();
    }

    @Override
    public ClientConnectionFactory getClientConnectionFactory() {
        return clientConnectionFactoryInstance;
    }
}
