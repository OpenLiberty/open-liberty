/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threadContext;

public interface ThreadContext<T> {

    /**
     * beginContext returns the Object currently associated with the thread.
     * It associates the new Object passed parameter with the thread.
     */
    T beginContext(T object);

    /**
     * endContext disassociates the object currently associated with the thread
     * and returns it.
     */
    T endContext();

    /**
     * getContext returns the Object currently associated thread
     */
    T getContext();

    /**
     * getContext returns the index of the Object currently associated thread
     */
    int getContextIndex();

    /**
     * peekContext returns the topmost Object of which class is passed in.
     */
    T peekContext(Class clz);

}
