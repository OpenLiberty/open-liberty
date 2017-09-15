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
package com.ibm.ejs.container.util;

/**
 * Override EJSPlatformHelper as provided by traditional WAS. z/OS on Liberty does not use
 * the split-process architecture.
 */
public class EJSPlatformHelper {
    public static boolean isZOS() {
        return false;
    }

    public static boolean isZOSCRA() {
        return false;
    }
}
