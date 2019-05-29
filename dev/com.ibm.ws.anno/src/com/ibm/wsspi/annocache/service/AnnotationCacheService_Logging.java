/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.annocache.service;

/**
 * Annotations logging constants.
 */
public interface AnnotationCacheService_Logging {
    /** Usual annotations logger. */
    String ANNO_LOGGER_NAME = "com.ibm.ws.annocache";

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
