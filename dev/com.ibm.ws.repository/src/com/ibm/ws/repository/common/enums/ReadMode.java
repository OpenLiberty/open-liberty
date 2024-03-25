/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.repository.common.enums;

/**
 * The mode to use for reading from a directory-based repository
 * <p>
 * In production, {@code ASSUME_UNCHANGED} should usually be used
 */
public enum ReadMode {

    /**
     * Assume that the repository contents does not change during the lifetime of the connection
     * <p>
     * Allows resources retrieved from the repository to be cached by the client to avoid re-reading and parsing the same data.
     */
    ASSUME_UNCHANGED,

    /**
     * Detect if the repository contents changes between requests and always return up to date data
     * <p>
     * Resources cannot be cached by the client
     */
    DETECT_CHANGES

}
