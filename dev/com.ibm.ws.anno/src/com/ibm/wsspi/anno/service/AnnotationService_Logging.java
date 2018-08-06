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

public interface AnnotationService_Logging {
    // Root logging constant.
    String ANNO_LOGGER_NAME = "com.ibm.ws.anno";

    // Function categories ... these match entire packages.
    String ANNO_LOGGER_SERVICE = ANNO_LOGGER_NAME + ".service";

    String ANNO_LOGGER_TARGETS = ANNO_LOGGER_NAME + ".target";
    String ANNO_LOGGER_TARGETS_VISITOR = ANNO_LOGGER_TARGETS + ".visitor";

    String ANNO_LOGGER_SOURCE = ANNO_LOGGER_NAME + ".source";
    String ANNO_LOGGER_UTIL = ANNO_LOGGER_NAME + ".util";
    String ANNO_LOGGER_INFO = ANNO_LOGGER_NAME + ".info";

    // Detail categories ... these cross the boundaries of the function categories.
    String ANNO_LOGGER_STATE_NAME = ANNO_LOGGER_NAME + ".state";
    String ANNO_LOGGER_SCAN = ANNO_LOGGER_NAME + ".scan";

    String ANNO_LOGGER_JANDEX_NAME = ANNO_LOGGER_NAME + ".jandex";
}
