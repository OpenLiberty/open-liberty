/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
/**
 * This is the package that provides the trace that is used to 
 * print messages to the log when the feature is started on liberty
 */
@TraceOptions(traceGroup = "Mail", messageBundle = "com.ibm.ws.javamail.resources.MailMessages")
package com.ibm.ws.javamail.internal;

import com.ibm.websphere.ras.annotation.TraceOptions;

