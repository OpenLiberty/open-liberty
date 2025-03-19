/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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
 * Handles validation of OpenAPI v3.0 documents.
 * <p>
 * {@link OASValidator30Impl} provides the {@link OASValidator} service for validating OpenAPI v3.0 documents.
 */
@Version(Constants.OSGI_VERSION)
@TraceOptions(traceGroup = Constants.TRACE_GROUP, messageBundle = Constants.TRACE_VALIDATION)
package io.openliberty.microprofile.openapi20.internal.validation;

import org.osgi.annotation.versioning.Version;

import com.ibm.websphere.ras.annotation.TraceOptions;

import io.openliberty.microprofile.openapi20.internal.utils.Constants;
