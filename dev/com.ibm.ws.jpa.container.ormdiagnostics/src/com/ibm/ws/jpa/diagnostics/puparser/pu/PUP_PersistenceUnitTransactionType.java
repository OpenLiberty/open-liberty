/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
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

package com.ibm.ws.jpa.diagnostics.puparser.pu;

public enum PUP_PersistenceUnitTransactionType {
    JTA,
    RESOURCE_LOCAL;

    public String value() {
        return name();
    }
    
    public static PUP_PersistenceUnitTransactionType fromValue(String v) {
        return valueOf(v);
    }
}
