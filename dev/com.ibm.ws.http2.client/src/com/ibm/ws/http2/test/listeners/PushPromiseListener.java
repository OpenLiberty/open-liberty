/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http2.test.listeners;

import com.ibm.ws.http.channel.h2internal.frames.FramePushPromise;

/**
 * Listener for push promise testing. When registered to an
 * Http2Client it will be called on each push promise frame
 * the client receives
 */
public interface PushPromiseListener {

    void onPushPromiseReceived(FramePushPromise receivedFrame);

}
