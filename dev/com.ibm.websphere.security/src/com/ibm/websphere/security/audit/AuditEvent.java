/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.audit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.ibm.ws.security.utils.SecurityUtils;
import com.ibm.wsspi.security.audit.AuditService;

/**
 * This is the base class for audit events. The default constructor
 * will create an event with default values for the minimum required
 * set of CADF record fields.
 */
public class AuditEvent {

    /**
     * CADF JSON field names
     */
    public final static String EVENTNAME = "eventName";
    public final static String EVENTTYPE = "eventType";
    public final static String EVENTTIME = "eventTime";
    public final static String LOGGING_EVENTTIME = "loggingEventTime";
    public final static String WASEVENTTYPE = "wasEventType";
    public final static String HASHTAG = "hashTag";

    public final static String OBSERVER = "observer";
    public final static String OBSERVER_ID = "observer.id";
    public final static String OBSERVER_NAME = "observer.name";
    public final static String OBSERVER_TYPEURI = "observer.typeURI";
    public final static String OBSERVER_HOST_ADDRESS = "observer.host.address";
    public final static String OBSERVER_HOST_AGENT = "observer.host.agent";

    public final static String TARGET = "target";
    public final static String TARGET_ACTION = "target.action";
    public final static String TARGET_ID = "target.id";
    public final static String TARGET_UNIQUENAME = "target.uniqueName";
    public final static String TARGET_ENTITY_TYPE = "target.entityType";
    public final static String TARGET_EXTENDED_PROPERTIES = "target.extendedProperties";
    public final static String TARGET_METHOD = "target.method";
    public final static String TARGET_NAME = "target.name";

    public final static String TARGET_MESSAGING_BUSNAME = "target.messaging.busname";
    public final static String TARGET_MESSAGING_ENGINE = "target.messaging.engine";
    public final static String TARGET_MESSAGING_LOGIN_TYPE = "target.messaging.loginType";
    public final static String TARGET_MESSAGING_REMOTE_CHAIN_NAME = "target.messaging.remote.chainName";
    public final static String TARGET_MESSAGING_USER_NAME = "target.messaging.userName";
    public final static String TARGET_MESSAGING_DESTINATION = "target.messaging.destination";
    public final static String TARGET_MESSAGING_OPERATIONTYPE = "target.messaging.operationType";
    public final static String TARGET_MESSAGING_CALLTYPE = "target.messaging.callType";
    public final static String TARGET_MESSAGING_JMS_RESOURCE = "target.messaging.jmsResource";
    public final static String TARGET_MESSAGING_JMS_ACTIONS = "target.messaging.jmsActions";
    public final static String TARGET_MESSAGING_JMS_ROLES = "target.messaging.jmsRoles";
    public final static String TARGET_MESSAGING_JMS_REQUESTOR_TYPE = "target.messaging.jmsRequestorType";
    public final static String TARGET_MESSAGING_JMS_QUEUE_PERMISSIONS = "target.messaging.queuePermissions";
    public final static String TARGET_MESSAGING_JMS_TOPIC_PERMISSIONS = "target.messaging.topicPermissions";
    public final static String TARGET_MESSAGING_JMS_TEMPORARY_DESTINATION_PERMISSIONS = "target.messaging.tempDestinationPermissions";

    public final static String TARGET_JMX_NOTIFICATION_NAME = "target.jmx.notification.name";
    public final static String TARGET_JMX_NOTIFICATION_LISTENER = "target.jmx.notification.listener";
    public final static String TARGET_JMX_NOTIFICATION_FILTER = "target.jmx.notification.filter";
    public final static String TARGET_JMX_NOTIFICATION_HANDBACK = "target.jmx.notification.handback";

    public final static String TARGET_JMX_MBEAN_NAME = "target.jmx.mbean.name";
    public final static String TARGET_JMX_MBEAN_ACTION = "target.jmx.mbean.action";
    public final static String TARGET_JMX_MBEAN_ATTRIBUTE_NAME = "target.jmx.mbean.attribute.name";
    public final static String TARGET_JMX_MBEAN_ATTRIBUTE_NAMES = "target.jmx.mbean.attribute.names";
    public final static String TARGET_JMX_MBEAN_ATTRIBUTE_ACTION = "target.jmx.mbean.attribute.action";
    public final static String TARGET_JMX_MBEAN_CLASSNAME = "target.jmx.mbean.classname";
    public final static String TARGET_JMX_MBEAN_CLASSLOADER_NAME = "target.jmx.mbean.classloader.name";
    public final static String TARGET_JMX_MBEAN_INVOKE_OPERATION = "target.jmx.mbean.invoke.operation";
    public final static String TARGET_JMX_MBEAN_PARAMS = "target.jmx.mbean.params";
    public final static String TARGET_JMX_MBEAN_SIGNATURE = "target.jmx.mbean.signature";
    public final static String TARGET_JMX_MBEAN_QUERYEXP = "target.jmx.mbean.queryExp";

    public final static String TARGET_APPNAME = "target.appname";
    public final static String TARGET_PARAMS = "target.params";
    public final static String TARGET_TYPEURI = "target.typeURI";
    public final static String TARGET_HOST_ADDRESS = "target.host.address";
    public final static String TARGET_HOST_AGENT = "target.host.agent";
    public final static String TARGET_CREDENTIAL_TYPE = "target.credential.type";
    public final static String TARGET_CREDENTIAL_TOKEN = "target.credential.token";
    public final static String TARGET_REPOSITORY_ID = "target.repositoryId";
    public static final String TARGET_SESSION = "target.session";
    public static final String TARGET_DELEGATION_USERS = "target.delegation.users";
    public static final String TARGET_ORIGINAL_AUTH_TYPE = "target.authtype.original";
    public static final String TARGET_FAILOVER_AUTH_TYPE = "target.authtype.failover";
    public static final String TARGET_URINAME = "target.uriname";
    public static final String TARGET_ROLE_NAMES = "target.role.names";
    public static final String TARGET_RUNAS_ROLE = "target.runas.role";
    public static final String TARGET_JACC_PERMISSIONS = "target.jacc.permissions";
    public static final String TARGET_JACC_CONTAINER = "target.jacc.container";
    public static final String TARGET_JASPI_PROVIDER = "target.jaspi.provider";
    public static final String TARGET_JASPI_AUTHTYPE = "target.jaspi.authtype";
    public static final String TARGET_REALM = "target.realm";
    public static final String TARGET_EJB_MODULE_NAME = "target.ejb.module.name";
    public static final String TARGET_EJB_METHOD_INTERFACE = "target.ejb.method.interface";
    public static final String TARGET_EJB_METHOD_SIGNATURE = "target.ejb.method.signature";
    public static final String TARGET_EJB_BEAN_NAME = "target.ejb.beanname";
    public static final String TARGET_EJB_METHOD_PARAMETERS = "target.ejb.method.parameters";

    public final static String INITIATOR = "initiator";
    public final static String INITIATOR_ID = "initiator.id";
    public final static String INITIATOR_NAME = "initiator.name";
    public final static String INITIATOR_TYPEURI = "initiator.typeURI";
    public final static String INITIATOR_HOST_ADDRESS = "initiator.host.address";
    public final static String INITIATOR_HOST_AGENT = "initiator.host.agent";

    public final static String OUTCOME = "outcome";
    public final static String ACTION = "action";

    public final static String REASON = "reason";
    public final static String REASON_CODE = "reason.reasonCode";
    public final static String REASON_TYPE = "reason.reasonType";

    /**
     * HTTP method types
     */
    public static final String TARGET_METHOD_GET = "GET";
    public static final String TARGET_METHOD_POST = "POST";
    public static final String TARGET_METHOD_PUT = "PUT";
    public static final String TARGET_METHOD_DELETE = "DELETE";
    public static final String TARGET_METHOD_CUSTOM = "CUSTOM";
    /**
     * Reason types
     */
    public static final String REASON_TYPE_HTTP = "HTTP";
    public static final String REASON_TYPE_HTTPS = "HTTPS";
    public static final String REASON_TYPE_EJB_DENYALL = "EJB Deny All";
    public static final String REASON_TYPE_EJB_PERMITALL = "EJB Permit All";
    public static final String REASON_TYPE_EJB_NO_ROLES = "EBJ No Roles";
    public static final String REASON_TYPE_EJB_NO_AUTHZ_SERVICE = "EJB No Authorization Service Found";

    public static final String REASON_TYPE_MISSING_ENTITY_DATA_OBJECT = "Missing entity data object"; // 201
    public static final String REASON_TYPE_ACTION_MULTIPLE_ENTITIES_SPECIFIED = "Multiple entities specified"; // 202
    public static final String REASON_TYPE_ENTITY_IDENTIFIER_NOT_SPECIFIED = "Entity identifier not specified"; // 203
    public static final String REASON_TYPE_ENTITY_NOT_IN_REALM_SCOPE = "Entity not in the scope of the realm"; // 204
    public static final String REASON_TYPE_INVALID_PARENT_UNIQUE_ID = "Invalid parent uniqueId"; // 205
    public static final String REASON_TYPE_DEFAULT_PARENT_NOT_FOUND = "Default parent not found"; // 206
    public static final String REASON_TYPE_MISSING_REPOSITORIES_FOR_GROUPS_CONFIGURATION = "Repository for groups is missing or indicates groups are in cross repositories"; // 207
    public static final String REASON_TYPE_ENTITY_TYPE_NOT_SUPPORTED = "Entity type not supported"; // 208
    public static final String REASON_TYPE_CANNOT_WRITE_TO_READ_ONLY_REPOSITORY = "Cannot write to read only repository"; // 209
    public static final String REASON_TYPE_EXTERNAL_NAME_CONTROL_NOT_FOUND = "External name control not found"; // 210
    public static final String REASON_TYPE_INVALID_IDENTIFIER = "Invalid identifier"; // 211
    public static final String REASON_TYPE_ENTITY_NOT_FOUND = "Entity not found"; // 212
    public static final String REASON_TYPE_MISSING_SORT_KEY = "Missing sort key"; // 213
    public static final String REASON_TYPE_MISSING_SEARCH_CONTROL = "Missing search control"; // 214
    public static final String REASON_TYPE_INCORRECT_SEARCH_LIMIT = "Incorrect search limit"; // 215
    public static final String REASON_TYPE_CANNOT_SPECIFY_COUNT_LIMIT = "Cannot specify count limit"; // 216
    public static final String REASON_TYPE_MISSING_SEARCH_EXPRESSION = "Missing search expression"; // 217
    public static final String REASON_TYPE_SEARCH_EXPRESSION_ERROR = "Search expression error"; // 218
    public static final String REASON_TYPE_INVALID_SEARCH_EXPRESSION = "Invalid search expression"; // 219
    public static final String REASON_TYPE_EXCEED_MAX_TOTAL_SEARCH_LIMIT = "Exceeding max total search limit"; // 220
    public static final String REASON_TYPE_ENTITY_SEARCH_FAILED = "Entity search failed"; // 221

    public static final String REASON_TYPE_NON_COMPLIANT_MBEAN = "Not compliant MBean";
    public static final String REASON_TYPE_INSTANCE_ALREADY_EXISTS = "Instance of MBean already exists";
    public static final String REASON_TYPE_MBEAN_REGISTRATION_FAILURE = "MBean registration failure";
    public static final String REASON_TYPE_INSTANCE_NOT_FOUND = "Instance of MBean not found";

    /**
     * Outcomes
     */
    public static final String OUTCOME_REDIRECT_TO_PROVIDER = "redirect_to_provider";
    public static final String OUTCOME_TAI_CHALLENGE = "tai_challenge";
    public static final String OUTCOME_REDIRECT = "redirect";
    public static final String OUTCOME_CHALLENGE = "challenge";
    public static final String OUTCOME_FAILURE = "failure";
    public static final String OUTCOME_SUCCESS = "success";
    public static final String OUTCOME_DENIED = "denied";

    /**
     * Credential types for inbound requests
     */
    public static final String CRED_TYPE_FORM = "FORM";
    public static final String CRED_TYPE_CERTIFICATE = "CLIENT-CERT";
    public static final String CRED_TYPE_BASIC = "BASIC";
    public static final String CRED_TYPE_JASPIC = "JASPIC";
    public static final String CRED_TYPE_TAI = "TrustAssociationInterceptor";
    public static final String CRED_TYPE_IDTOKEN = "IDToken";
    public static final String CRED_TYPE_SPNEGO = "SPNEGO";
    public static final String CRED_TYPE_OAUTH_TOKEN = "OAuth token";
    public static final String CRED_TYPE_LTPATOKEN2 = "LtpaToken2";
    public static final String CRED_TYPE_JASPI_AUTH = "JASPI_AUTH";

    public static final String WEB = "WEB";
    public static final String EJB = "EJB";

    /**
     * Static declarations and initialization
     */
    public final static String STD_ID = getServerID();
    public final static TreeMap<String, Object> STD_OBSERVER = new TreeMap<String, Object>();
    public final static TreeMap<String, Object> STD_INITIATOR = new TreeMap<String, Object>();
    public final static TreeMap<String, Object> STD_TARGET = new TreeMap<String, Object>();

    static {
        STD_OBSERVER.put(AuditEvent.OBSERVER_ID, STD_ID);
        STD_OBSERVER.put(AuditEvent.OBSERVER_NAME, "SecurityService");
        STD_OBSERVER.put(AuditEvent.OBSERVER_TYPEURI, "service/server");
        STD_TARGET.put(AuditEvent.TARGET_ID, STD_ID);
        STD_TARGET.put(AuditEvent.TARGET_TYPEURI, "service/application/web");
    }

    /**
     * Using synchronizedMap for thread safety. Be sure to synchronize on eventMap
     * whenever iterating over it.
     */
    private final Map<String, Object> eventMap = Collections.synchronizedMap(new TreeMap<String, Object>());

    private static String serverID = null;

    public AuditEvent() {
        setEventTime(getCurrentTime());

    }

    /**
     * @return current time in yyyy-MM-dd HH:mm:ss.SSS format
     */
    public String getCurrentTime() {

        //TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"); // Quoted "Z" to indicate UTC, no timezone offset
        //df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());

        return nowAsISO;
    }

    /**
     * Get a Map of all the initiator keys/values.
     *
     * @return - Map of all the initiator keys/values
     */
    public Map<String, Object> getInitiator() {
        TreeMap<String, Object> map = new TreeMap<String, Object>();
        synchronized (eventMap) {
            for (Entry<String, Object> entry : eventMap.entrySet()) {
                if (entry.getKey().startsWith(INITIATOR))
                    map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    /**
     * Set the initiator keys/values. The provided Map will completely replace
     * the existing initiator, i.e. all current initiator keys/values will be
     * removed and the new initiator keys/values will be added.
     *
     * @param initiator - Map of all the initiator keys/values
     */
    public void setInitiator(Map<String, Object> initiator) {
        removeEntriesStartingWith(INITIATOR);
        eventMap.putAll(initiator);
    }

    /**
     * Get a Map of all the observer keys/values.
     *
     * @return Map of all the observer keys/values
     */
    public Map<String, Object> getObserver() {
        TreeMap<String, Object> map = new TreeMap<String, Object>();
        synchronized (eventMap) {
            for (Entry<String, Object> entry : eventMap.entrySet()) {
                if (entry.getKey().startsWith(OBSERVER))
                    map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    /**
     * Set the observer keys/values. The provided Map will completely replace
     * the existing observer, i.e. all current observer keys/values will be removed
     * and the new observer keys/values will be added.
     *
     * @param observer - Map of all the observer keys/values
     */
    public void setObserver(Map<String, Object> observer) {
        removeEntriesStartingWith(OBSERVER);
        eventMap.putAll(observer);
    }

    /**
     * Get a Map of all the target keys/values.
     *
     * @return Map of all the target keys/values
     */
    public Map<String, Object> getTarget() {
        TreeMap<String, Object> map = new TreeMap<String, Object>();
        synchronized (eventMap) {
            for (Entry<String, Object> entry : eventMap.entrySet()) {
                if (entry.getKey().startsWith(TARGET))
                    map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    /**
     * Set the target keys/values. The provided Map will completely replace
     * the existing target, i.e. all current target keys/values will be removed
     * and the new target keys/values will be added.
     *
     * @param target - Map of all the target keys/values
     */
    public void setTarget(Map<String, Object> target) {
        removeEntriesStartingWith(TARGET);
        eventMap.putAll(target);
    }

    /**
     * Get a Map of all the reason keys/values.
     *
     * @return
     */
    public Map<String, Object> getReason() {
        TreeMap<String, Object> map = new TreeMap<String, Object>();
        synchronized (eventMap) {
            for (Entry<String, Object> entry : eventMap.entrySet()) {
                if (entry.getKey().startsWith(REASON))
                    map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    /**
     * Set the reason keys/values. The provided Map will completely replace
     * the existing reason, i.e. all current reason keys/values will be removed
     * and the new reason keys/values will be added.
     *
     * @param reason
     */
    public void setReason(Map<String, Object> reason) {
        removeEntriesStartingWith(REASON);
        eventMap.putAll(reason);
    }

    /**
     * Get a Map with all of the keys/values of the event
     *
     * @return - a thread safe Map (created with Collections.synchronizedMap())
     */
    public Map<String, Object> getMap() {
        Map<String, Object> retMap = Collections.synchronizedMap(new TreeMap<String, Object>());
        retMap.putAll(eventMap);
        return retMap;
    }

    /**
     * Replace the entire event with the keys/values in the provided Map.
     * All existing keys/values will be lost.
     *
     * @param map
     */
    public void setMap(Map<String, Object> map) {
        eventMap.clear();
        eventMap.putAll(map);
    }

    /**
     * Get the event outcome.
     *
     * @return
     */
    public String getOutcome() {
        return (String) eventMap.get(OUTCOME);
    }

    /**
     * Set the event outcome.
     *
     * @param outcome
     */
    public void setOutcome(String outcome) {
        eventMap.put(OUTCOME, outcome);
    }

    /**
     * Get the event action.
     *
     * @return
     */
    public String getAction() {
        return (String) eventMap.get(ACTION);
    }

    /**
     * Set the event action.
     *
     * @param action
     */
    public void setAction(String action) {
        eventMap.put(ACTION, action);
    }

    /**
     * Get the event type.
     *
     * @return - the event type
     */
    public String getType() {
        return (String) eventMap.get(EVENTTYPE);
    }

    /**
     * Set the event type.
     *
     * @param eventType - the event type
     */
    public void setType(String eventType) {
        eventMap.put(EVENTTYPE, eventType);
    }

    /**
     * Get the Websphere event type.
     *
     * @return - the Websphere event type
     */
    public String getWASType() {
        return (String) eventMap.get(WASEVENTTYPE);
    }

    /**
     * Set the Websphere event type.
     *
     * @param wasEventType - the Websphere event type.
     */
    public void setWASType(String wasEventType) {
        eventMap.put(WASEVENTTYPE, wasEventType);
    }

    /**
     * Get the event time.
     *
     * @return - the event time
     */
    public String getEventTime() {
        return (String) eventMap.get(EVENTTIME);
    }

    /**
     * Set the event time.
     *
     * @param eventTime - the event time.
     */
    public void setEventTime(String eventTime) {
        eventMap.put(EVENTTIME, eventTime);
    }

    /**
     * Get the hashtag value for a configuration change.
     *
     * @return - hashtag value
     */
    public String getHashtag() {
        return (String) eventMap.get(HASHTAG);
    }

    /**
     * Set the hashtag value for a configuration change.
     *
     * @param hash - hashtag value
     */
    public void setHashtag(String hash) {
        eventMap.put(HASHTAG, hash);
    }

    /**
     * Get the value for the given key.
     *
     * @return - value
     */
    public Object get(String key) {
        return eventMap.get(key);
    }

    /**
     * Set a key with the given value.
     *
     * @param key
     * @param value
     */
    public void set(String key, Object value) {
        eventMap.put(key, value);
    }

    /**
     * Add the keys/values in the map to the event, replacing any
     * existing keys/values.
     *
     * @param map
     */
    public void set(Map<String, Object> map) {
        eventMap.putAll(map);
    }

    /**
     * @return unique identifier id of this server
     */
    public static String getServerID() {
        if (serverID == null) {
            AuditService auditService = SecurityUtils.getAuditService();
            if (auditService != null) {
                serverID = auditService.getServerID();
            }
        }
        return serverID;
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        synchronized (eventMap) {
            for (Entry<String, Object> entry : eventMap.entrySet()) {
                buff.append("\n").append(entry.getKey()).append("    :    ").append(entry.getValue() != null ? entry.getValue().toString() : "null");
            }
        }
        return buff.toString();
    }

    /**
     * Send this event to the audit service for logging.
     *
     * @throws AuditServiceUnavailableException
     */
    public void send() throws AuditServiceUnavailableException {
        AuditService auditService = SecurityUtils.getAuditService();
        if (auditService != null) {
            auditService.sendEvent(this);
        } else {
            throw new AuditServiceUnavailableException();
        }
    }

    /**
     * Check to see if auditing is required for this event.
     *
     * @return true - this event should be audited
     *         false - this event should not be audited
     *
     * @throws AuditServiceUnavailableException
     */
    public boolean isAuditRequired() throws AuditServiceUnavailableException {
        return isAuditRequired(getType(), getOutcome());
    }

    /**
     * Check to see if auditing is required for an event type and outcome.
     *
     * @param eventType SECURITY_AUTHN, SECURITY_AUTHZ, etc
     * @param outcome OUTCOME_SUCCESS, OUTCOME_DENIED, etc.
     * @return true - events with the type/outcome should be audited
     *         false - events with the type/outcome should not be audited
     *
     * @throws AuditServiceUnavailableException
     */
    public static boolean isAuditRequired(String eventType, String outcome) throws AuditServiceUnavailableException {
        AuditService auditService = SecurityUtils.getAuditService();
        if (auditService != null) {
            return auditService.isAuditRequired(eventType, outcome);
        } else {
            throw new AuditServiceUnavailableException();
        }
    }

    /**
     * Remove all entries starting with the given key
     *
     * @param key
     */
    private void removeEntriesStartingWith(String key) {
        synchronized (eventMap) {
            Iterator<String> iter = eventMap.keySet().iterator();
            while (iter.hasNext()) {
                String str = iter.next();
                if (str.startsWith(key)) {
                    iter.remove();
                }
            }
        }
    }
}
