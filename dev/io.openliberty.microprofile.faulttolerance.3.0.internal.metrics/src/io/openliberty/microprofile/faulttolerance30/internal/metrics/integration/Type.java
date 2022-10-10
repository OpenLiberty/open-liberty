/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.faulttolerance30.internal.metrics.integration;

/**
 * Enum of metric types which we use
 * <p>
 * The MetricType enum in the metrics API is removed from version 5.0 onwards
 */
public enum Type {
    COUNTER, GAUGE, HISTOGRAM
}
