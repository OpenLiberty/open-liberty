/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.common.impl;

import com.ibm.websphere.ras.annotation.Trivial;

/*
 * State details here are used only for managing the call and response
 * flows for the WS-AT protocol (since these one-way web services, where we
 * send a request and have to wait for an asynchronous response) - they
 * do not attempt to track the true transaction state. That is the business
 * of the transaction manager!
 */
@Trivial
public enum WSATParticipantState {
    ACTIVE, TIMEOUT,
    PREPARE, COMMIT, ROLLBACK,
    PREPARED, ABORTED, READONLY, COMMITTED
}
