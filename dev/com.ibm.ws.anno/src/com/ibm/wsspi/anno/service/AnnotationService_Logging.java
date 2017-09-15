/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.anno.service;

public interface AnnotationService_Logging {
    // Root logging constant.
    public static final String ANNO_LOGGER = "com.ibm.ws.anno";

    // Function categories ... these match entire packages.
    public static final String ANNO_LOGGER_SERVICE = ANNO_LOGGER + ".service";

    public static final String ANNO_LOGGER_TARGETS = ANNO_LOGGER + ".target";
    public static final String ANNO_LOGGER_TARGETS_VISITOR = ANNO_LOGGER_TARGETS + ".visitor";

    public static final String ANNO_LOGGER_SOURCE = ANNO_LOGGER + ".source";
    public static final String ANNO_LOGGER_UTIL = ANNO_LOGGER + ".util";
    public static final String ANNO_LOGGER_INFO = ANNO_LOGGER + ".info";

    // Detail categories ... these cross the boundaries of the function categories.
    public static final String ANNO_LOGGER_STATE = ANNO_LOGGER + ".state";
    public static final String ANNO_LOGGER_SCAN = ANNO_LOGGER + ".scan";
}
