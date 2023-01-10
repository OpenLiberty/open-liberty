/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
package com.ibm.ws.logging.osgi;

/**
 * Interface for passing config updates through the listener service to the msg router configurator.
 */
public interface MessageRouterConfigListener {

    /**
     * Pass updated list of message IDs associated with a log handler to the msg router configurator.
     * 
     * @param msgIds
     * @param handlerId
     */
    void updateMessageListForHandler(String msgIds, String handlerId);

}
