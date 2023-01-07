/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
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
package com.ibm.ws.channel.ssl.internal;

import java.io.IOException;

import javax.net.ssl.SSLEngineResult;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

/**
 * This callback will be used when calling the SSL utils code to do an asynchronous
 * handshake. When it is complete or an error occurs, this callback will be called
 * to take the next step.
 */
public interface SSLHandshakeCompletedCallback {

    /**
     * Called when the handshake is completed with the input result.
     *
     * @param sslResult
     */
    void complete(SSLEngineResult sslResult);

    /**
     * Called when the handshake fails with the input exception.
     *
     * @param ioe
     */
    void error(IOException ioe);

    /**
     * Called to let code inform the callback that netBuffer was replaced
     */
    void updateNetBuffer(WsByteBuffer newBuffer);

    /**
     * Allow code using the callback to check if netBuffer was replaced
     */
    WsByteBuffer getUpdatedNetBuffer();

}
