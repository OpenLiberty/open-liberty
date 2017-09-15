/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal;

/**
 * Definitions for RAS tracing variables and any translated message in the
 * HTTP channel.
 */
public interface HttpMessages {

    // -------------------------------------------------------------------------
    // Public Constants
    // -------------------------------------------------------------------------

    /** RAS trace bundle for the HTTP channel */
    String HTTP_BUNDLE = "com.ibm.ws.http.channel.internal.resources.httpchannelmessages";
    /** RAS trace name for the HTTP channel */
    String HTTP_TRACE_NAME = "HTTPChannel";

    // -------------------------------------------------------------------------
    // NLS Messages
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Non translated messages
    // -------------------------------------------------------------------------

    /** Used when starting a read for another inbound request */
    String MSG_READ_PERSISTENT = "Reading for another request";
    /** Msg when that read fails */
    String MSG_READ_PERSISTENT_FAILED = "Read for another request failed";
    /** Msg when a generic read fails */
    String MSG_READ_FAIL = "A read during the connection failed due to a socket exception.";
    /** Msg when a generic write fails */
    String MSG_WRITE_FAIL = "The write of the response failed from a socket exception.";
    /** Msg when the sendError API is being used */
    String MSG_CONN_SENDERROR = "Sending an error status code: ";
    /** Msg when connection persistence is updated on the service context */
    String MSG_CONN_PERSIST = "Connection persistence updated to ";
    /** Msg when a secure connection is detected */
    String MSG_CONN_SSL = "Connection is secure";
    /** Msg when the request contains the Expect 100 Continues header */
    String MSG_CONN_EXPECT100 = "Request contains the Expect: 100 Continues header";
    /** Msg when first starting to send data back to the client */
    String MSG_CONN_SENDING_HEADERS = "Sending headers to client";
    /** Msg when a new connection is opened */
    String MSG_CONN_STARTING = "Received new connection ";
    /** Msg when a connection is being closed */
    String MSG_CONN_CLOSING = "Closing connection to client";
    /** Msg when the message contained a mismatch in lengths */
    String MSG_CONN_INVALID_LENGTHS = "Mismatch in content-length and bytes written: ";
    /** Msg when an invalid first line is detected */
    String MSG_PARSE_INVALID_FIRSTLINE = "Invalid number of first line tokens";
    /** Msg when the parsing of a message is started */
    String MSG_PARSE_STARTING = "Starting to parse the message";
    /** Msg when the parsing of a message is finished */
    String MSG_PARSE_FINISHED = "Finished parsing the message";
    /** Msg used when parsing a body chunk length */
    String MSG_PARSE_CHUNK_LEN = "Parsed chunk size of ";
    /** Msg used when beginning to parse a content-length body */
    String MSG_PARSE_CONTENT_LEN = "Parsed message content-length of ";
    /** Msg showing the configuration setting for the log level */
    String MSG_CONFIG_LEVEL = "Configuration specifies a loglevel of ";
    /** Msg showing the configuration wants the NCSA common format */
    String MSG_CONFIG_FORMAT_COMMON = "Configuration specifies the NCSA common format for ";
    /** Msg showing the configuration wants the NCSA combined format */
    String MSG_CONFIG_FORMAT_COMBINED = "Configuration specifies the NCSA combined format for ";
    /** Msg showing the access log name in the configuration */
    String MSG_CONFIG_ACCESSLOG = "Configuration has the access file name as : ";
    /** Msg showing the maximum file size of the access log */
    String MSG_CONFIG_ACCESSLOG_SIZE = "Access log maximum file size is ";
    /** Msg showing the maximum backup files of the access log */
    String MSG_CONFIG_ACCESSLOG_MAXFILES = "Access log maximum number of backup files is ";
    /** Msg showing the error log name in the configuration */
    String MSG_CONFIG_ERRORLOG = "Configuration has the error file name as : ";
    /** Msg showing the maximum file size of the access log */
    String MSG_CONFIG_ERRORLOG_SIZE = "Error log maximum file size is ";
    /** Msg showing the maximum backup files of the error log */
    String MSG_CONFIG_ERRORLOG_MAXFILES = "Error log maximum number of backup files is ";
    /** Msg used when the access log is disabled but error/debug is not */
    String MSG_CONFIG_ACCESSLOG_DISABLED = "Access log is disabled by configuration.";
    /** Msg used when an invalid non-delimited body is found */
    String MSG_INVALID_BODY = "Invalid non-delimited message body";

}
