package com.ibm.ws.Transaction.JTA;
/*******************************************************************************
 * Copyright (c) 1999, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import javax.transaction.xa.Xid;

public interface JTAXAResource extends StatefulResource
{
    /**
     * return XID associated with this JTAXAResource object.
     */
    public Xid getXID();
    
    /**
     * return recoveryId associated with this JTAXAResource object.
     */
    public long getRecoveryId();
    
}
