/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics;

/**
 *
 */
public class Util {
    public static boolean isGetMethod(String method) {
        return Constants.METHOD_GET.equals(method);
    }

    public static boolean isOptionsMethod(String method) {
        return Constants.METHOD_OPTIONS.equals(method);
    }
}
