/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
}
