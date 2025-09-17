/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.timeout;

/**
 * Enumeration to identify the type of timeout being enforced.
 */
public enum TimeoutType {

    READ,   // Used for request read timeouts
    PERSIST,// Used for keep-alive persist read timeouts
    WRITE   // Used for write timeouts

}
