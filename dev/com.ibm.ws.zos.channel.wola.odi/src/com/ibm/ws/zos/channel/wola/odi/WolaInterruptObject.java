/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.odi;

import com.ibm.websphere.interrupt.InterruptObject;

/**
 * Adds a cancel method to the InterruptObject to allow us to
 * invalidate a call to interrupt().
 */
public interface WolaInterruptObject extends InterruptObject {

    /**
     * Cancels this interrupt object. If interrupt() is called after
     * cancel is driven, interrupt() will be a no-op.
     */
    void cancel();

    /**
     * Gets the value of the cancelled flag.
     */
    boolean isCancelled();
}
