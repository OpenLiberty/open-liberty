/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.client.rest.internal.resources;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 *
 */
public class RESTClientMessagesUtil {

    private static final ResourceBundle logMessages = ResourceBundle.getBundle("com.ibm.ws.jmx.connector.client.rest.internal.resources.RESTClientMessages");

    // Severe log messages
    public static final String REQUEST_ERROR = "jmx.rest.client.request.error";
    public static final String RESPONSE_ERROR = "jmx.rest.client.response.error";
    public static final String RESPONSE_CODE_ERROR = "jmx.rest.client.response.code.error";

    // Exception messages
    public static final String SERVER_THROWABLE_EXCEPTION = "jmx.rest.client.server.throwable.exception";
    public static final String SERVER_RESULT_EXCEPTION = "jmx.rest.client.server.result.exception";
    public static final String NOT_CONNECTED = "jmx.rest.client.not.connected";
    public static final String URL_NOT_FOUND = "jmx.rest.client.url.not.found";
    public static final String CLASS_NAME_NULL = "jmx.rest.client.class.name.null";
    public static final String ATTRIBUTE_NAME_NULL = "jmx.rest.client.attribute.name.null";
    public static final String ATTRIBUTE_NAMES_NULL = "jmx.rest.client.attribute.names.null";
    public static final String ATTRIBUTE_NULL = "jmx.rest.client.attribute.null";
    public static final String ATTRIBUTE_LIST_NULL = "jmx.rest.client.attribute.list.null";
    public static final String OBJECT_NAME_NULL = "jmx.rest.client.object.name.null";
    public static final String UNEXPECTED_SERVER_THROWABLE = "jmx.rest.client.unexpected.server.throwable";

    // Exception messages with parameters
    public static final String BAD_CREDENTIALS = "jmx.rest.client.bad.credentials";
    public static final String BAD_USER_CREDENTIALS = "jmx.rest.client.bad.user.credentials";
    public static final String OBJECT_NAME_PATTERN = "jmx.rest.client.object.name.pattern";
    public static final String INSTANCE_NOT_FOUND = "jmx.rest.client.instance.not.found";
    public static final String ATTRIBUTE_NOT_FOUND = "jmx.rest.client.attribute.not.found";
    public static final String OPERATION_NOT_FOUND = "jmx.rest.client.operation.not.found";
    public static final String LISTENER_NOT_FOUND = "jmx.rest.client.listener.not.found";

    // Connection listener messages
    public static final String NOTIFICATION_LOST = "jmx.rest.client.notification.lost";
    public static final String CONNECTION_FAILED = "jmx.rest.client.connection.failed";
    public static final String CONNECTION_TEMPORARILY_LOST = "jmx.rest.client.connection.temporarily.lost";
    public static final String CONNECTION_RESTORED_WITH_EXCEPTIONS = "jmx.rest.client.connection.restored.with.exceptions";
    public static final String CONNECTION_RESTORED = "jmx.rest.client.connection.restored";
    public static final String MEMBER_CONNECT = "jmx.rest.client.connection.connect";
    public static final String MEMBER_DISCONNECT = "jmx.rest.client.connection.disconnect";

    // Endpoint related messages
    public static final String NULL_SERVICE_URL = "jmx.rest.client.connection.illegal.argument";
    public static final String INVALID_ENDPOINT = "jmx.rest.client.connection.invalid.endpoint";
    public static final String NO_AVAILABLE_ENDPOINTS = "jmx.rest.client.connection.no.endpoints";

    public static String getMessage(String messageName, Object... arguments) {
        if (arguments.length > 0)
            return MessageFormat.format(logMessages.getString(messageName), arguments);
        else
            return logMessages.getString(messageName);
    }

    public static String getObjID(Object obj) {
        return obj.getClass().getSimpleName() + "@" + System.identityHashCode(obj);
    }
}
