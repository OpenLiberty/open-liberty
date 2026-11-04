/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola;

import java.util.concurrent.Future;

/**
 * This class registers and deregisters InterruptObject instances for specific WOLA
 * requests. The implementation of this class must be capable of providing
 * an interrupt object for an OTMA C/I request, or for a regular WOLA request
 * using a WOLA registration.
 *
 * Callers will use one of the register methods to register the InterruptObject, perform
 * the WOLA request, and then call the deregister method.
 */
public interface WolaInterruptObjectBridge {
    /**
     * Create an InterruptObject for an OTMA C/I request.
     *
     * @param anchor    The OTMA anchor that was used to obtain the session.
     * @param sessionId The OTMA C/I session that we're going to use to
     *                      send the request.
     * @param ecbPtr    The address of the ECB that needs to be posted
     *                      if we cancel the request.
     *
     * @return A token which is provided to deregister. If interrupt
     *         objects are not available, null is returned and deregister
     *         should not be called.
     */
    Object registerOtma(long anchor, long sessionId, int ecbPtr);

    /**
     * Create an InterruptObject for a WOLA getClientService call. This will break
     * out of the MVS PAUSE that occurs while the server is waiting for the client to
     * host a particular service. The PAUSE token is wrapped in a "waiter" object,
     * which will have a unique token.
     *
     * @param wolaGroupBytes    The WOLA group name, encoded in EBCDIC and filled
     *                              out to 8 bytes.
     * @param registerNameBytes The WOLA client registration name, encoded in
     *                              EBCDIC and filled out to 16 bytes.
     * @param waiterToken       The token used to look up the waiter in the queue.
     *
     * @return A token which is provided to deregister. If interrupt
     *         objects are not available, null is returned and deregister
     *         should not be called.
     */
    Object register(byte[] wolaGroupBytes, byte[] registerNameBytes, long waiterToken);

    /**
     * Create an InterruptObject for a WOLA request. This will break out of the
     * read() that is waiting for a response.
     *
     * @param responseFuture The future object that represents the server task
     *                           waiting on the response. The future will be cancelled if the
     *                           InterruptObject is interrupted.
     *
     * @return A token which is provided to deregister. If interrupt
     *         objects are not available, null is returned and deregister
     *         should not be called.
     */
    Object register(Future<?> responseFuture);

    /**
     * Remove an InterruptObject after the request is finished.
     *
     * @param token The token provided on the register method.
     */
    void deregister(Object token);
}
