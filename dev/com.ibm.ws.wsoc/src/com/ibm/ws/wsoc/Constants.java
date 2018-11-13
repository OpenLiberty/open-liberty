/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

public class Constants {

    // Contants returned by the frame processing code, when asked to process a new buffer.
    // a non-negative return code is the position in the buffer where a new frame is assumed to start.
    // for this reason, these return codes are "int" and not an enumeration.
    // BP stands for Buffer Processing.
    public static final int BP_FRAME_ALREADY_COMPLETE = -3;
    public static final int BP_FRAME_IS_NOT_COMPLETE = -2;
    public static final int BP_FRAME_EXACTLY_COMPLETED = -1;

    public static final String HEADER_VALUE_UPGRADE = "Upgrade";
    public static final String HEADER_VALUE_WEBSOCKET = "websocket";
    public static final String HEADER_VALUE_FOR_SEC_WEBSOCKET_VERSION = "13";

    public static final String HEADER_NAME_HOST = "Host";
    public static final String HEADER_NAME_ORIGIN = "Origin";
    public static final String HEADER_NAME_UPGRADE = "Upgrade";
    public static final String HEADER_NAME_CONNECTION = "Connection";
    public static final String HEADER_NAME_SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";
    public static final String HEADER_NAME_SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";
    public static final String HEADER_NAME_SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
    public static final String HEADER_NAME_SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";

    public static final String MC_HEADER_NAME_SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";

    // Websocket Spec says max messsage size should default  to -1, which means unlimited.
    public static final long DEFAULT_MAX_MSG_SIZE = -1;

    // javadoc says -1 for annotation max message size attribute means "unlimited", and that is also the default.
    public static final long ANNOTATED_UNDEFINED_MAX_MSG_SIZE = -1;

    public static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public static final int MAX_PING_SIZE = 125;
}
