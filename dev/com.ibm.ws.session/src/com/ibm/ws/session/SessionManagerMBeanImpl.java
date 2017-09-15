/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import javax.management.AttributeChangeNotification;
import javax.management.DynamicMBean;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationBroadcasterSupport;
import javax.management.StandardEmitterMBean;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.ws.webcontainer.httpsession.SessionMgrComponentImpl;

/**
 * @see SessionManagerMBean
 */
@Component(service = { SessionManagerMBean.class, DynamicMBean.class },
           configurationPid = "com.ibm.ws.session",
           configurationPolicy = ConfigurationPolicy.OPTIONAL,
           property = { "service.vendor=IBM",
                       "jmx.objectname=" + SessionManagerMBean.OBJECT_NAME })
public class SessionManagerMBeanImpl extends StandardEmitterMBean implements SessionManagerMBean {
    private static final String methodClassName = "SessionManagerMBeanImpl";

    private Map<String, Object> sessionManagerProps = null;
    private Map<String, Object> allowedProps = new HashMap<String, Object>();    

    /** Configuration constants */
    static final String CFG_COOKIE_NAME = "cookieName";
    static final String CFG_CLONE_SEPARATOR = "cloneSeparator";
    static final String CFG_CLONE_ID = "cloneId";    

    /** List of known, expected keys */
    private static final List<String> EXPECTED_KEYS = new ArrayList<String>(
                    Arrays.asList(CFG_COOKIE_NAME, CFG_CLONE_SEPARATOR, CFG_CLONE_ID));

    /** Attribute names */
    static final String ATTRIBUTE_NAME_COOKIE = "CookieName";
    static final String ATTRIBUTE_NAME_CLONE_SEPARATOR = "CloneSeparator";
    static final String ATTRIBUTE_NAME_CLONE_ID = "CloneID";

    private final AtomicLong sequenceNum = new AtomicLong();
    boolean sessionManagerConfigFound = false;
   

    public SessionManagerMBeanImpl() throws NotCompliantMBeanException {
        super(SessionManagerMBean.class, new NotificationBroadcasterSupport((Executor) null,
                        new MBeanNotificationInfo(new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE },
                                        AttributeChangeNotification.class.getName(),
                                        "")));
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        //make sure we have defaults available
        SessionManagerConfig sessionManagerConfig = SessionMgrComponentImpl.getServerSessionManagerConfig();
        if( sessionManagerConfig != null ) {
            allowedProps.put(CFG_COOKIE_NAME, sessionManagerConfig.getSessionCookieName());
            allowedProps.put(CFG_CLONE_SEPARATOR, Character.toString(SessionManagerConfig.getCloneSeparator()));
            allowedProps.put(CFG_CLONE_ID, SessionManagerConfig.getCloneId());
            sessionManagerConfigFound = true;
        }
        else {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, "activate", "No session manager config found, so no session cookie names are available.");
            }
        }
        // refresh with config
        sessionManagerProps = filterUnexpectedKeys(props);        
        Set<Entry<String, Object>> entries = sessionManagerProps.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String key = entry.getKey();
            if (EXPECTED_KEYS.contains(key)) {
                allowedProps.put(key, entry.getValue());
            }
        }
        modified(props);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc, Map<String, Object> props) {
    }

    @Modified
    protected void modified(Map<String, Object> props) {
        // If session manager config property is not found during activate, try again...
        if( !sessionManagerConfigFound ) {
            SessionManagerConfig sessionManagerConfig = SessionMgrComponentImpl.getServerSessionManagerConfig();
            if( sessionManagerConfig != null ) {
                allowedProps.put(CFG_COOKIE_NAME, sessionManagerConfig.getSessionCookieName());
                allowedProps.put(CFG_CLONE_SEPARATOR, Character.toString(SessionManagerConfig.getCloneSeparator()));
                allowedProps.put(CFG_CLONE_ID, SessionManagerConfig.getCloneId());
                sessionManagerConfigFound = true;            
            }
            else {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, "activate", "No session manager config found, so no session cookie names are available.");
                }
            }            
        }
        String oldCookieName = null;
        String oldSeparatorName = null;
        String oldCloneID = null;
        if (sessionManagerProps != null) {
            oldCookieName = (String) allowedProps.get(CFG_COOKIE_NAME);
            oldSeparatorName = (String) allowedProps.get(CFG_CLONE_SEPARATOR);
            oldCloneID = (String) allowedProps.get(CFG_CLONE_ID);
        }
        sessionManagerProps = filterUnexpectedKeys(props); //get modified version and update allowed properties with latest
        Set<Entry<String, Object>> entries = sessionManagerProps.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String key = entry.getKey();
            if (EXPECTED_KEYS.contains(key)) {
                allowedProps.put(key, entry.getValue());
            }
        }
        String newCookieName = (String) sessionManagerProps.get(CFG_COOKIE_NAME);
        String newSeparatorName = (String) sessionManagerProps.get(CFG_CLONE_SEPARATOR);
        String newCloneID = (String) sessionManagerProps.get(CFG_CLONE_ID);
        // should we ensure cloneID is never null - but getCloneId() always returns old cloneID when deleted from config?
        if (newCloneID == null){
            newCloneID = SessionManagerConfig.getCloneId();
            allowedProps.put(CFG_CLONE_ID, newCloneID);
        }
        publishToRepository(ATTRIBUTE_NAME_COOKIE, "java.lang.String", oldCookieName, newCookieName);            
        publishToRepository(ATTRIBUTE_NAME_CLONE_SEPARATOR, "java.lang.String", oldSeparatorName, newSeparatorName);
        publishToRepository(ATTRIBUTE_NAME_CLONE_ID, "java.lang.String", oldCloneID, newCloneID);        
    }

    /**
     * Reduce the map to the set of expected keys
     */
    private Map<String, Object> filterUnexpectedKeys(Map<String, Object> inputProps) {
        Map<String, Object> outputProps = new HashMap<String, Object>();

        Set<Entry<String, Object>> entries = inputProps.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String key = entry.getKey();
            if (EXPECTED_KEYS.contains(key)) {
                outputProps.put(key, entry.getValue());
            }
        }
        return outputProps;
    }

    /**
     * Send attribute change notification
     */
    private void publishToRepository(String attributeName, String attributeType, Object oldValue, Object newValue) {
        super.sendNotification(new AttributeChangeNotification(
                        this,
                        sequenceNum.incrementAndGet(),
                        System.currentTimeMillis(),
                        "",
                        attributeName,
                        attributeType,
                        oldValue,
                        newValue));
    }    

    /* (non-Javadoc)
     * @see com.ibm.websphere.servlet.session.SessionManagerMBean#getCloneSeparator()
     */
    @Override
    public String getCloneSeparator() {
        if (allowedProps != null) {
            return (String) allowedProps.get(CFG_CLONE_SEPARATOR);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.ibm.websphere.servlet.session.SessionManagerMBean#getCookieName()
     */
    @Override
    public String getCookieName() {
        if (allowedProps != null) {
            return (String) allowedProps.get(CFG_COOKIE_NAME);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.ibm.websphere.servlet.session.SessionManagerMBean#getCloneID()
     */
    @Override
    public String getCloneID() {
        if (allowedProps != null) {
            return (String) allowedProps.get(CFG_CLONE_ID);
        }
        return null;
    }

}
