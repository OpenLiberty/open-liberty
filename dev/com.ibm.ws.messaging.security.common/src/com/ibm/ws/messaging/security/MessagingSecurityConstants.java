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

package com.ibm.ws.messaging.security;

public interface MessagingSecurityConstants {

    /*
     * List of Services Used
     */
    public final static String KEY_SECURITY_SERVICE = "securityService";
    public static final String KEY_CONFIG_ADMIN_SERVICE = "configAdmin";
    public static final String KEY_RUNTIME_SECURITY_SERVICE = "runtimeSecurityService";

    /*
     * Authentication Related Constants
     */
    public static final String SUPPORTED_TOKEN_TYPE = "LTPA";
    public static final String DEFAULT_REALM = "DEFAULT";
    public static final String MESSAGING_JASS_ENTRY_NAME = "system.DEFAULT";

    /*
     * Constants defined for the server.xml tags
     */
    public static final String ROLE = "role";
    public static final String USER = "user";
    public static final String GROUP = "group";
    public static final String NAME = "name";
    public static final String ACTION = "action";

    /*
     * Constants defined for QueuePermission
     */
    public static final String QUEUE_PERMISSION = "queuePermission";
    public static final String QUEUE_REF = "queueRef";

    /*
     * Constants defined for TempDestination Permission
     */
    public static final String TEMPORARY_DESTINATION_PERMISSION = "tempDestinationPermission";
    public static final String PREFIX = "prefix";
    public static final int MAX_PREFIX_SIZE = 12;

    /*
     * Constants defined for Topic Permission
     */
    public static final String TOPIC_PERMISSION = "topicPermission";
    public static final String TOPIC_NAME = "topicName";
    public static final String TOPIC_SPACE = "topicSpaceRef";
    public static final String TOPIC_DELIMITER = "/";

    /*
     * Define the Operation Types
     */
    public static final String OPERATION_TYPE_SEND = "SEND";
    public static final String OPERATION_TYPE_RECEIVE = "RECEIVE";
    public static final String OPERATION_TYPE_CREATE = "CREATE";
    public static final String OPERATION_TYPE_BROWSE = "BROWSE";
    public static final String OPERATION_TYPE_ALL = "ALL";

    public static final String DEFAULT_TOPIC_SPACE_NAME = "Default.Topic.Space";

    /*
     * Authentication types
     */
    public static final String SUBJECT = "SUBJECT";
    public static final String USERID = "USERID";
    public static final String LTPA = "LTPA";
    public static final String IDASSERTION = "IDASSERTION";
    public static final String CLIENTSSL = "CLIENTSSL";

    /*
     * Destination Types
     */
    public static final int DESTINATION_TYPE_QUEUE = 0;
    public static final int DESTINATION_TYPE_TOPICSPACE = 1;

}
