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

package com.ibm.ws.jpa.fvt.txsync.dd.web;

import javax.persistence.EntityManager;

import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

public class TxSyncTestDDServlet extends JPATestServlet {
    private EntityManager emCMTSTxSync;
    private EntityManager emCMTSTxUnsync;
}
