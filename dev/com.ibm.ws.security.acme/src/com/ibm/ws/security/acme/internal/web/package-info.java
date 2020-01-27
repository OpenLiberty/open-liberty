/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * This is the package that provides the trace that is used to print messages to
 * the log when the feature is started on liberty
 */
@TraceOptions(traceGroup = TRACE_GROUP, messageBundle = MESSAGE_BUNDLE)
package com.ibm.ws.security.acme.internal.web;

import com.ibm.websphere.ras.annotation.TraceOptions;
import static com.ibm.ws.security.acme.internal.TraceConstants.TRACE_GROUP;
import static com.ibm.ws.security.acme.internal.TraceConstants.MESSAGE_BUNDLE;
