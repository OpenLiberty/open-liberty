/*******************************************************************************
 * Copyright (c) 1998, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

/**
 * <code>InsufficientCacheSpaceException</code> used to be thrown by
 * <code>Cache</code> during an <code>insert()</code> operation when
 * the cache was "full" _and_ the cache was unable to evict objects
 * (either because the <code>EvictionStrategy</code> did not supply
 * a victim, or all victims are currently pinned).
 * <p>
 * 
 * This is kept only for backwards compatibility with down-level servers.
 * It is not currently used by any current code.
 * 
 */

public class InsufficientCacheSpaceException
                extends CSIException
{
    private static final long serialVersionUID = -3751156269968731288L;
}
