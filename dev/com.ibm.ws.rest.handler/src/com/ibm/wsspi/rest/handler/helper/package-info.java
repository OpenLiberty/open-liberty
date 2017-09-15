/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * Provides the main OSGi container for the REST Handler framework.
 * 
 * @version 1.4
 */
@org.osgi.annotation.versioning.Version("1.4")
@TraceOptions(traceGroup = TraceConstants.TRACE_GROUP, messageBundle = TraceConstants.TRACE_BUNDLE_CORE)
package com.ibm.wsspi.rest.handler.helper;

import com.ibm.websphere.ras.annotation.TraceOptions;
import com.ibm.ws.rest.handler.internal.TraceConstants;

