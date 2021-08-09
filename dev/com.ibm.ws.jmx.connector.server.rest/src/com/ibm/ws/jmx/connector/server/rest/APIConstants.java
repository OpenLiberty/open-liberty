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
package com.ibm.ws.jmx.connector.server.rest;

/**
 * Defines the URL resource paths for the core Collective REST API.
 */
public class APIConstants {
    public static final int SERVER_CONNECTOR_VERSION = 6;

    public static final String TRACE_GROUP = "jmx.rest.server.connector";

    public static final String TRACE_BUNDLE_CORE = "com.ibm.ws.jmx.connector.server.internal.resources.RESTServerMessages";

    public static final String TRACE_BUNDLE_FILE_TRANSFER = "com.ibm.ws.jmx.connector.server.internal.resources.FileTransferServerMessages";

    public static final String MEDIA_TYPE_APPLICATION_JSON = "application/json";

    public static final String MEDIA_TYPE_TEXT_PLAIN = "text/plain";

    public static final String MEDIA_TYPE_TEXT_HTML = "text/html";

    public static final String MEDIA_TYPE_IMAGE_JPEG = "image/jpeg";

    /**
     * HTTP GET Method.
     */
    public static final String METHOD_GET = "GET";

    /**
     * HTTP POST Method.
     */
    public static final String METHOD_POST = "POST";

    /**
     * HTTP PUT Method.
     */
    public static final String METHOD_PUT = "PUT";

    /**
     * HTTP DELETE Method.
     */
    public static final String METHOD_DELETE = "DELETE";

    /**
     * HTTP Status code for No Content (204).
     */
    public static final int STATUS_NO_CONTENT = 204;

    /**
     * HTTP Status code for Bad Request (400).
     */
    public static final int STATUS_BAD_REQUEST = 400;

    /**
     * HTTP Status code for Not Found (404).
     */
    public static final int STATUS_NOT_FOUND = 404;

    /**
     * HTTP Status code for Method Not Supported (405).
     */
    public static final int STATUS_METHOD_NOT_SUPPORTED = 405;

    /**
     * HTTP Status code for Gone (410).
     */
    public static final int STATUS_GONE = 410;

    /**
     * HTTP Status code for Invalid Data (422).
     */
    public static final int STATUS_INVALID_DATA = 422;

    /**
     * HTTP Status code for Internal Server Error (500).
     */
    public static final int STATUS_INTERNAL_SERVER_ERROR = 500;

    /**
     * HTTP Status code for Service Unavailable (503).
     */
    public static final int STATUS_SERVICE_UNAVAILABLE = 503;

    /**
     * The base path from the IBM API path from which our REST API is rooted.
     */
    public static final String JMX_CONNECTOR_API_ROOT_PATH = "/IBMJMXConnectorREST";

    public static final String PATH_MBEANS = "/mbeans";

    public static final String PATH_MBEANS_FACTORY = "/mbeans/factory";

    public static final String PATH_MBEANS_OBJECTNAME = "/mbeans/{objectName}";

    public static final String PATH_MBEANS_OBJECTNAME_OPERATIONS_OPERATION = "/mbeans/{objectName}/operations/{operation}";

    public static final String PATH_MBEANS_OBJECTNAME_ATTRIBUTES = "/mbeans/{objectName}/attributes";

    public static final String PATH_MBEANS_OBJECTNAME_ATTRIBUTES_ATTRIBUTE = "/mbeans/{objectName}/attributes/{attribute}";

    public static final String PATH_NOTIFICATIONS = "/notifications";

    public static final String PATH_NOTIFICATIONS_CLIENTID = "/notifications/{clientID}";

    public static final String PATH_NOTIFICATIONS_CLIENTID_INBOX = "/notifications/{clientID}/inbox";

    public static final String PATH_NOTIFICATIONS_CLIENTID_REGISTRATIONS = "/notifications/{clientID}/registrations";

    public static final String PATH_NOTIFICATIONS_CLIENTID_REGISTRATIONS_OBJECTNAME = "/notifications/{clientID}/registrations/{objectName}";

    public static final String PATH_NOTIFICATIONS_CLIENTID_SERVERREGISTRATION = "/notifications/{clientID}/serverRegistrations";

    public static final String PATH_NOTIFICATIONS_CLIENTID_SERVERREGISTRATIONS_SOURCEOBJNAME_LISTENERS = "/notifications/{clientID}/serverRegistrations/{source_objName}/listeners";

    public static final String PATH_NOTIFICATIONS_CLIENTID_SERVERREGISTRATIONS_SOURCEOBJNAME_LISTENERS_LISTENEROBJNAME_IDS = "/notifications/{clientID}/serverRegistrations/{source_objName}/listeners/{listener_objName}/ids";

    public static final String PATH_NOTIFICATIONS_CLIENTID_SERVERREGISTRATIONS_SOURCEOBJNAME_LISTENERs_LISTENEROBJNAME_IDS_REGISTRATIONID = "/notifications/{clientID}/serverRegistrations/{source_objName}/listeners/{listener_objName}/ids/{registrationID}";

    public static final String PATH_FILE_COLLECTION = "/file/collection";

    public static final String PATH_FILE_FILEPATH = "/file/{filePath}";

    public static final String PATH_FILE_STATUS = "/file/status";

    public static final String PATH_FILE_STATUS_TASKID = "/file/status/{taskID}";

    public static final String PATH_FILE_STATUS_TASKID_PROPERTIES = "/file/status/{taskID}/properties";

    public static final String PATH_FILE_STATUS_TASKID_PROPERTIES_PROPERTY = "/file/status/{taskID}/properties/{property}";

    public static final String PATH_FILE_STATUS_TASKID_HOSTS = "/file/status/{taskID}/hosts";

    public static final String PATH_FILE_STATUS_TASKID_HOSTS_HOST = "/file/status/{taskID}/hosts/{host}";

    public static final String PATH_FILETRANSFER_FILEPATH = "/fileTransfer/{filePath}";

    public static final String PATH_FILETRANSFER_ROUTER_FILEPATH = "/fileTransfer/router/{filePath}";

    public static final String PATH_ROUTER = "/router";

    public static final String PATH_ROUTER_URI = "/router/{uri}";

    public static final String PATH_ROUTER_MBEANS_OBJECTNAME_ATTRIBUTES = "/router/mbeans/{objectName}/attributes";

    public static final String PATH_ROUTER_MBEANS_OBJECTNAME_ATTRIBUTES_ATTRIBUTE = "/router/mbeans/{objectName}/attributes/{attribute}";

    public static final String PATH_ROOT = "/";

    public static final String PATH_MBEANCOUNT = "/mbeanCount";

    public static final String PATH_DEFAULTDOMAIN = "/defaultDomain";

    public static final String PATH_DOMAINS = "/domains";

    public static final String PATH_MBEANSERVER = "/mbeanServer";

    public static final String PATH_GRAPH = "/graph";

    public static final String PATH_API = "/api";

    public static final String PATH_INSTANCEOF = "/instanceOf";

    public static final String PATH_POJO = "/pojo";

    public static final String PARAM_OBJECT_NAME = "objectName";

    public static final String PARAM_ATTRIBUTE = "attribute";

    public static final String PARAM_OPERATION = "operation";

    public static final String PARAM_CLASSNAME = "className";

    public static final String PARAM_FILEPATH = "filePath";

    public static final String PARAM_START_OFFSET = "startOffset";

    public static final String PARAM_END_OFFSET = "endOffset";

    public static final String PARAM_EXPAND_ON_COMPLETION = "expandOnCompletion";

    public static final String PARAM_LOCAL = "local";

    public static final String PARAM_RECURSIVE_DELETE = "recursiveDelete";

    public static final String PARAM_TASK_ID = "taskID";

    public static final String PARAM_HOST = "host";

    public static final String PARAM_PROPERTY = "property";

    public static final String PARAM_URI = "uri";

    public static final String PARAM_CLIENTID = "clientID";

    public static final String PARAM_SOURCE_OBJNAME = "source_objName";

    public static final String PARAM_LISTENER_OBJNAME = "listener_objName";

    public static final String PARAM_REGISTRATIONID = "registrationID";
}
