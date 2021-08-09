/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.osgi;

/**
 *
 */
public interface OsgiLogConstants {
    final static String TRACE_GROUP = "osgilogging";
    static final String TRACE_ENABLED = "enabled";

    final static String MESSAGE_BUNDLE = "com.ibm.ws.logging.internal.osgi.resources.OSGiMessages";

    final static String LOG_SERVICE_GROUP = "logservice";
    static final String LOGGER_EVENTS = "Events";
    static final String TRACE_SPEC_OSGI_EVENTS = "OSGi.Events";
    static final String LOGGER_EVENTS_PREFIX = LOGGER_EVENTS + '.';
    static final String EQUINOX_METATYPE_BSN = "com.ibm.ws.org.eclipse.equinox.metatype";
}
