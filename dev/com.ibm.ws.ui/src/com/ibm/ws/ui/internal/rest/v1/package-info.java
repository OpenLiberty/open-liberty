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

/**
 * This package contains the base JAX-RS application used to define the
 * REST API.
 * @version 1.0
 */
@org.osgi.annotation.versioning.Version("1.0")
@TraceOptions(traceGroup = TraceConstants.TRACE_GROUP, messageBundle = TraceConstants.MESSAGE_BUNDLE)
package com.ibm.ws.ui.internal.rest.v1;

import com.ibm.websphere.ras.annotation.TraceOptions;
import com.ibm.ws.ui.internal.TraceConstants;

