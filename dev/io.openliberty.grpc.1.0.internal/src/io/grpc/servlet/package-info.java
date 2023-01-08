/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

@org.osgi.annotation.versioning.Version("1.0")
@TraceOptions(traceGroup = GrpcMessages.GRPC_TRACE_NAME, messageBundle = GrpcMessages.GRPC_BUNDLE)
package io.grpc.servlet;

import io.openliberty.grpc.internal.GrpcMessages;
import com.ibm.websphere.ras.annotation.TraceOptions;