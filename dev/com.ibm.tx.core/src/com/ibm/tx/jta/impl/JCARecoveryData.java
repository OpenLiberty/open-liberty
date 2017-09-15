package com.ibm.tx.jta.impl;
/*******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

//
// JCARecoveryData is a specialization of PartnerLogData
//
// The log data object is an JCARecoveryWrapper and this class provides
// methods to support the use of this particular data type.
//
public final class JCARecoveryData extends PartnerLogData
{
    //
    // Ctor when called from registration of a JCA provider
    //
    public JCARecoveryData(FailureScopeController failureScopeController,JCARecoveryWrapper logData)
    {
        super(logData,failureScopeController);
        
        _serializedLogData = logData.serialize();
        _sectionId = TransactionImpl.JCAPROVIDER_SECTION;
    }

    //
    // Ctor when called from recovery of an JCA provider from the log
    //
    public JCARecoveryData(RecoveryManager recoveryManager,byte[] serializedLogData, long id)
    {
        super(serializedLogData, new JCARecoveryWrapper(serializedLogData), id, recoveryManager.getFailureScopeController().getPartnerLog());
        _recovered = true;
    }

    public JCARecoveryWrapper getWrapper()
    {
        return (JCARecoveryWrapper)_logData;
    }
}