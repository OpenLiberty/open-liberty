/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

import java.nio.ByteBuffer;

import javax.websocket.PongMessage;

/**
 *
 */
public class PongMessageImpl implements PongMessage {

    private ByteBuffer _content = null;

    /**
     * 
     */
    public PongMessageImpl(ByteBuffer content) {
        _content = content;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.websocket.PongMessage#getApplicationData()
     */
    @Override
    public ByteBuffer getApplicationData() {
        return _content;
    }
}
