/*******************************************************************************
 * Copyright (c) 1998, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

/**
 * Receives tx notifications from the Container. PM provides the implementation class.
 */
public interface TransactionListener {

    public void afterBegin();

    public void afterCompletion(int isCommit); // d160445

    public void beforeCompletion();

    /**
     * Set whether the LTC is being driven mid-ActivitySession
     * or the UOW of work is completing.
     * 
     * @param isCompleting is defined as follows:
     *            <UL>
     *            <LI>true - UOW is completing.</LI>
     *            <LI>false - ActivitySession is executing a mid session checkpoint/reset.</LI>
     *            </UL>
     */
    public void setCompleting(boolean isCompleting);

}