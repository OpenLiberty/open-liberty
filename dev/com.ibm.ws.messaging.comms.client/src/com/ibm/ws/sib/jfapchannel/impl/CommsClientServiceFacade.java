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

import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ejs.util.am.AlarmManager;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
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
public class CommsClientServiceFacade implements CommsClientServiceFacadeInterface {
    private static final TraceComponent tc = Tr.register(CommsClientServiceFacade.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

    private static final AtomicServiceReference<CHFWBundle> chfwRef = new AtomicServiceReference<CHFWBundle>("chfwBundle");
    private static final AtomicServiceReference<ExecutorService> exeServiceRef = new AtomicServiceReference<ExecutorService>("executorService");
    private static final AtomicServiceReference<CommonServiceFacade> _commonServiceFacadeRef = new AtomicServiceReference<CommonServiceFacade>("commonServiceFacade");
    private static final AtomicServiceReference<AlarmManager> alarmManagerRef = new AtomicServiceReference<AlarmManager>("alarmManager");

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
    public void activate(Map<String, Object> properties, ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "activate");
        chfwRef.activate(context);
        _commonServiceFacadeRef.activate(context);
        alarmManagerRef.activate(context);
        exeServiceRef.activate(context);

        getChannelFramewrok().registerFactory("JFapChannelOutbound", JFapChannelFactory.class);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "activate");
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param reason int representation of reason the component is stopping
     */
    protected void deactivate(ComponentContext context) {
        chfwRef.deactivate(context);
        _commonServiceFacadeRef.deactivate(context);
        alarmManagerRef.deactivate(context);
        exeServiceRef.deactivate(context);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "CommsClientServiceFacade deactivated");
    }

    /**
     * Declarative Services method for setting the ConfigurationAdmin service
     * reference.
     * 
     * @param ref
     *            reference to the service
     */
    protected void setChfwBundle(ServiceReference<CHFWBundle> ref) {
        chfwRef.setReference(ref);
    }

    protected void unsetChfwBundle(ServiceReference<CHFWBundle> ref) {
        chfwRef.unsetReference(ref);
    }

    protected void setExecutorService(ServiceReference<ExecutorService> ref) {
        exeServiceRef.setReference(ref);
    }

    protected void unsetExecutorService(ServiceReference<ExecutorService> ref) {
        exeServiceRef.unsetReference(ref);
    }

    protected void setCommonServiceFacade(ServiceReference<CommonServiceFacade> ref) {
        _commonServiceFacadeRef.setReference(ref);
    }

    protected void unsetCommonServiceFacade(ServiceReference<CommonServiceFacade> ref) {
        _commonServiceFacadeRef.unsetReference(ref);
    }

    //obtain TrmMessageFactory from MFP implementation ( via common bundle) 
    public static TrmMessageFactory getTrmMessageFactory() {
        return _commonServiceFacadeRef.getService().getTrmMessageFactory();
    }

    //obtain getJsDestinationAddressFactory from MFP implementation ( via common bundle)
    public static JsDestinationAddressFactory getJsDestinationAddressFactory() {
        return _commonServiceFacadeRef.getService().getJsDestinationAddressFactory();
    }

    //obtain getJsDestinationAddressFactory from core implementation ( via common bundle)
    public static SelectionCriteriaFactory getSelectionCriteriaFactory() {
        return _commonServiceFacadeRef.getService().getSelectionCriteriaFactory();
    }

    protected void setAlarmManager(ServiceReference<AlarmManager> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "setAlarmManager", ref);

        alarmManagerRef.setReference(ref);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "setAlarmManager");
    }

    protected void unsetAlarmManager(ServiceReference<AlarmManager> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "unsetAlarmManager", ref);

        alarmManagerRef.unsetReference(ref);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "unsetAlarmManager");
    }

    public static AlarmManager getAlarmManager() {
        return alarmManagerRef.getService();
    }

    public static ChannelFramework getChannelFramewrok() {
        if (null == chfwRef.getService()) {
            return ChannelFrameworkFactory.getChannelFramework();
        }
        return chfwRef.getService().getFramework();
    }

    public static ExecutorService getExecutorService() {
        return exeServiceRef.getService();
    }

    /**
     * Access the current reference to the bytebuffer pool manager from channel frame work.
     * 
     * @return WsByteBufferPoolManager
     */
    public static WsByteBufferPoolManager getBufferPoolManager() {
        if (null == chfwRef.getService()) {
            return ChannelFrameworkFactory.getBufferManager();
        }
        return chfwRef.getService().getBufferManager();
    }

    //Export implementations of this bundles through CommsClientServiceFacadeInterface

    volatile private ClientConnectionFactory _ClientConnectionFactoryInstance = null;

    @Override
    public ClientConnectionFactory getClientConnectionFactory() {
        if (_ClientConnectionFactoryInstance == null) {
            synchronized (this) {
                _ClientConnectionFactoryInstance = new ClientConnectionFactoryImpl();
            }
        }
        return _ClientConnectionFactoryInstance;
    }

}
