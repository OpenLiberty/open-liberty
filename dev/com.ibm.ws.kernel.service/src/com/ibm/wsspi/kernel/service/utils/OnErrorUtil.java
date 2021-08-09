/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

/**
 *
 */
public class OnErrorUtil {

    public final static String CFG_KEY_ON_ERROR = "onError";
    public final static String CFG_VALID_OPTIONS = "[IGNORE][WARN][FAIL]";

    public enum OnError {
        IGNORE, WARN, FAIL
    };

    public static OnError getDefaultOnError() {
        // Note: Metatype definitions should/must match this value, default="WARN"
        return OnError.WARN;
    }

    public static String getAttributeName() {
        return CFG_KEY_ON_ERROR;
    }
}
