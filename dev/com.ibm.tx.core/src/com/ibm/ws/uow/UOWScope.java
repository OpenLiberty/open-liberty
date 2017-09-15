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
package com.ibm.ws.uow;

/** 
 * A abstract representation of an object that is responsible for
 * scoping units of work namely an ActivitySession, local transaction,
 * or global transaction.
 */
public interface UOWScope
{
    public void setTaskId(String taskId);
    
    public String getTaskId();
}
