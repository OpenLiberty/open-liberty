/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * This is the package that provides the trace that is used to
 * print messages to the log when the feature is started on liberty
 */
@TraceOptions(traceGroup = "ACME", messageBundle = "com.ibm.ws.security.acme.resources.AcmeMessages")
package com.ibm.ws.security.acme.internal;

import com.ibm.websphere.ras.annotation.TraceOptions;
