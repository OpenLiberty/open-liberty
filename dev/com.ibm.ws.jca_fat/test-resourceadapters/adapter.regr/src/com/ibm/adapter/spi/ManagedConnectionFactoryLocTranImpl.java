/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.spi;

import java.io.Serializable;

import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.resource.spi.TransactionSupport;
import javax.resource.spi.ValidatingManagedConnectionFactory;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * Implementation class of ManagedConnectionFactory
 */
public class ManagedConnectionFactoryLocTranImpl extends ManagedConnectionFactoryImpl implements ManagedConnectionFactory, Serializable, ResourceAdapterAssociation, ValidatingManagedConnectionFactory, TransactionSupport {

    private static final TraceComponent tc = Tr.register(ManagedConnectionFactoryLocTranImpl.class);

    /**
     * Constructor.<p>
     */
    public ManagedConnectionFactoryLocTranImpl() {

        super();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "<init>");
        System.out.println("ManagedConnectionFactoryLocTranImpl constructor " + this);

    }

    @Override
    public TransactionSupportLevel getTransactionSupport() {
        Tr.entry(tc, "getTransactionSupport");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getTransactionSupport", TransactionSupportLevel.LocalTransaction);

        return TransactionSupportLevel.LocalTransaction;

    }

}
