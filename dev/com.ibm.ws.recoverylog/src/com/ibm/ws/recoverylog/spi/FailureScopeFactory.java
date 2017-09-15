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

package com.ibm.ws.recoverylog.spi;

public interface FailureScopeFactory
{
    public static final Byte FILE_FAILURE_SCOPE_ID = new Byte((byte)1);
    public static final Byte SERVANT_FAILURE_SCOPE_ID = new Byte((byte)2);
    public static final Byte CONTROLLER_FAILURE_SCOPE_ID = new Byte ((byte)3);
    public static final Byte EPOCH_FAILURE_SCOPE_ID = new Byte ((byte)4);
        
    public FailureScope toFailureScope(byte[] bytes);
    public byte[] toByteArray(FailureScope failureScope);
}
