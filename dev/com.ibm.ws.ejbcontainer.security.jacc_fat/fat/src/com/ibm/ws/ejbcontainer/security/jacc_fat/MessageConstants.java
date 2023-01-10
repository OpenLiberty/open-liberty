/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.security.jacc_fat;

/**
 *
 */
public class MessageConstants {

    // Values to be verified in messages
    public final static String EJB_ACCESS_EXCEPTION = "EJBAccessException:";
    //ORIGINAL
    public final static String AUTH_DENIED_METHOD_EXPLICITLY_EXCLUDED = "CWWKS9402A";
    public final static String AUTH_DENIED_USER_NOT_GRANTED_ACCESS_TO_ROLE = "CWWKS9400A";
    public final static String AUTH_DENIED_SYSTEM_IDENTITY_NOT_SUPPORTED = "CWWKS9405E";

    //JACC Information messages
    public final static String JACC_SERVICE_STARTING = "CWWKS2850I";
    public final static String JACC_SERVICE_STARTED = "CWWKS2851I";
    public final static String JACC_SERVICE_STOPPED = "CWWKS2852I";
    public final static String JACC_FAILED_TO_START = "CWWKS2853E";

    //JACC authorization messages
    public final static String JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE = "CWWKS9406A";

}
