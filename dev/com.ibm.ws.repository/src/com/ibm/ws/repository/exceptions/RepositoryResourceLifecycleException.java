/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.exceptions;

import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.common.enums.StateAction;

/**
 * An exception to indicate that a state transition attempted on a Massive
 * Asset was not a valid transition for an asset in that state ie between
 * draft and published without going through awwaiting_approval.
 *
 */
public class RepositoryResourceLifecycleException extends RepositoryResourceException {

    private static final long serialVersionUID = -7562518379718038340L;

    private final State oldState;
    private final StateAction action;

    public RepositoryResourceLifecycleException(String message, String resourceId, State oldState, StateAction action) {
        super(message, resourceId);
        this.oldState = oldState;
        this.action = action;
    }

    public RepositoryResourceLifecycleException(String message, String resourceId, State oldState, StateAction action, Throwable cause) {
        super(message, resourceId, cause);
        this.oldState = oldState;
        this.action = action;
    }

    public State getOldState() {
        return this.oldState;
    }

    public StateAction getAction() {
        return this.action;
    }
}
