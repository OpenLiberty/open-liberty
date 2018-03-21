/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * Provides the classes necessary to implement a &quot;work-stealing&quot; threading
 * implementation.
 */
@TraceOptions(traceGroups = { "springboot", "applications", "app.manager" }, messageBundle = "com.ibm.ws.app.manager.springboot.internal.resources.Messages")
package com.ibm.ws.app.manager.springboot.internal;

import com.ibm.websphere.ras.annotation.TraceOptions;
