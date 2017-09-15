/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

public enum FrameTypes {
    NOT_SET,
    UNKNOWN,
    DATA, // 0x00
    HEADERS, // 0x01
    PRIORITY, // 0x02
    RST_STREAM, // 0x03
    SETTINGS, // 0x04
    PUSH_PROMISE, // 0x05
    PING, // 0x06
    GOAWAY, // 0x07
    WINDOW_UPDATE, // 0x08
    CONTINUATION // 0x09
}
