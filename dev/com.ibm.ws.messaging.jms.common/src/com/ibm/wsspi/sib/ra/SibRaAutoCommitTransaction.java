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

package com.ibm.wsspi.sib.ra;

import com.ibm.wsspi.sib.core.SITransaction;

/**
 * Transaction object passed on core SPI methods to indicate that the work
 * should be performed immediately when a <code>FactoryType</code> of
 * <code>RA_CONNECTION</code> has been specified on the
 * <code>SICoreConnectionFactorySelector</code>. This is required because,
 * for this factory type, a transaction parameter of <code>null</code>
 * indicates that the current container transaction (if any) should be used.
 */
public class SibRaAutoCommitTransaction implements SITransaction {

    /**
     * Singleton instance of this class.
     */
    public static final SibRaAutoCommitTransaction AUTO_COMMIT_TRANSACTION = new SibRaAutoCommitTransaction();

    /**
     * Private construtor to prevent instantiation.
     */
    private SibRaAutoCommitTransaction() {

        // Do nothing

    }

}
