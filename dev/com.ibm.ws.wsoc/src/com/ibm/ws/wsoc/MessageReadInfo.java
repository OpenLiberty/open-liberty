/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

/**
 *
 */
public class MessageReadInfo {

    public enum State {
        COMPLETE,
        PARTIAL_COMPLETE,
        FRAME_INCOMPLETE,
        CONTROL_MESSAGE_EMBEDDED,
        CLOSE_FRAME_ERROR
    }

    private final State state;
    private final OpcodeType type;

    private final boolean moreBuffer;;

    public MessageReadInfo(State s, OpcodeType t, boolean m) {
        state = s;
        type = t;
        moreBuffer = m;

    }

    public State getState() {
        return state;
    }

    public OpcodeType getType() {
        return type;
    }

    public boolean isMoreBufferToProcess() {
        return moreBuffer;
    }

}
