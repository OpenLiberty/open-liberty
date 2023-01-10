/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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
package com.ibm.wsspi.webcontainer40;

import com.ibm.wsspi.webcontainer31.WCCustomProperties31;

/**
 *
 */
public class WCCustomProperties40 extends WCCustomProperties31 {

    static {
        setCustomPropertyVariables(); //initializes all the variables
    }

    public static String SERVER_REQUEST_ENCODING;
    public static String SERVER_RESPONSE_ENCODING;

    public static void setCustomPropertyVariables() {

        SERVER_REQUEST_ENCODING = customProps.getProperty("serverrequestencoding");
        SERVER_RESPONSE_ENCODING = customProps.getProperty("serverresponseencoding");

    }

}
