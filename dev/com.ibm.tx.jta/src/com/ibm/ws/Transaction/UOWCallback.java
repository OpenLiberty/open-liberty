/*******************************************************************************
 * Copyright (c) 2002, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.Transaction;

/**
 * This interface provides a callback method that can be used
 * to inform interested parties of a context change between 
 * Units of Work. The Units of Work in question will either 
 * be Local or Global transaction coordinators.
 *
 * <p> This interface is private to WAS.
 * Any use of this interface outside the WAS Express/ND codebase 
 * is not supported.
 *
 */
public interface UOWCallback
{
    /**
     * Change types for transaction context changes 
     * between Local and Global transactions.
     */
    static public final int PRE_BEGIN  = 0;
    static public final int POST_BEGIN = 1;
    static public final int PRE_END    = 2;
    static public final int POST_END   = 3;

    /**
     * 
     * @param typeOfChange One of the following values:
     * <PRE>
     * PRE_BEGIN
     * POST_BEGIN
     * PRE_END
     * POST_END
     * </PRE>
     * @param UOW The Unit of Work that will be affected by the begin/end i.e.
     * <PRE>
     * PRE_BEGIN  - NULL
     * POST_BEGIN - The UOW that was just begun
     * PRE_END    - The UOW to be ended
     * POST_END   - NULL
     * </PRE>
     * 
     * @exception IllegalStateException
     */
    public void contextChange(int typeOfChange, UOWCoordinator UOW) throws IllegalStateException;
}
