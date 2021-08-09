/*******************************************************************************
 * Copyright (c) 2001, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter;

/**
 * This class enumerates the connection sharing constants used to enable
 * sharing based on the current state vs. the original connection request.
 * 
 * MatchOriginalRequest: Match all connection properties based on their originally requested values
 * 
 * conn1 = ds.getConnection(isolation=RR, catalog=jtest1)
 * conn1.setTransactionIsolation(RC)
 * conn1.setCatalog(jtest2)
 * // The following shares with the first connection because it matches the original request
 * conn2 = ds.getConnection(isolation=RR, catalog=jtest1)
 * 
 * MatchCurrentState: Matches all connection properties based on the current state.
 * 
 * conn1 = ds.getConnection(isolation=RR, readonly=true, typemap=())
 * conn1.setReadOnly(false)
 * conn2 = ds.getConnection(isolation=RR, readonly=true, typemap=()) // does not share with conn1
 * conn3 = ds.getConnection(isolation=RR, readonly=false, typemap=()) // shares with conn1
 * conn3.setReadOnly(true)
 * conn4 = ds.getConnection(isolation=RR, readonly=true, typemap=()) // shares with conn1
 */
public enum ConnectionSharing {
    MatchOriginalRequest,
    MatchCurrentState;
}
