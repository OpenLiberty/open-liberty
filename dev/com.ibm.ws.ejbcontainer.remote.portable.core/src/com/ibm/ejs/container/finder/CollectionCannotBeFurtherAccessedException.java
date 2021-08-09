/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.finder;

public class CollectionCannotBeFurtherAccessedException
                extends RuntimeException
{
    private static final long serialVersionUID = -2238613038201337470L;

    /**
     * Constructs a <code>CollectionCannotBeFurtherAccessedException</code> with <tt>null</tt>
     * as its error message string.
     */
    public CollectionCannotBeFurtherAccessedException() {
        super();
    }

    /**
     * Constructs a <code>CollectionCannotBeFurtherAccessedException</code>, saving a reference
     * to the error message string <tt>s</tt> for later retrieval by the
     * <tt>getMessage</tt> method.
     * 
     * @param s the detail message.
     */
    public CollectionCannotBeFurtherAccessedException(String s) {
        super(s);
    }
}
