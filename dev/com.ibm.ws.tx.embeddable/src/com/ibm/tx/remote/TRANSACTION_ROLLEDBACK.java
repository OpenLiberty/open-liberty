/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.remote;


/**
 *
 */
@SuppressWarnings("serial")
public class TRANSACTION_ROLLEDBACK extends RuntimeException {

    /**
     * @param string
     */
    public TRANSACTION_ROLLEDBACK(String string) {
        super(string);
    }

    /**
     * @param i
     * @param completedYes
     */
    public TRANSACTION_ROLLEDBACK(int i, Boolean completionStatus) {
        // TODO Auto-generated constructor stub
    }

}
