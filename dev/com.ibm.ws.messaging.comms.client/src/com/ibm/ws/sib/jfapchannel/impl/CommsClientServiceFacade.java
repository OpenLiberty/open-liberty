/*******************************************************************************
 * Copyright (c) 2011, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.messaging.lifecycle.Singleton;
import com.ibm.ws.sib.comms.ClientConnectionFactory;
import com.ibm.ws.sib.comms.CommsClientServiceFacadeInterface;
import com.ibm.ws.sib.comms.client.ClientConnectionFactoryImpl;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.richclient.impl.JFapChannelFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;

@Component (
        service = {CommsClientServiceFacade.class, CommsClientServiceFacadeInterface.class, Singleton.class},
        configurationPolicy = IGNORE,
        immediate = true,
        property= {"type=messaging.comms.client.service"})
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
