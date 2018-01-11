/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.channel;

/**
 * Public constants shared outside of the HTTP channel itself.
 *
 * @ibm-private-in-use
 */
public interface HttpConstants {

    /** Key used in certain cases for session persistence */
    String SESSION_PERSISTENCE = "SessionPersistence";
    /** Key used on z/OS to mark the final outbound write of a message */
    String FINAL_WRITE_MARK = "HTTPFinalWrite";
    /** 342859 - Key used on z/OS to mark the initial read of a request message */
    String HTTPFirstRead = "HTTPFirstRead";
    /** 363633 - Store the read buffer size on z/OS for proxy use */
    String HTTPReadBufferSize = "zConfiguredHttpReadBufferSize";
    /** Key used on z/OS for an unlimited HTTP body size */
    String HTTPUnlimitedMessageMark = "UNLIMITED_HTTP_MESSAGE_SIZE";
    /** HTTP/2 MAGIC string */
    String HTTP2PrefaceString = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n";
}
