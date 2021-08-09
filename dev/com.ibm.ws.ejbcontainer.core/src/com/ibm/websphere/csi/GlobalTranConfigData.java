/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

/**
 * 
 * This interface defines methods to retrieve Global Tran attributes as defined
 * in the deployment XML.
 */
public interface GlobalTranConfigData
{
    /**
     * @return int The component transaction timeout value.
     */
    public int getTransactionTimeout();

    /**
     * @return boolean The isSendWSAT value.
     */
    public boolean isSendWSAT();
}
