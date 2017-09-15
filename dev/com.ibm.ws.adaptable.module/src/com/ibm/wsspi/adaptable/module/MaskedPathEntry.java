/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.adaptable.module;

/**
 *
 */
public interface MaskedPathEntry {
    /**
     * Hides the adapted entry.<p>
     * Applies to both overlaid, and original Entries. <br>
     * Applies even if Entry is added via overlay after mask invocation.
     */
    public void mask();

    /**
     * UnHides the adapted entry previously hidden via 'mask'.
     * <br>
     * Has no effect if path is not masked.
     */
    public void unMask();

    /**
     * Query if the adapted entry is currently masked via 'mask'.
     * 
     * @return true if masked, false otherwise.
     */
    public boolean isMasked();
}
