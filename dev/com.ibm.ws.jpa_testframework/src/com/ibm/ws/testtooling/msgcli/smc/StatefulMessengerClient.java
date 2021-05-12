/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testtooling.msgcli.smc;

import com.ibm.ws.testtooling.msgcli.MessagingClient;
import com.ibm.ws.testtooling.msgcli.MessagingException;
import com.ibm.ws.testtooling.msgcli.jms.JMSClientConfig;

public interface StatefulMessengerClient extends MessagingClient {
    public void initialize(String clientName, JMSClientConfig fullDuplexConfig) throws MessagingException;

    public void initialize(String clientName, JMSClientConfig receiver, JMSClientConfig sender) throws MessagingException;

    @Override
    public void close();
}
