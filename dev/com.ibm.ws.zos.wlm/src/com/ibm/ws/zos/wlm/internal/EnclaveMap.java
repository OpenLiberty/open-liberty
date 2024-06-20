/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.wlm.internal;

import java.util.concurrent.ConcurrentHashMap;

import com.ibm.ws.zos.wlm.Enclave;

/**
 * A static holder for enclaves.
 */
public class EnclaveMap {

    /**
     * A holder for the enclaves
     */
    protected final static ConcurrentHashMap<String, Enclave> enclaves = new ConcurrentHashMap<String, Enclave>();

    /**
     * Put an enclave into the map
     *
     * @param e The enclave to remember
     * @return The token (extracted from the enclave) and used as a key
     */
    static public String rememberEnclave(Enclave e) {
        String s = ((EnclaveImpl) e).getStringToken();
        enclaves.put(s, e);
        return s;
    }

    /**
     * Get an enclave out of the map
     *
     * @param s The string returned by rememberEnclave
     * @return An enclave to go with the string, or null if no match is found
     */
    static public Enclave findEnclave(String s) throws EnclaveNotFoundException {
        Enclave e = enclaves.get(s);
        if (null == e) {
            throw new EnclaveNotFoundException();
        }
        return e;
    }

    /**
     * Remove an enclave from the map
     *
     * @param e The enclave to remove
     */
    static public void forgetEnclave(Enclave e) {
        String s = ((EnclaveImpl) e).getStringToken();
        enclaves.remove(s);
    }

}
