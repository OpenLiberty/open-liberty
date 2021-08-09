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

import java.io.Serializable;

public interface RecoveryWrapper extends Serializable
{
    public boolean isSameAs(RecoveryWrapper rw);
    
    public byte[] serialize();
    
    public PartnerLogData container(FailureScopeController failureScopeController);
}
