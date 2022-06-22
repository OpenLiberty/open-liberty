package com.ibm.tx.jta;
/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import javax.transaction.xa.XAResource;

public interface OnePhaseXAResource extends XAResource
{
    //
    //  Return the name of the OnePhaseXAResource
    //
    public String getResourceName();
}