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

import com.ibm.tx.jta.impl.PartnerLogData;
import com.ibm.tx.jta.impl.RecoveryWrapper;
import com.ibm.tx.jta.impl.FailureScopeController;

public final class JCARecoveryWrapper implements RecoveryWrapper
{
    private final String _providerId;
    
    public JCARecoveryWrapper(String providerId)
    {
        _providerId = providerId;
    }

    // recovery constructor
    JCARecoveryWrapper(byte[] logData)
    {
        _providerId = new String(logData);
    }

    public String getProviderId()
    {
        return _providerId;
    }

    public String toString()
    {
        return _providerId;
    }

    public boolean isSameAs(RecoveryWrapper rw)
    {
        if (rw instanceof JCARecoveryWrapper)
        {
            if (_providerId != null)
            {
                return _providerId.equals(((JCARecoveryWrapper)rw).getProviderId());
            }

            return ((JCARecoveryWrapper)rw).getProviderId() == null;
        }

        return false;
    }

    public int hashCode()
    {
        if(_providerId == null)
        {
            return 0;
        }

        return _providerId.hashCode();
    }

    /**
     * @return
     */
    public byte[] serialize()
    {
        if(_providerId == null)
        {
            return new byte[0];
        }

        return _providerId.getBytes();
    }
    
    public PartnerLogData container(FailureScopeController failureScopeController)
    {
        return new JCARecoveryData(failureScopeController,this);
    }
}