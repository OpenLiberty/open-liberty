/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.bytebuffer.internal;

/**
 * This purpose of this interface is to consolidate Strings used throughout
 * the Channel Framework to prevent future changes from rippling to all
 * files.
 */
public interface WsByteBufferConstants {

    /** Trace group id used */
    String BASE_TRACE_NAME = "ChannelFramework"; //TODO
    /** Resource bundle used */
    String BASE_BUNDLE = "com.ibm.ws.channelfw.internal.resources.ChannelfwMessages"; //TODO

}
