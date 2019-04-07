/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit.source;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.event.Topic;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.security.audit.InvalidConfigurationException;
import com.ibm.ws.collector.manager.buffer.BufferManagerImpl;
import com.ibm.ws.logging.collector.LogFieldConstants;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.utils.SequenceNumber;
import com.ibm.ws.security.audit.event.AuditMgmtEvent;
import com.ibm.ws.security.audit.utils.AuditUtils;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.Source;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.audit.AuditService;

/**
 * This class is the security audit service. It handles audit config and is also
 * a collector manager Source for audit events.
 */
@Component(service = { AuditService.class, Source.class }, configurationPid = "com.ibm.ws.security.audit.event", configurationPolicy = ConfigurationPolicy.OPTIONAL,
           property = "service.vendor=IBM", immediate = true)

public class AuditServiceImpl implements AuditService, Source {

    private static final String VAR_DEFAULTHOSTNAME = "${defaultHostName}";
    private static final String VAR_WLPSERVERNAME = "${wlp.server.name}";
    private static final String VAR_WLPUSERDIR = "${wlp.user.dir}";
    private static final String ENV_VAR_CONTAINERHOST = "${env.CONTAINER_HOST}";
    private static final String ENV_VAR_CONTAINERNAME = "${env.CONTAINER_NAME}";
    private static final String AUDIT_SERVER_ID_PREFIX = "websphere: ";
    private static final String AUDIT_SERVER_ID_SEPARATOR = ":";

    /** Event topic used for queued work */
    public static final Topic TOPIC_QUEUED_WORK = new Topic("com/ibm/ws/jmx/QUEUED_AUDIT_WORK");
    public static final String TOPIC_QUEUED_WORK_NAME = TOPIC_QUEUED_WORK.getName();
    /** Event property key for finding the actual runnable task */

    private int eventSequenceNumber = 0;
    private static Object syncObject = new Object();
    private static Object syncSeqNum = new Object();

    private final SequenceNumber sequenceNumber = new SequenceNumber();

    private static final String INCORRECT_AUDIT_EVENT_CONFIGURATION = "INCORRECT_AUDIT_EVENT_CONFIGURATION";
    private static final String INCORRECT_AUDIT_OUTCOME_CONFIGURATION = "INCORRECT_AUDIT_OUTCOME_CONFIGURATION";

    /** Event topic used for queued work */
    // cl-ol public static final Topic TOPIC_QUEUED_WORK = new Topic("com/ibm/ws/jmx/QUEUED_AUDIT_WORK");
    // cl-ol public static final String TOPIC_QUEUED_WORK_NAME = TOPIC_QUEUED_WORK.getName();
    /** Event property key for finding the actual runnable task */
    public static final String KEY_RUNNABLE = "JMXWork";

    private static final String VARIABLE_REGISTRY_SERVICE = "variableRegistryService";
    private static final AtomicServiceReference<VariableRegistry> variableRegistryServiceRef = new AtomicServiceReference<VariableRegistry>(VARIABLE_REGISTRY_SERVICE);

    @Reference(name = VARIABLE_REGISTRY_SERVICE,
               service = VariableRegistry.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setVariableRegistryService(ServiceReference<VariableRegistry> variableRegistry) {
        variableRegistryServiceRef.setReference(variableRegistry);
    }

    protected void unsetVariableRegistryService(ServiceReference<VariableRegistry> variableRegistry) {
        variableRegistryServiceRef.unsetReference(variableRegistry);
    }

    private static final TraceComponent tc = Tr.register(AuditServiceImpl.class);

    // BufferManager for collector framework Source
    private BufferManager bufferMgr;
    private BufferManager saved_bufferMgr;

    private final ConcurrentHashMap<String, List<Map<String, Object>>> handlerEventsMap = new ConcurrentHashMap<String, List<Map<String, Object>>>();

    private String eventName = null;
    private boolean isCustomEvent = false;
    private String[] auditData = null;
    private String[] outcome = null;
    private Map<String, Object> thisConfiguration = null;
    private AuditEvent[] savedEvent = new AuditEvent[10];
    private int savedEventIndex = 0;
    private final boolean savedEventEmitted = false;
    private String serverID = null;
    private volatile BufferManagerImpl auditLogConduit;
    private boolean auditServiceStarted = false;
    private boolean emitted1 = false;
    private final boolean emitted2 = false;

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> configuration) {
        Tr.info(tc, "AUDIT_SERVICE_STARTING");
        thisConfiguration = configuration;
        variableRegistryServiceRef.activate(cc);
        if (configuration != null && !configuration.isEmpty()) {
            for (Map.Entry<String, Object> entry : configuration.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (key.equals(AuditConstants.EVENT_NAME)) {
                    setEventName(value);
                } else if (key.equals(AuditConstants.CUSTOM)) {
                    setIsCustomEvent(value);
                } else if (key.equals(AuditConstants.AUDIT_DATA)) {
                    setAuditData(value);
                } else if (key.equals(AuditConstants.OUTCOME)) {
                    setOutcome(value);
                }
            }
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "configuration: " + configuration.toString());
        }

    }

    @Override
    public void auditStarted(String serviceName) {
        if (isAuditRequired(AuditConstants.SECURITY_AUDIT_MGMT,
                            AuditConstants.SUCCESS)) {
            AuditMgmtEvent av = new AuditMgmtEvent(thisConfiguration, "AuditService", "start");
            sendEvent(av);
        }
        auditServiceStarted = true;

        saved_bufferMgr = bufferMgr;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "auditStarted, bufferMgr = " + bufferMgr + " saved_bufferMgr = " + saved_bufferMgr);
        }
        auditServiceStarted = true;
        Tr.info(tc, "AUDIT_SERVICE_READY");
    }

    @Override
    public void auditStopped(String serviceName) {

        if (isAuditRequired(AuditConstants.SECURITY_AUDIT_MGMT, AuditConstants.SUCCESS)) {
            AuditMgmtEvent av = new AuditMgmtEvent(thisConfiguration, "AuditService", "stop");
            sendEvent(av);
            emitted1 = false;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "auditStopped, emitted1 = false, saved_bufferMgr = null");
            }
            saved_bufferMgr = null;
        }

    }

    @Deactivate
    protected void deactivate(ComponentContext cc, int reason) {
        Tr.info(tc, "AUDIT_SERVICE_STOPPED");
        handlerEventsMap.clear();
        variableRegistryServiceRef.deactivate(cc);

    }

    @Modified
    protected void modified(Map<String, Object> configuration) {}

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.security.audit.AuditService#registerEvents()
     */
    @Override
    public void registerEvents(String handlerName, List<Map<String, Object>> configuredEvents) throws InvalidConfigurationException {

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "size of configuredEvents: " + configuredEvents.size());
        }
        if (validateEventsAndOutcomes(handlerName, configuredEvents)) {
            if (!handlerEventsMap.containsKey(handlerName)) {
                handlerEventsMap.put(handlerName, configuredEvents);
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "handlerEventsMap: " + handlerEventsMap.toString());
            }

            auditStarted("AuditService");

            if (handlerEventsMap.isEmpty() || handlerEventsMap.containsKey(AUDIT_FILE_HANDLER_NAME)) {
                AuditMgmtEvent av = new AuditMgmtEvent(thisConfiguration, "AuditHandler:" + AUDIT_FILE_HANDLER_NAME, "start");
                sendEvent(av);
            }

        } else {
            throw new InvalidConfigurationException();
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.security.audit.AuditService#validateEventsAndOutcomes()
     */
    @Override
    public boolean validateEventsAndOutcomes(String handlerName, List<Map<String, Object>> configuredEvents) {

        if (configuredEvents.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "configuredEvents is empty, returning true as all events are valid");
            }
            return true;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "configuredEvents: " + configuredEvents.toString());
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "size of configuredEvents: " + configuredEvents.size());
        }

        for (Map<String, Object> events : configuredEvents) {

            if (events.isEmpty()) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "no events or outcomes specified, all events and outcomes are honoured");
                }
                return true;
            } else {
                boolean isFoundEventName = false;
                String foundEventName = null;
                boolean isFoundOutcome = false;
                String foundOutcome = null;
                for (Entry<String, Object> entry : events.entrySet()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "entry: " + entry.toString());
                    }

                    // if the event is a custom one, we cannot validate, so return true:  buyer beware
                    if (entry.getKey().equals(AuditConstants.CUSTOM) && entry.getValue().equals(Boolean.TRUE)) {
                        return true;
                    } else {
                        if (entry.getKey().equals(AuditConstants.EVENT_NAME)) {
                            isFoundEventName = true;
                            foundEventName = (String) entry.getValue();
                            if (!validateEventName((String) entry.getValue())) {
                                String eventsList = "";
                                for (String temp : AuditConstants.validEventNamesList) {
                                    eventsList = eventsList.concat(temp).concat(" ");
                                }
                                Tr.error(tc, INCORRECT_AUDIT_EVENT_CONFIGURATION, new Object[] { foundEventName, handlerName, eventsList });
                                return false;
                            }
                        } else if (entry.getKey().equals(AuditConstants.OUTCOME)) {
                            isFoundOutcome = true;
                            foundOutcome = (String) entry.getValue();
                            if (!validateOutcomeName((String) entry.getValue())) {
                                String outcomesList = "";
                                for (String temp : AuditConstants.validOutcomesList) {
                                    outcomesList = outcomesList.concat(temp).concat(" ");
                                }
                                Tr.error(tc, INCORRECT_AUDIT_OUTCOME_CONFIGURATION, new Object[] { foundOutcome, handlerName, outcomesList });
                                return false;
                            }
                        }
                    }
                }
                if (!isFoundEventName && isFoundOutcome) {
                    String eventsList = "";
                    for (String temp : AuditConstants.validEventNamesList) {
                        eventsList = eventsList.concat(temp).concat(" ");
                    }
                    Tr.error(tc, "INCORRECT_AUDIT_CONFIGURATION_OUTCOME_SPECIFIED_MISSING_EVENTNAME",
                             new Object[] { foundOutcome, handlerName, eventsList });
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * (non Java-doc)
     *
     * Validates the supplied non-custom event name is a valid event name. Return true if valid; false otherwise
     */
    public boolean validateEventName(String eventName) {
        boolean isValid = false;
        if ((AuditConstants.validEventNamesList).contains(eventName))
            isValid = true;
        return isValid;
    }

    /*
     * (non Java-doc)
     *
     * Validates the input as a valid outcome. Return true if valid; false otherwise
     */
    public boolean validateOutcomeName(String outcome) {
        boolean isValid = false;
        if ((AuditConstants.validOutcomesList).contains(outcome))
            isValid = true;

        return isValid;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.security.audit.AuditService#registerEvents()
     */
    @Override
    public void unRegisterEvents(String handlerName) {

        if (handlerEventsMap.containsKey(handlerName)) {

            if (handlerEventsMap.size() == 1) {
                auditStopped(null);
            }

            handlerEventsMap.remove(handlerName);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "handlerEventsMap: " + handlerEventsMap.toString());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.security.audit.AuditService#isAuditRequired()
     */
    @Override
    public boolean isAuditRequired(String eventType, String outcome) {
        // TODO: need to handle auditData

        for (Map.Entry<String, List<Map<String, Object>>> handlerMap : handlerEventsMap.entrySet()) {

            List<Map<String, Object>> handlerEventsList = handlerMap.getValue();
            if (handlerEventsList.isEmpty()) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "handlerEventsList is empty, returning true for isAuditRequired");
                }
                return true;
            }
            for (Map<String, Object> handlerEvents : handlerEventsList) {
                if (handlerEvents.isEmpty()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "no events or outcomes specified, all events and outcomes are honoured");
                    }
                    return true;
                } else {
                    boolean foundMatchingEvent = false;
                    boolean foundAnOutcome = false;
                    boolean foundMatchingEventAndOutcome = false;
                    for (Entry<String, Object> entry : handlerEvents.entrySet()) {
                        if (entry.getKey().equals(AuditConstants.EVENT_NAME) && entry.getValue().equals(eventType)) {
                            foundMatchingEvent = true;
                        } else {
                            if (entry.getKey().equals(AuditConstants.OUTCOME)) {
                                foundAnOutcome = true;
                                if (entry.getValue().toString().equalsIgnoreCase(outcome)) {
                                    if (foundMatchingEvent) {
                                        foundMatchingEventAndOutcome = true;
                                    }
                                }
                            }
                        }

                    }
                    if (foundMatchingEventAndOutcome || (foundMatchingEvent && !foundAnOutcome)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Given a Map, add the corresponding audit data to the given GenericData object.
     *
     * @param gdo - GenericData object
     * @param map - Java Map object
     */
    private GenericData map2GenericData(GenericData gdo, Map<String, Object> map) {
        for (Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                if (entry.getKey().equals(AuditEvent.TARGET_APPNAME)) {
                    gdo.addPair(entry.getKey(), AuditUtils.getJ2EEComponentName());
                }
            } else {
                gdo.addPair(entry.getKey(), entry.getValue().toString());
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "gdo: " + gdo.toString());
        }
        return gdo;
    }

    /** {@inheritDoc} */
    @Override
    public void sendEvent(AuditEvent event) {

        if (event == null) {
            emitSavedEvents();
        } else {
            String en = (String) event.getMap().get(AuditEvent.EVENTNAME);
            String eo = (String) event.getMap().get(AuditEvent.OUTCOME);
            if (isAuditRequired(en, eo)) {

                synchronized (syncSeqNum) {
                    if (event.getTarget().get(AuditEvent.TARGET_ID) == "null" || event.getTarget().get(AuditEvent.TARGET_ID) == null) {
                        event.set(AuditEvent.TARGET_ID, getServerID());
                    }
                    if (event.getObserver().get(AuditEvent.OBSERVER_ID) == "null" || event.getObserver().get(AuditEvent.OBSERVER_ID) == null) {
                        event.set(AuditEvent.OBSERVER_ID, getServerID());
                    }

                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "sendEvent, bufferMgr = " + bufferMgr + " saved_bufferMgr = " + saved_bufferMgr);
                    }

                    if (bufferMgr != null && saved_bufferMgr != null) {
                        event.set(AuditConstants.EVENT_SEQUENCE_NUMBER, eventSequenceNumber++);

                        GenericData gdo = new GenericData();
                        gdo.setSourceName("com.ibm.ws.audit.source.auditsource");
                        long dateVal = System.currentTimeMillis();
                        gdo.addPair(LogFieldConstants.IBM_DATETIME, dateVal);
                        gdo.addPair(LogFieldConstants.IBM_SEQUENCE, sequenceNumber.next(dateVal));
                        gdo.addPair(LogFieldConstants.IBM_THREADID, new Integer((int) Thread.currentThread().getId()));
                        gdo = map2GenericData(gdo, event.getMap());

                        final GenericData f_gdo = gdo;
                        AccessController.doPrivileged(new PrivilegedAction<Void>() {
                            @Override
                            public Void run() {
                                bufferMgr.add(f_gdo);
                                return null;
                            }
                        });

                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "gdo: " + gdo.toString());
                        }

                    } else {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "sendEvent, savedEventIndex = " + savedEventIndex + " saved event: " + event.toString());
                        }
                        savedEvent[savedEventIndex++] = event;
                    }
                }

            }

        }

    }

    //@Override
    public void emitSavedEvents() {
        if (tc.isDebugEnabled()) {
            if (bufferMgr == null)
                Tr.debug(tc, "emitSavedEvents, bufferMgr is null");
        }
        if (bufferMgr != null) {
            if (!savedEventEmitted) {
                if (savedEvent != null && savedEvent.length > 0) {
                    for (int i = 0; i < savedEventIndex; i++) {
                        sendEvent(savedEvent[i]);
                    }
                    //savedEventEmitted = true;
                    savedEvent = new AuditEvent[10];
                    savedEventIndex = 0;
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getSourceName() {
        return AuditService.AUDIT_SOURCE_NAME;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocation() {
        return AuditService.AUDIT_SOURCE_LOCATION;
    }

    /** {@inheritDoc} */
    @Override
    public void setBufferManager(BufferManager bufferMgr) {
        this.bufferMgr = bufferMgr;
        saved_bufferMgr = bufferMgr;
    }

    /** {@inheritDoc} */
    @Override
    public void unsetBufferManager(BufferManager bufferMgr) {
        this.bufferMgr = null;
    }

    /** {@inheritDoc} */
    public void setEventName(Object value) {
        this.eventName = (String) value;
    }

    /** {@inheritDoc} */
    public String getEventName() {
        return this.eventName;
    }

    /** {@inheritDoc} */
    public void setIsCustomEvent(Object value) {
        this.isCustomEvent = (Boolean) value;
    }

    /** {@inheritDoc} */
    public Boolean getIsCustomEvent() {
        return this.isCustomEvent;
    }

    /** {@inheritDoc} */
    public void setAuditData(Object value) {
        this.auditData = ((String) value).split(", ");
    }

    /** {@inheritDoc} */
    public String[] getAuditData() {
        return this.auditData;
    }

    /** {@inheritDoc} */
    public void setOutcome(Object value) {

        this.outcome = ((String) value).split(", ");
    }

    /** {@inheritDoc} */
    public String[] getOutcome() {
        return this.outcome;
    }

    /** {@inheritDoc} */
    @Override
    public String getServerID() {
        if (serverID == null) {
            String serverName = null;
            String serverHostName = null;
            String serverUserDir = null;

            serverName = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return variableRegistryServiceRef.getService().resolveString(ENV_VAR_CONTAINERNAME);
                }
            });

            if (ENV_VAR_CONTAINERNAME.equals(serverName)) {
                serverName = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return variableRegistryServiceRef.getService().resolveString(VAR_WLPSERVERNAME);
                    }
                });

            }
            // None of the variables resolved, set the server name back to an empty string.
            if (VAR_WLPSERVERNAME.equals(serverName)) {
                serverName = "";
            }

            serverUserDir = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return variableRegistryServiceRef.getService().resolveString(VAR_WLPUSERDIR);
                }
            });

            if (VAR_WLPUSERDIR.equals(serverUserDir)) {
                serverUserDir = "";
            }

            serverHostName = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return variableRegistryServiceRef.getService().resolveString(ENV_VAR_CONTAINERHOST);
                }
            });

            if (ENV_VAR_CONTAINERHOST.equals(serverHostName)) {
                // env var CONTAINER_HOST did not resolve

                serverHostName = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return variableRegistryServiceRef.getService().resolveString(VAR_DEFAULTHOSTNAME);
                    }
                });

                // defaultHostName variable did not resolve or has resolved to "localhost"
                if (VAR_DEFAULTHOSTNAME.equals(serverHostName) || serverHostName.equals("localhost")) {
                    try {
                        serverHostName = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                            @Override
                            public String run() throws UnknownHostException {
                                return InetAddress.getLocalHost().getCanonicalHostName();
                            }
                        });

                    } catch (PrivilegedActionException pae) {
                        serverHostName = "";
                    }
                }
            }
            serverID = AUDIT_SERVER_ID_PREFIX + serverHostName +
                       AUDIT_SERVER_ID_SEPARATOR + serverUserDir +
                       AUDIT_SERVER_ID_SEPARATOR + serverName;
        }
        return serverID;
    }

}
