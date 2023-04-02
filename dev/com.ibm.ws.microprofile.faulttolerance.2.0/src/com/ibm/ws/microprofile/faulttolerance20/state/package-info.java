/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
 * Includes interfaces for Fault Tolerance State objects, which each implement one of the fault tolerance policies.
 * <p>
 * Implementations for these interfaces are created using the methods on {@link com.ibm.ws.microprofile.faulttolerance20.state.FaultToleranceStateFactory}
 */
@TraceOptions(traceGroup = "FAULTTOLERANCE")
package com.ibm.ws.microprofile.faulttolerance20.state;

import com.ibm.websphere.ras.annotation.TraceOptions;
