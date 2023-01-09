/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package cdi.war;

import java.io.Serializable;

/**
 * Encapsulates an integer, and increments it every time you call {@link #getNext()}.
 */
public class Counter implements Serializable {

    private static final long serialVersionUID = 1L;

    private int counter = 0;

    public int getCounter() {
        return counter;
    }

    /**
     * Increments the counter value and returns the result
     * 
     * @return the next counter value
     */
    public int getNext() {
        return ++this.counter;
    }

}