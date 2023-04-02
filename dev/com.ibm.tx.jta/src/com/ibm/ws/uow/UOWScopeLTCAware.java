/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
package com.ibm.ws.uow;

/** 
 * A abstract representation of an object that is responsible for
 * storing information about LocalTransactions across its lifetime - namely an ActivitySession
 * or global transaction.
 */
public interface UOWScopeLTCAware extends UOWScope
{
    public void setCompletedLTCBoundary(Byte boundary);
    
    public Byte getCompletedLTCBoundary();
}
