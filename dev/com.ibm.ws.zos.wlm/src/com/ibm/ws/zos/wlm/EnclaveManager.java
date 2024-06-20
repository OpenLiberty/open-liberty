/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.wlm;

/**
 * WLM Enclave management
 */
public interface EnclaveManager {

    /**
     * Create a classification based on the specified transaction class.
     */
    ClassificationInfo createClassificationInfo(String transactionClass);

    /**
     * Create an Enclave with information from the ClassificationInfo.
     */
    Enclave create(ClassificationInfo info, long arrivalTime);

    /**
     * Indicate an intent to Join an enclave. Calling this will prevent the enclave from being deleted.
     *
     * @param enclave The enclave you wish to join later
     */
    void preJoinEnclave(Enclave enclave);

    /**
     * Join the current thread to the specified enclave, if possible.
     */
    void joinEnclave(Enclave enclave) throws AlreadyClassifiedException;

    /**
     * Create and then Join an Enclave to the current thread.
     *
     * @param transactionClass The transaction class (in EBCDIC)
     * @param arrivalTime      The arrival time (STCK)
     * @return the created enclave
     */
    Enclave joinNewEnclave(byte[] transactionClass, long arrivalTime);

    /**
     * An alternate signature for joinNewEnclave that takes the transaction class as a Java String
     *
     * @param tclass      The Transaction Class
     * @param arrivalTime The work Arrival Time (STCK)
     * @return the created enclave
     */
    Enclave joinNewEnclave(String tclass, long arrivalTime);

    /**
     * If the specified Enclave is associated with this thread, leave it.
     */
    byte[] leaveEnclave(Enclave enclave);

    /**
     * Removes the current enclave from the thread. Does not mess with the in-use counters at all.
     * You intend to eventually restore this enclave to this thread. You're just taking it off the thread
     * for a little bit to do something (probably temporarily put a different enclave on the thread)
     *
     * @return The enclave currently on the thread, void if no current enclave. You need to hang onto this
     *         because you will need it to call restoreEnclaveToThread.
     */
    Enclave removeCurrentEnclaveFromThread();

    /**
     * Joins an enclave to the thread. Does not mess with the in-use counters. This should be an enclave
     * that was previously removed from this thread by removeCurrentEnclaveFromThread.
     *
     * @param enclave
     */
    void restoreEnclaveToThread(Enclave enclave) throws AlreadyClassifiedException;

    /**
     * Is there an enclave joined to this thread?
     *
     * @return the enclave on the thread, if there is one
     */
    Enclave getCurrentEnclave();

    /**
     * Get a string token for an enclave
     *
     * @return a string token that represents the enclave
     */
    String getStringToken(Enclave enclave);

    /**
     * Get an enclave from the token
     *
     * @return an enclave
     */
    Enclave getEnclaveFromToken(String s);

    /**
     * Deletes the specified enclave.
     *
     * @param enclave The enclave instance to delete.
     * @param force   If true, the delete service is invoked. If false, the delete service is
     *                    invoked only if the enclave is not in use and has no pending associations.
     */
    void deleteEnclave(Enclave enclave, boolean force);
}
