/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpHeaders;

/**
 * Custom class for writing data frame to specific streams
 */
public class LastStreamSpecificHttpContent extends DefaultLastHttpContent {
	
	int streamId = -1;
	
	public LastStreamSpecificHttpContent(int streamId) {
		super();
		this.streamId = streamId;
	}
    
	public LastStreamSpecificHttpContent(int streamId, ByteBuf content) {
		super(content);
		this.streamId = streamId;
	}
	
	public LastStreamSpecificHttpContent(int streamId, HttpHeaders trailingHeaders) {
		super(Unpooled.buffer(0), trailingHeaders);
		this.streamId = streamId;
	}
	
}
