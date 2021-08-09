/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import com.ibm.ws.microprofile.faulttolerance20.state.TimeoutState;

public class TimeoutStateNullImpl implements TimeoutState {

    @Override
    public void start() {}

    @Override
    public void setTimeoutCallback(Runnable timeoutCallback) {}

    @Override
    public void stop() {}

    @Override
    public boolean isTimedOut() {
        return false;
    }

}
