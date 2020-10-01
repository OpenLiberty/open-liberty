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
package com.ibm.ws.testtooling.msgcli;

import com.ibm.ws.testtooling.testinfo.TestSessionSignature;

/**
 * Uni- or Bi-Directional messaging client interface.
 *
 */
public interface MessagingClient {
    /**
     * Returns the name associated with this MessagingClient.
     *
     * @return
     */
    public String clientIdentity();

    /**
     * Returns true if the client is configured to receive messages, false if it is not.
     * Will return false if close() has been invoked.
     *
     * @return
     */
    public boolean canReceiveMessages();

    /**
     * Returns true if the client is configured to transmit messages, false if it is not.
     * Will return false if close() has been invoked.
     *
     * @return
     */
    public boolean canTransmitMessages();

    /**
     * Transmits a packet to one or more receivers.
     *
     * @param packet
     */
    public void transmitPacket(DataPacket packet) throws MessagingException;

    /**
     * Receives a packet from a transmitter.
     *
     * @param timeout - maximum time to wait, in ms. A value of 0 means no wait. A value of -1 means
     *                    to wait indefinitely.
     *
     * @return An instance of TestDataPacket if one was received. Returns null if no message was received
     *         by the time the timeout expired.
     */
    public DataPacket receivePacket(long timeout) throws MessagingException;

    /**
     * Receives a packet from a transmitter, filtering by a specified TestSessionSignature.
     *
     * @param timeout        - maximum time to wait, in ms. A value of 0 means no wait. A value of -1 means
     *                           to wait indefinitely.
     *
     * @param testSessionSig - TestSessionSiguature to filter for.
     *
     * @return An instance of TestDataPacket if one was received. Returns null if no message was received
     *         by the time the timeout expired.
     */
    public DataPacket receivePacket(long timeout, TestSessionSignature testSessionSig) throws MessagingException;

    /**
     * Closes the MessagingClient. No further messages can be transmitted or received after this is invoked.
     *
     */
    public void close();
}