package com.ibm.ws.objectManager;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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

/**
 * This is never thrown. It is simply used to record the fact that the padding
 * space available went negative. We do not expect that it should.
 * 
 */
public final class NegativePaddingSpaceException extends ObjectManagerException
{

    private static final long serialVersionUID = -4617412147654063010L;

    /**
     * 
     * @param Object creating the exception.
     * @param paddingSpaceAvailable
     */
    protected NegativePaddingSpaceException(Object source,
                                            long paddingSpaceAvailable)
    {
        super(source,
              NegativePaddingSpaceException.class,
              new Object[] { Long.valueOf(paddingSpaceAvailable) });
    }
}
