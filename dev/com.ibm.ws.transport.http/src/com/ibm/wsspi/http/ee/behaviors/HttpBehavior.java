/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.wsspi.http.ee.behaviors;

/**
 * Create a generic configure class for WebContainer/others to pass down any configuration
 */
public class HttpBehavior {

    /*
     * Since Servlet 6.0 (EE10):
     * Follows RFC 6265.
     * Attributes are no longer accepted from the request Cookie header (section 4.2.2)
     * $ is used only for $Versions in the request Cookie; prefix any other will be treated as new cookie ($ is part of a cookie name)
     */
    public static final String USE_EE10_COOKIES = "useEE10Cookies";
}
