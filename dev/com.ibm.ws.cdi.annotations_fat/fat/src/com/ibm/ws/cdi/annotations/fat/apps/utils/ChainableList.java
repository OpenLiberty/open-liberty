/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.ws.cdi.annotations.fat.apps.utils;

import java.util.List;

/**
 * A list which extra methods for chaining method calls.
 */
public interface ChainableList<E> extends List<E> {

    /**
     * Adds an item to the list and returns the list. Useful for making code more concise. For example:
     * <pre> {@code
     * List<String> list = getList();
     * list.add(item);
     * return list;
     * } </pre>
     * <p>can be replaced by:
     * <p>{@code return list.chainAdd(item);}
     */
    ChainableList<E> chainAdd(E item);
}
