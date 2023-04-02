/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
package com.ibm.ws.tx.embeddable;

import javax.transaction.xa.XAResource;

import com.ibm.ws.Transaction.RecoverableXAResource;

/**
 *
 */
public class RecoverableXAResourceAccessor {
    public static boolean isRecoverableXAResource(XAResource resource) {
        return resource instanceof RecoverableXAResource;
    }

    public static int getXARecoveryToken(XAResource resource) {
        return ((RecoverableXAResource) resource).getXARecoveryToken();
    }
}
