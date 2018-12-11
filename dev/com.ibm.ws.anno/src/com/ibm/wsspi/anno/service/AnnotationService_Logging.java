/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corporation 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

package com.ibm.wsspi.anno.service;

/**
 * Annotations logging constants.
 */
public interface AnnotationService_Logging {
    /** Usual annotations logger. */
    String ANNO_LOGGER_NAME = "com.ibm.ws.anno";

    /** Functional logger: Log annotation queries to the cache log file. */
    String ANNO_LOGGER_QUERY_NAME = ANNO_LOGGER_NAME + ".query";
    /** Functional logger: Log annotation state after a completed scan. */
    String ANNO_LOGGER_STATE_NAME = ANNO_LOGGER_NAME + ".state";
    /** Functional logger: Log JANDEX activity. */
    String ANNO_LOGGER_JANDEX_NAME = ANNO_LOGGER_NAME + ".jandex";

    //

    @Deprecated
    String ANNO_LOGGER_SERVICE = ANNO_LOGGER_NAME + ".service";
    @Deprecated
    String ANNO_LOGGER_TARGETS = ANNO_LOGGER_NAME + ".target";
    @Deprecated
    String ANNO_LOGGER_TARGETS_VISITOR = ANNO_LOGGER_TARGETS + ".visitor";
    @Deprecated
    String ANNO_LOGGER_INFO = ANNO_LOGGER_NAME + ".info";
    @Deprecated
    String ANNO_LOGGER_UTIL = ANNO_LOGGER_NAME + ".util";
    @Deprecated
    String ANNO_LOGGER_SOURCE = ANNO_LOGGER_NAME + ".source";
    @Deprecated
    String ANNO_LOGGER_SCAN = ANNO_LOGGER_NAME + ".scan";
}
