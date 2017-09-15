/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;


/**
 * An instance of a RLSSuspendToken represents a unique token returned when the Recovery Log Service
 * is called to suspend.   The token must be passed in during the corresponding call to resume the Recovery Log Service
 */
public interface RLSSuspendToken
{
   /**
    * Returns a byte array representation of the RLSSuspendToken. 
    */
    public byte[] toBytes();

    /**
     * Returns a printable String that identifies the RLSSuspendToken. This is
     * used for debug and servicability purposes.
     * 
     * @return A printable String that identifies the RLSSuspendToken.
     *
     */
    public String print();
}
