package com.ibm.ws.Transaction;
/*******************************************************************************
 * Copyright (c) 2002, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import javax.transaction.xa.XAResource;

/**
 *
 * This class is currently used to help JetStream implement XARecovery in an environment 
 * where the recovery information dynacially changes.  During transaction enlistment 
 * the JCA runtime or EJB container should detect that the XAResource implements 
 * this interface and use the recovery token obtained from getXARecoveryToken 
 * on the enlist call on WebSphereTransactionManager.  This method should be 
 * called prior to every enlistment.  The value should not be cached by the 
 * caller as the resource adapter may be connecting to different resource managers.  
 * The resource adapter will obtain this recovery id via the registerResourceInfo 
 * method on WebSphereTransactionManager and will provide its own implementation 
 * of the XAResourceInfo and XAResourceFactory interfaces.  
 * Note that the same mechanism will be used for both recovery of both inbound and 
 * outbound work i.e. the registerActivationSpec method will not be used.
 * 
 * <p> This interface is private to WAS.
 * Any use of this interface outside the WAS Express/ND codebase 
 * is not supported.
 *
 */
public interface RecoverableXAResource extends XAResource
{
    /**
     * @return int RecoveryToken (or RecoveryId) which is orginally obtained from the
     * transaction service when registering the recovery information.  This information
     * is expected to change over time so it should be retrieved each time before 
     * an enlist with the TM (i.e. don't cache it).
     */
    int getXARecoveryToken();
}