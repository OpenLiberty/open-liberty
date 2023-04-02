/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.http2.test.listeners;

import com.ibm.ws.http.channel.h2internal.frames.Frame;

/**
 *
 */
public interface FramesListener {
    public void receivedLastFrame(boolean sendGoAway);

    public void receivedFrameGoAway();

    void receivedSettingsAckFrame();

    void sentSettingsFrame();

    void receivedFrame(Frame receivedFrame);
}