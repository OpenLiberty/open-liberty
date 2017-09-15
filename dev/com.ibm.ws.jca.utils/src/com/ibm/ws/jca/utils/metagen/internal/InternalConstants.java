/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.metagen.internal;

/**
 *
 */
public class InternalConstants {
    public static final String WLP_RA_XML_FILE_NAME = "wlp-ra.xml";
    public static final String RA_XML_FILE_NAME = "ra.xml";
    public static final String METATYPE_XML_FILE_NAME = "metatype.xml";
    public static final String METATYPE_PROPERTIES_FILE_NAME = "metatype.properties";

    public static final String TRACE_LEVEL_DEBUG = "debug";
    public static final String TRACE_LEVEL_WARNING = "warning";
    public static final String TRACE_LEVEL_ENTRY_EXIT = "entry-exit";
    public static final String TRACE_LEVEL_ALL = "all";
    public static final String TRACE_LEVEL_NONE = "none";

    /**
     * Prefix that should be appending at the start of all generated pids
     * to ensure there aren't any conflicting pids with other components
     * or within our own component. This comes as a result of someone
     * using an adapter name of com.ibm.ws.jca on something like a JMS
     * connection factory with would resolve to com.ibm.ws.jca.jmsConnectionFactory
     * which is an established pid within the JCA component.
     */
    public static final String JCA_UNIQUE_PREFIX = "com.ibm.ws.jca";

    public static final String RECOMMEND_AUTH_ALIAS_MSG = "It is recommended to use a container managed authentication alias instead of configuring this property";
}
