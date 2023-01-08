/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.wsspi.ssl;

/**
 * Marker interface for a configured repertoire. Use this to allow one component
 * to track/require a configuration other than the default.
 */
public interface SSLConfiguration {
    /**
     * Returns the alias for this SSL configuration.
     * 
     * @return the alias for this SSL configuration.
     */
    String getAlias();
}
