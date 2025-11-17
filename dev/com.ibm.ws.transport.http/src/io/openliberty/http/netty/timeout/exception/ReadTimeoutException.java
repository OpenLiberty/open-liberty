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

import java.util.concurrent.TimeUnit;

public class ReadTimeoutException extends TimeoutException{

    private static final long   serialVersionUID = 1L;
    private static final String warningCode = "SOMECODE";  

    public ReadTimeoutException(long duration, TimeUnit unit){
        super(warningCode, duration, unit);
    }
}
