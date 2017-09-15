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
package com.ibm.ws.sib.msgstore.gbs;

/**
 * An exception that is thrown during an optimistic search when the
 * search depth exceeds the maximum.
 *
 * <p>This is not necessarily an error condition.  During an optimistic
 * search the tree is not latched and could be changing.  Modifications
 * made to the tree during an optimistic search could result in:</p>
 *
 *<ol>
 * <li>An incorrect search result,</li>
 * <li>A null pointer exception, or</li>
 * <li>A circular link leading to an infinite search path.</li>
 *</ol>
 *
 * <p>The first case is handled by re-doing the search if the the tree
 * was modified during the search.  The latter two cases have to be
 * handled by catching the exception, making sure that the tree did, in
 * fact change, and then redoing the search pessimistically.  This
 * exception is thrown after a reasonable number of tree levels have
 * been traversed without finding a leaf.  The assumption is that a tree
 * modification has resulted in the observation of a circular
 * structure.</p>
 *
 * @author Stewart L. Palmer
 */

public class OptimisticDepthException extends RuntimeException
{
    private static final long serialVersionUID = -5648212203609181354L;

    /**
     * Construct
     */
    OptimisticDepthException()
    {
        super();
    }

    /**
     * Construct with an error message
     */
    OptimisticDepthException(String msg)
    {
        super(msg);
    }
}
