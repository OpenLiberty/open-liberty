/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.api.jmsra;

import com.ibm.wsspi.sib.core.trm.SibTrmConstants;

/**
 * Constants used by the JMS resource adapter.
 */
public interface JmsraConstants {

    /**
     * The trace group for this component.
     */
    static final String MSG_GROUP = com.ibm.ws.sib.utils.TraceGroups.TRGRP_JMSRA;

    /**
     * The message bundle for this component.
     */
    static final String MSG_BUNDLE = "com.ibm.ws.sib.api.jmsra.CWSJRMessages";

    /**
     * The key for the client ID property.
     */
    static final String CLIENT_ID = "clientID";

    /**
     * The key for the user name property.
     */
    static final String USERNAME = "userName";

    /**
     * The key for the password property.
     */
    static final String PASSWORD = "password";

    /**
     * The key for the non-persistent mapping property.
     */
    static final String NON_PERSISTENT_MAP = "nonPersistentMapping";

    /**
     * The key for the persistent mapping property.
     */
    static final String PERSISTENT_MAP = "persistentMapping";

    /**
     * The key for the durable subscription home property.
     */
    static final String DURABLE_SUB_HOME = "durableSubscriptionHome";

    /**
     * The key for the read ahead property.
     */
    static final String READ_AHEAD = "readAhead";

    /**
     * The key for the temporary queue name prefix property.
     */
    static final String TEMP_QUEUE_NAME_PREFIX = "tempQueueNamePrefix";

    /**
     * The key for the temporary topic name prefix property.
     */
    static final String TEMP_TOPIC_NAME_PREFIX = "tempTopicNamePrefix";

    /**
     * The key for the share data source with container managed persistence
     * beans property.
     */
    static final String SHARE_DATA_SOURCE_WITH_CMP = "shareDataSourceWithCMP";

    /**
     * The key for the share durable subscriptions property.
     */
    static final String SHARE_DURABLE_SUBS = "shareDurableSubs";

    /**
     * The key for the property that indicates if the producer will modify
     * the payload after setting it
     */
    static final String PRODUCER_DOES_NOT_MODIFY_PAYLOAD_AFTER_SET = "producerDoesNotModifyPayloadAfterSet";

    /**
     * The key for the property that indicates if the producer will modify
     * the payload after setting it
     */
    static final String CONSUMER_DOES_NOT_MODIFY_PAYLOAD_AFTER_GET = "consumerDoesNotModifyPayloadAfterGet";

    /**
     * The key for the property that indicates if the forwarder will modify
     * the payload after setting it. Applies to ActivationSpecs only -
     * actually synonymous with an equivalent 'producer' property.
     */
    static final String FORWARDER_DOES_NOT_MODIFY_PAYLOAD_AFTER_SET = "forwarderDoesNotModifyPayloadAfterSet";

    /**
     * The key for the bus name property.
     * 
     * @deprecated components should use <code>SibTrmConstants</code> directly
     */
    @Deprecated
    static final String BUSNAME = SibTrmConstants.BUSNAME;

    /**
     * The key for the provider endpoints property.
     * 
     * @deprecated components should use <code>SibTrmConstants</code> directly
     */
    @Deprecated
    static final String PROVIDER_ENDPOINTS = SibTrmConstants.PROVIDER_ENDPOINTS;

    /**
     * The default value for the bus name property.
     */
    static final String DEFAULT_BUS_NAME = "defaultBus";

    /**
     * The default value for the client ID property.
     */
    static final String DEFAULT_CLIENT_ID = null;

    /**
     * The default value for the client ID property for the administratively configured Connection Factory(ex from server.xml) and this is applicable only for Liberty.
     */
    static final String DEFAULT_ADMIN_CONFIG_CLIENT_ID = "clientID";

    /**
     * The default value for the password property.
     */
    static final String DEFAULT_PASSWORD = null;

    /**
     * The default value for the user name property.
     */
    static final String DEFAULT_USER_NAME = null;

    /**
     * The default value for the durable subscription home property.
     */
    static final String DEFAULT_DURABLE_SUB_HOME = "defaultME";

    /**
     * The default value for the temporary queue name prefix property.
     */
    static final String DEFAULT_TEMP_QUEUE_NAME_PREFIX = null;

    /**
     * The default value for the temporary topic name prefix property.
     */
    static final String DEFAULT_TEMP_TOPIC_NAME_PREFIX = null;

    /**
     * The default value for the target property.
     */
    static final String DEFAULT_TARGET = null;

    /**
     * The default value for the target type property.
     */
    //chetan liberty change
    //currently we have only 1 ME per Liberty profile and always we connect to ME.
    //Hence making target type to ME
    static final String DEFAULT_TARGET_TYPE = SibTrmConstants.TARGET_TYPE_ME;

    /**
     * The default value for the target significance property.
     */
    static final String DEFAULT_TARGET_SIGNIFICANCE = SibTrmConstants.TARGET_SIGNIFICANCE_DEFAULT;

    /**
     * The default value for the target transport chain property.
     */
    static final String DEFAULT_TARGET_TRANSPORT_CHAIN = null;

    /**
     * The default value for the provider endpoints property.
     */
    static final String DEFAULT_PROVIDER_ENDPOINTS = null;

    /**
     * The default value for the provider endpoints property.
     */
    static final String DEFAULT_TARGET_TRANSPORT = SibTrmConstants.TARGET_TRANSPORT_DEFAULT;

    /**
     * The default value for the connection proximity property.
     */
    static final String DEFAULT_CONNECTION_PROXIMITY = SibTrmConstants.CONNECTION_PROXIMITY_BUS;

    /**
     * The default value for the share data source with container managed
     * persistence beans property.
     */
    static final Boolean DEFAULT_SHARE_DATA_SOURCE_WITH_CMP = Boolean.FALSE;

    /**
     * Separator used for path elements in JMS resource references.
     */
    static final String PATH_ELEMENT_SEPARATOR = "<#>";

}
