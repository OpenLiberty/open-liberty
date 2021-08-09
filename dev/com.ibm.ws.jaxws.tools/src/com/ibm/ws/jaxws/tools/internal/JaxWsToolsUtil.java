/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.tools.internal;

/**
 * The general utility for com.ibm.ws.jaxws.tools projects.
 */
public class JaxWsToolsUtil {
    /**
     * get the localized message provided in the message files in the com.ibm.ws.jaxws.tools.
     * 
     * @param msgKey
     * @return
     */
    public static String formatMessage(String msgKey) {
        String msg;
        try {
            msg = JaxWsToolsConstants.messages.getString(msgKey);
        } catch (Exception ex) {
            // no FFDC required
            return msgKey;
        }

        return msg;
    }
}
