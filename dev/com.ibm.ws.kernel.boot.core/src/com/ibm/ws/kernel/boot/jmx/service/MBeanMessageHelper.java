/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.jmx.service;

import java.text.MessageFormat;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;

/**
 * Helper for accessing generic messages for mbeans
 */
public class MBeanMessageHelper {

    /**
     * Provides a message for when the mbean is unable to perform an operation. Takes an Object[]
     * which should contain 4 strings:<br>
     * {0} server name<br>
     * {1} host name<br>
     * {2} logs location<br>
     * {3} unique request id<br>
     * 
     * @param inserts Substitution text for the message
     * @return Formated "mbean is unable to perform an operation" string
     */
    public static String getUnableToPerformOperationMessage(String libertyServerName, String hostName, String logsLocation, String requestId) {
        Object[] inserts = { libertyServerName, hostName, logsLocation, requestId };
        String msg = BootstrapConstants.messages.getString("error.mbean.operation.failure");
        return inserts == null || inserts.length == 0 ? msg : MessageFormat.format(msg, inserts);

    }

}
