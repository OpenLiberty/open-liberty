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
package com.ibm.wsspi.channelfw;

import com.ibm.ws.bytebuffer.internal.WsByteBufferPoolManagerImpl;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;

/**
 * Factory access for the channel framework. This must be a singleton object per
 * JVM; however, can be accessed from numerous types of callers.
 * 
 */
public class ChannelFrameworkFactory {

    /**
     * Access the channel framework instance for this JVM.
     * 
     * @return ChannelFramework
     */
    public static ChannelFramework getChannelFramework() {
        return ChannelFrameworkImpl.getRef();
    }

    /**
     * Access the WSByteBuffer pool manager instance.
     * 
     * @return WsByteBufferPoolManager
     */
    public static WsByteBufferPoolManager getBufferManager() {
        return WsByteBufferPoolManagerImpl.getRef();
    }
}
