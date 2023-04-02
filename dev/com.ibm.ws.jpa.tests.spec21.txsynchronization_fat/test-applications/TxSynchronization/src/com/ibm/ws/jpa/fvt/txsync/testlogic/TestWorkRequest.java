/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.txsync.testlogic;

import java.io.Serializable;

import javax.persistence.EntityManager;
import javax.transaction.UserTransaction;

/**
 * Interface for passing work requests off to Buddy EJBs.
 *
 */
public interface TestWorkRequest extends Serializable {
    public Serializable doTestWork(EntityManager em,
                                   UserTransaction tx,
                                   Object managedComponentObject);
}
