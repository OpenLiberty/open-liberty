/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.message;

import javax.jms.MessageListener;

/**
 * This class is for the listener for ActivationSpecCompMsgDestRequiredImpl
 */
public interface MessageListenerCMDRequired extends MessageListener {
    /**
     * <p>Passes a String message to the listener.
     *
     * @param a String message
     */
    void onStringMessage(String message);
}