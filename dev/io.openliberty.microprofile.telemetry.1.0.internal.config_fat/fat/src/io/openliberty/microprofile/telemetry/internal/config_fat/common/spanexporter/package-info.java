/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
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
 * Holds the test span exporter
 * <p>
 * We need this because the Span interface doesn't expose the attributes which are set on it, so we need to export any spans we want to test.
 */
package io.openliberty.microprofile.telemetry.internal.config_fat.common.spanexporter;