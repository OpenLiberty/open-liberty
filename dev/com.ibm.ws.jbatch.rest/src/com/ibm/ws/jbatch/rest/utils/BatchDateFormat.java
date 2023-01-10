/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.jbatch.rest.utils;

import java.text.SimpleDateFormat;

/**
 * Simple convenience class wrapped around a ThreadLocal for obtaining a thread-specific
 * SimpleDateFormat for ser/deser dates in batch JSON data.
 * 
 * DateFormats are not thread safe; hence the ThreadLocal.
 */
public class BatchDateFormat {
    
    private static final ThreadLocal<SimpleDateFormat> threadLocal = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS Z");
        }
    };
    
    public static SimpleDateFormat get() {
        return threadLocal.get();
    }

}
