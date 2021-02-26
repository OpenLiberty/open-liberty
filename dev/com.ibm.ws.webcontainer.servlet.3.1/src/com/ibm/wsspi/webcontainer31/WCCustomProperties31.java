/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer31;

import com.ibm.ws.webcontainer.WebContainer;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.webcontainer.WCCustomProperties;

/**
 * A new class which houses the servlet 3.1 custom properties
 */
public class WCCustomProperties31 extends WCCustomProperties {
    
    static {
        setCustomPropertyVariables(); //initializes all the variables
    }
    
    public static int UPGRADE_READ_TIMEOUT; //The timeout to use when the request has been upgraded and a read is happening
    public static int UPGRADE_WRITE_TIMEOUT; //The timeout to use when the request has been upgraded and a write is happening
    
    public static void setCustomPropertyVariables() {
        
        UPGRADE_READ_TIMEOUT = Integer.valueOf(customProps.getProperty("upgradereadtimeout", Integer.toString(TCPReadRequestContext.NO_TIMEOUT))).intValue();
        UPGRADE_WRITE_TIMEOUT = Integer.valueOf(customProps.getProperty("upgradewritetimeout", Integer.toString(TCPReadRequestContext.NO_TIMEOUT))).intValue();
    }
}
