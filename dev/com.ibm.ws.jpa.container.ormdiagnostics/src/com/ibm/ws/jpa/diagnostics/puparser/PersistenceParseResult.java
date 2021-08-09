/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.diagnostics.puparser;

import java.math.BigInteger;

import com.ibm.ws.jpa.diagnostics.puparser.pu.PUP_Persistence;

public final class PersistenceParseResult {
    private final BigInteger hash;
    private final PUP_Persistence persistence;
    
    public PersistenceParseResult(BigInteger hash, PUP_Persistence persistence) {
        if (hash == null || persistence == null) {
            throw new NullPointerException("Constructor cannot accept any null arguments.");
        }
        
        this.hash = hash;
        this.persistence = persistence;
    }

    public final BigInteger getHash() {
        return hash;
    }

    public final PUP_Persistence getPersistence() {
        return persistence;
    }

    @Override
    public String toString() {
        return "PersistenceDefinition [hash=" + hash + ", persistence=" + persistence + "]";
    }
}
