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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.management.StandardMBean;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.messaging.mbean.QueueMBean;
import com.ibm.websphere.messaging.mbean.SubscriberMBean;
import com.ibm.websphere.messaging.mbean.TopicMBean;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.Controllable;
import com.ibm.ws.sib.admin.ControllableRegistrationService;
import com.ibm.ws.sib.admin.ControllableType;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.RuntimeEventListener;
import com.ibm.ws.sib.admin.SIBExceptionInvalidValue;
import com.ibm.ws.sib.admin.exception.AlreadyRegisteredException;
import com.ibm.ws.sib.admin.exception.NotRegisteredException;
import com.ibm.ws.sib.admin.exception.ParentNotFoundException;
import com.ibm.ws.sib.utils.ras.SibTr;

public final class MessagingMBeanFactoryImpl implements ControllableRegistrationService {

    private static final String CLASS_NAME = "com.ibm.ws.sib.admin.internal.MessagingMBeanFactoryImpl";
    private static final TraceNLS nls = TraceNLS.getTraceNLS(JsConstants.MSG_BUNDLE);
    private static final TraceComponent tc = SibTr.register(MessagingMBeanFactoryImpl.class, JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);
    private final HashMap serviceObjects = new HashMap();
    // Debugging aid
    static {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "com/ibm/ws/sib/admin/internal/MessagingMBeanFactoryImpl.java");
    }

    private JsMessagingEngineImpl _me = null;

    private final Vector objects = new Vector();

    private final HashMap newObjects = new HashMap();

    private final HashMap controllableMap = new HashMap();

    BundleContext bcontext;

    public MessagingMBeanFactoryImpl(JsMessagingEngineImpl me) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "JsMessagingEngineImpl().<init>", this);
        SibTr.entry(tc, CLASS_NAME + "().<init>");
        _me = me;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, CLASS_NAME + "().<init>");
    }

    public MessagingMBeanFactoryImpl(JsMessagingEngineImpl jsMessagingEngineImpl,
                                     BundleContext bContext) {
        this(jsMessagingEngineImpl);
        this.bcontext = bContext;
    }

/*
 * (non-Javadoc)
 * 
 * @see com.ibm.ws.sib.admin.ControllableRegistrationService#register(com.ibm.ws.sib.admin.Controllable, com.ibm.ws.sib.admin.ControllableType)
 */
    @Override
    public RuntimeEventListener register(Controllable controllable, ControllableType type)
                    throws AlreadyRegisteredException, SIBExceptionInvalidValue {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "register", new Object[] { controllable, type });

        RuntimeEventListener retValue = null;
        if (type == ControllableType.QUEUE_POINT) {
            createQueueMBean(controllable);

        }
        else if (type == ControllableType.PUBLICATION_POINT) {
            createTopicMBean(controllable);
        }
        else if (type == ControllableType.SUBSCRIPTION_POINT) {
            createSubscriberMBean(controllable);
        }
        else
        {
            String reason = "Invalid ControllableType of " + type.toString();
            final SIBExceptionInvalidValue e =
                            new SIBExceptionInvalidValue(
                                            nls.getFormattedMessage("INTERNAL_ERROR_SIAS0003", new Object[] { reason }, reason));
            FFDCFilter.processException(e, CLASS_NAME + ".register", "PROBE_ID_10", this);
            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "register", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "register", new Object[] { retValue });
        return retValue;
    }

    @Override
    public RuntimeEventListener register(Controllable controllable, Controllable parent, ControllableType type)
                    throws AlreadyRegisteredException, SIBExceptionInvalidValue {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "register", new Object[] { controllable, parent, type });

        RuntimeEventListener retValue = null;
        if (false) {
            // Currently no heirarchical runtime objects
        }
        else {
            String reason = "Invalid ControllableType of " + type.toString();
            final SIBExceptionInvalidValue e =
                            new SIBExceptionInvalidValue(
                                            nls.getFormattedMessage("INTERNAL_ERROR_SIAS0003", new Object[] { reason }, reason));
            FFDCFilter.processException(e, CLASS_NAME + ".register", "PROBE_ID_20", this);
            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "register", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "register", new Object[] { retValue });
        return retValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.ControllableRegistrationService#register(com.ibm.ws.sib.admin.Controllable, com.ibm.ws.sib.admin.RuntimeEventListener,
     * com.ibm.ws.sib.admin.ControllableType)
     */
    public RuntimeEventListener register(Controllable controllable, RuntimeEventListener parent, ControllableType type)
                    throws AlreadyRegisteredException, SIBExceptionInvalidValue {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "register", new Object[] { controllable, parent, type });

        RuntimeEventListener retValue = null;
        if (false) {
            // Currently no heirarchical runtime objects
        }
        else {
            String reason = "Invalid ControllableType of " + type.toString();
            final SIBExceptionInvalidValue e =
                            new SIBExceptionInvalidValue(
                                            nls.getFormattedMessage("INTERNAL_ERROR_SIAS0003", new Object[] { reason }, reason));
            FFDCFilter.processException(e, CLASS_NAME + ".register", "PROBE_ID_30", this);
            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "register", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "register", new Object[] { retValue });
        return retValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.ControllableRegistrationService#deregister(com.ibm.ws.sib.admin.Controllable, com.ibm.ws.sib.admin.ControllableType)
     */
    @Override
    public void deregister(Controllable controllable, ControllableType type)
                    throws NotRegisteredException, SIBExceptionInvalidValue {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deregister", new Object[] { controllable, type });
        try {
            if (type == ControllableType.QUEUE_POINT) {
                ServiceRegistration<QueueMBean> qMBean = (ServiceRegistration<QueueMBean>) this.serviceObjects.get(controllable.getName());
                qMBean.unregister();
            } else if (type == ControllableType.PUBLICATION_POINT) {
                ServiceRegistration<TopicMBean> topicMBean = (ServiceRegistration<TopicMBean>) this.serviceObjects.get(controllable.getName());
                topicMBean.unregister();
            } else if (type == ControllableType.SUBSCRIPTION_POINT) {
                ServiceRegistration<SubscriberMBean> subMBean = (ServiceRegistration<SubscriberMBean>) this.serviceObjects.get(controllable.getName());
                subMBean.unregister();
            }
        } catch (Exception e) {
            SibTr.exception(tc, e);
        }

        deregister(controllable);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deregister");
        return;
    }

    /**
     * @param controllable
     * @throws NotRegisteredException
     */
    public void deregister(Controllable controllable) throws NotRegisteredException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deregister", new Object[] { controllable });

        Object previousValue = controllableMap.remove(controllable);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deregister Mbean", previousValue);
        return;
    }

    /**
   *
   */
    public synchronized void deregisterAll() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deregisterAll");

        Set s = controllableMap.entrySet();
//    controllableMap.values();
        Vector v = new Vector();
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            Map.Entry mapEntry = (Map.Entry) iter.next();
            Object o = mapEntry.getKey();
            if (o instanceof Controllable) {
                v.add(o);
            }
            else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    SibTr.debug(tc, "Object " + o.toString() + " which is not of type Controllable was found in controllableMap");
                }
            }
        }

        iter = v.iterator();
        while (iter.hasNext()) {
            try {
                Controllable c = (Controllable) iter.next();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    SibTr.debug(tc, "Controllable object " + c.toString() + " is being deregistered");
                }
                Object mbeanObject = controllableMap.get(c);
                if (mbeanObject instanceof JsQueue) {
                    deregister(c, ControllableType.QUEUE_POINT);
                } else if (mbeanObject instanceof TopicImpl) {
                    deregister(c, ControllableType.PUBLICATION_POINT);
                }
                else if (mbeanObject instanceof SubscriberImpl) {
                    deregister(c, ControllableType.SUBSCRIPTION_POINT);
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    SibTr.debug(tc, "Controllable object was deregistered");
                }
            } catch (NotRegisteredException e) {
                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    SibTr.debug(tc, "Controllable object could not be deregistered");
                }
            } catch (SIBExceptionInvalidValue e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    SibTr.exception(tc, e);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deregisterAll");
    }

    /**
     * Create an instance of the required MBean and register it
     * 
     * @param c
     * @return
     */
    private Controllable createQueueMBean(Controllable c) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createQueuePointMBean", new Object[] { c });
        JsQueue qp = new JsQueue(_me, c);
        controllableMap.put(c, qp);

        try {
            Dictionary<String, Object> properties = new Hashtable<String, Object>();
            properties.put("service.vendor", "IBM");
            String cName = c.getName();
            properties.put("jmx.objectname", "WebSphere:feature=wasJmsServer,type=Queue,name=" + cName);
            ServiceRegistration<QueueMBean> qMbean = (ServiceRegistration<QueueMBean>) bcontext.registerService(QueueMBean.class.getName(), qp, properties);
            serviceObjects.put(cName, qMbean);
        } catch (Exception e) {
            SibTr.exception(tc, e);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createQueuePointMBean", new Object[] { qp });
        return qp;

    }

    /**
     * Create an instance of the required MBean and register it
     * 
     * @param c
     */
    private Controllable createTopicMBean(Controllable c) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createPublicationPointMBean", new Object[] { c });
        TopicImpl pp = new TopicImpl(_me, c);
        controllableMap.put(c, pp);
        try {
            Dictionary<String, Object> properties = new Hashtable<String, Object>();
            properties.put("service.vendor", "IBM");
            String cName = c.getName();
            properties.put("jmx.objectname", "WebSphere:feature=wasJmsServer,type=Topic,name=" + cName);
            ServiceRegistration<TopicMBean> topicMBean = (ServiceRegistration<TopicMBean>) bcontext.registerService(TopicMBean.class.getName(), pp, properties);
            serviceObjects.put(cName, topicMBean);
        } catch (Exception e) {
            SibTr.exception(tc, e);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createPublicationPointMBean", new Object[] { pp });
        return pp;

    }

    private Controllable createSubscriberMBean(Controllable c) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createSubscriptionPointMBean", new Object[] { c });
        SubscriberImpl sp = new SubscriberImpl(_me, c);
        controllableMap.put(c, sp);
        try {
            Dictionary<String, Object> properties = new Hashtable<String, Object>();
            properties.put("service.vendor", "IBM");
            String cName = c.getName();
            properties.put("jmx.objectname", "WebSphere:feature=wasJmsServer,type=Subscriber,name=" + cName);
            ServiceRegistration<SubscriberMBean> subscriberMbean = (ServiceRegistration<SubscriberMBean>) bcontext.registerService(SubscriberMBean.class.getName(), sp, properties);
            serviceObjects.put(cName, subscriberMbean);
        } catch (Exception e) {
            SibTr.exception(tc, e);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createSubscriptionPointMBean", new Object[] { sp });
        return sp;

    }

    public void deregister(Object o) throws NotRegisteredException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deregister", new Object[] { o });

        Object previousValue = newObjects.remove(o);
        if (previousValue != null) {
            if (previousValue instanceof JsObject) {
//        ((JsObject) previousValue).deactivateMBean();
            }
        }
        else {
            throw new NotRegisteredException("");
        }

        int i = 0;
        Enumeration vEnum = objects.elements();
        while (vEnum.hasMoreElements()) {
            Object el = vEnum.nextElement();
            if (el.equals(o)) {
                objects.remove(i);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "deregister", "removed object");
                return;
            }
            i++;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deregister", "did not find object");

        throw new NotRegisteredException("");
    }

    @Override
    public RuntimeEventListener register(Controllable controllable, StandardMBean parent,
                                         ControllableType type) throws AlreadyRegisteredException,
                    ParentNotFoundException, SIBExceptionInvalidValue {
        return null;
    }

}
