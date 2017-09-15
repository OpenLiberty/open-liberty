/*******************************************************************************
 * Copyright (c) 1997, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.runtime;

public class UnsynchronizedStack extends java.util.ArrayList {
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3257291318249140273L;

	public UnsynchronizedStack() {}
        
    public UnsynchronizedStack(int initialSize) {
    	super(initialSize);
    }
    
    public Object push(Object item) {
        add(item);
        return item;
    }

    public Object pop() {
        Object obj = null;
        int len = size();
        if (len > 0)
            obj = remove(len - 1);
        return obj;
    }
}

