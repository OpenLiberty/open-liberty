/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.runtime;

import javax.servlet.jsp.PageContext;

public class PageContextPool {
    private PageContext pool[];
    private int index;

    public PageContextPool(int size) {
        pool = new PageContext[size];
        index = 0;
    }

    public boolean add(PageContext pageContext) {
        if (index < pool.length) {
            pool[index++] = pageContext;
            return true;
        }
        return false;
    }

    public PageContext remove() {
        if (index > 0) {
            PageContext pageContext = pool[--index];
            pool[index] = null;
            return pageContext;
        }
        return createPageContext();
    }

    protected PageContext createPageContext() {
        return null;
    }
}
