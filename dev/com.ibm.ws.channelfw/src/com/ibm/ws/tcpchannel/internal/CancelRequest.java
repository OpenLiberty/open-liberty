/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tcpchannel.internal;

import java.nio.channels.SelectionKey;

/**
 * Wrapper class for handling an attempt to cancel an outstanding
 * IO request.
 */
public class CancelRequest {
    static final int Reset = 0;
    static final int Ready_To_Cancel = 1;
    static final int Ready_To_Signal_Done = 2;

    /** Target key of the cancel attempt */
    protected SelectionKey key = null;
    /** Current state of the cancel attempt */
    protected int state = Reset;

    /**
     * Cosntructor.
     * 
     * @param targetKey
     */
    protected CancelRequest(SelectionKey targetKey) {
        if (null == targetKey) {
            throw new IllegalArgumentException("Null key");
        }
        this.key = targetKey;
        this.state = Ready_To_Cancel;
    }

}
