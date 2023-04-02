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
package com.ibm.ws.wsoc.outbound;

import java.io.IOException;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.ws.wsoc.ParametersOfInterest;
import com.ibm.wsspi.genericbnf.exception.MessageSentException;

public interface HttpRequestor {

    ClientTransportAccess getClientTransportAccess();

    void connect() throws Exception;

    void sendRequest() throws IOException, MessageSentException ;

    void sendRequest(ParametersOfInterest poi) throws IOException, MessageSentException;

    WsByteBuffer completeResponse() throws IOException;

    void closeConnection(IOException ioe);
}
