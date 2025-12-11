/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.timeout.exception;

import io.openliberty.http.netty.timeout.TimeoutType;

/**
 * This exception should not be possible unless the TimeoutType enumeration
 * expands in the future. In which case, this will serve to fail fast
 * and help implement the missing type into the {@link TimeoutEventHandler}
 */
public class UnknownTimeoutException extends TimeoutException{

    private static final long serialVersionUID = 1L;

    public UnknownTimeoutException(TimeoutType type){
        super("Unsupported timeout type found: [" + type + "]", -3L, null);
        
    }

}
