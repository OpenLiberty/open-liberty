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

/**
 * Any channel factory implementation that limits its chain to only be used
 * by client requests from the local machine must implement this
 * interface. Note that it is intentionally empty. No methods are necessary.
 * The logic of the channel framework does a Class.isAssignableFrom on each
 * channel
 * factory in a chain to determine if the chain is local only. For example,
 * the in process channel factory must implement this interface.
 */
public interface LocalChannelFactory extends ChannelFactory {
    // nothing additional
}
