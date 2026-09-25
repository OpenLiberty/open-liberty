/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
 * Post-Quantum Cryptography (PQC) support for audit encryption and signing.
 * 
 * <p>This package provides ML-KEM (FIPS 203) and ML-DSA (FIPS 204) implementations
 * for quantum-resistant audit log protection.</p>
 * 
 * @version 1.0.0
 */
@org.osgi.annotation.versioning.Version("1.0.0")
@TraceOptions(traceGroup = "audit", messageBundle = "com.ibm.ws.security.audit.source.internal.resources.AuditMessages")
package com.ibm.ws.security.audit.pqc;

import com.ibm.websphere.ras.annotation.TraceOptions;

// Made with Bob
