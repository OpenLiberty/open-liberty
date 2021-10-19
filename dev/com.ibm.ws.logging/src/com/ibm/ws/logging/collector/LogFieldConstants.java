/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.collector;

public class LogFieldConstants {

    //common fields
    public static final String TYPE = "type";
    public static final String HOST = "host";
    public static final String HOSTNAME = "hostName";
    public static final String IBM_USERDIR = "ibm_userDir";
    public static final String WLPUSERDIR = "wlpUserDir";
    public static final String IBM_SERVERNAME = "ibm_serverName";
    public static final String SERVERNAME = "serverName";
    public static final String IBM_DATETIME = "ibm_datetime";
    public static final String DATETIME = "datetime";
    public static final String IBM_SEQUENCE = "ibm_sequence";
    public static final String SEQUENCE = "sequence";
    public static final String MESSAGE = "message";
    public static final String IBM_CLASSNAME = "ibm_className";
    public static final String CLASSNAME = "className";
    public static final String IBM_THREADID = "ibm_threadId";
    public static final String THREADID = "threadId";

    //liberty_message and liberty_trace
    public static final String IBM_MESSAGEID = "ibm_messageId";
    public static final String MESSAGEID = "messageId";
    public static final String MODULE = "module";
    public static final String LOGGERNAME = "loggerName";
    public static final String SEVERITY = "severity";
    public static final String LOGLEVEL = "loglevel";
    public static final String IBM_METHODNAME = "ibm_methodName";
    public static final String METHODNAME = "methodName";
    public static final String LEVELVALUE = "levelValue";
    public static final String THREADNAME = "threadName";
    public static final String CORRELATION_ID = "correlationId";
    public static final String ORG = "org";
    public static final String PRODUCT = "product";
    public static final String COMPONENT = "component";
    public static final String THROWABLE = "throwable";
    public static final String THROWABLE_LOCALIZED = "throwable_localized";
    public static final String FORMATTEDMSG = "formattedMsg";
    public static final String EXTENSIONS_KVPL = "extensions";
    public static final String OBJECT_ID = "objectId";

    //liberty_accesslog
    public static final String IBM_REQUESTSTARTTIME = "ibm_requestStartTime";
    public static final String REQUESTSTARTTIME = "requestStartTime";
    public static final String IBM_URIPATH = "ibm_uriPath";
    public static final String URIPATH = "uriPath";
    public static final String IBM_REQUESTMETHOD = "ibm_requestMethod";
    public static final String REQUESTMETHOD = "requestMethod";
    public static final String IBM_QUERYSTRING = "ibm_queryString";
    public static final String QUERYSTRING = "queryString";
    public static final String IBM_REQUESTHOST = "ibm_requestHost";
    public static final String REQUESTHOST = "requestHost";
    public static final String IBM_REQUESTPORT = "ibm_requestPort";
    public static final String REQUESTPORT = "requestPort";
    public static final String IBM_REMOTEHOST = "ibm_remoteHost";
    public static final String REMOTEHOST = "remoteHost";
    public static final String IBM_USERAGENT = "ibm_userAgent";
    public static final String USERAGENT = "userAgent";
    public static final String IBM_REQUESTPROTOCOL = "ibm_requestProtocol";
    public static final String REQUESTPROTOCOL = "requestProtocol";
    public static final String IBM_BYTESRECEIVED = "ibm_bytesReceived";
    public static final String BYTESRECEIVED = "bytesReceived";
    public static final String IBM_RESPONSECODE = "ibm_responseCode";
    public static final String RESPONSECODE = "responseCode";
    public static final String IBM_ELAPSEDTIME = "ibm_elapsedTime";
    public static final String ELAPSEDTIME = "elapsedTime";
    // Non-default access log fields
    public static final String IBM_REMOTEIP = "ibm_remoteIP";
    public static final String REMOTEIP = "remoteIP";
    public static final String IBM_BYTESSENT = "ibm_bytesSent";
    public static final String BYTESSENT = "bytesSent";
    public static final String IBM_COOKIE = "ibm_cookie";
    public static final String COOKIE = "cookie";
    public static final String IBM_REQUESTELAPSEDTIME = "ibm_requestElapsedTime";
    public static final String REQUESTELAPSEDTIME = "requestElapsedTime";
    public static final String IBM_REQUESTHEADER = "ibm_requestHeader";
    public static final String REQUESTHEADER = "requestHeader";
    public static final String IBM_RESPONSEHEADER = "ibm_responseHeader";
    public static final String RESPONSEHEADER = "responseHeader";
    public static final String IBM_REQUESTFIRSTLINE = "ibm_requestFirstLine";
    public static final String REQUESTFIRSTLINE = "requestFirstLine";
    public static final String IBM_ACCESSLOGDATETIME = "ibm_accessLogDatetime";
    public static final String ACCESSLOGDATETIME = "accessLogDatetime";
    public static final String IBM_REMOTEUSERID = "ibm_remoteUserID";
    public static final String REMOTEUSERID = "remoteUserID";
    public static final String IBM_REMOTEPORT = "ibm_remotePort";
    public static final String REMOTEPORT = "remotePort";

    //liberty_ffdc
    //fields that contain 'XXXXXXX' are not expected to be used
    public static final String IBM_EXCEPTIONNAME = "ibm_exceptionName";
    public static final String EXCEPTIONNAME = "exceptionName";
    public static final String IBM_PROBEID = "ibm_probeID";
    public static final String PROBEID = "probeID";
    public static final String IBM_STACKTRACE = "ibm_stackTrace";
    public static final String STACKTRACE = "stackTrace";
    public static final String IBM_OBJECTDETAILS = "ibm_objectDetails";
    public static final String OBJECTDETAILS = "objectDetails";

    //liberty_gc
    //fields that contain 'XXXXXXX' are not expected to be used
    public static final String IBM_HEAP = "XXXXXXX_ibm_heap";
    public static final String HEAP = "heap";
    public static final String IBM_USED_HEAP = "XXXXXXX_ibm_usedHeap";
    public static final String USED_HEAP = "usedHeap";
    public static final String IBM_MAX_HEAP = "XXXXXXX_ibm_maxHeap";
    public static final String MAX_HEAP = "maxHeap";
    public static final String IBM_DURATION = "XXXXXXX_ibm_duration";
    public static final String DURATION = "duration";
    public static final String IBM_GC_TYPE = "XXXXXXX_ibm_gcType";
    public static final String GC_TYPE = "gcType";
    public static final String IBM_REASON = "XXXXXXX_ibm_reason";
    public static final String REASON = "reason";

    //other
    public static final String IBM_TAG = "ibm_";
    public static final String UTF_8 = "UTF-8";
    public static final String EXT_PREFIX = "ext_";

}
