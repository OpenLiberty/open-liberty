/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.fat.rar.message;

import javax.jms.MessageListener;

/**
 * This class is for the listener for ActivationSpecCompMsgDestValidImpl
 */
public interface MessageListenerCMDValid extends MessageListener {
    /**
     * <p>Passes a String message to the listener.
     *
     * @param a String message
     */
    void onStringMessage(String message);
}