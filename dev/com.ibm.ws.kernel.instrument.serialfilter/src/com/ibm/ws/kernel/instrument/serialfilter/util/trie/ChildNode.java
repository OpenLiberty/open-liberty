/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.util.trie;

import java.util.Map;

final class ChildNode<T> extends Node<T> {
    private final String superstring;
    private final Node<T> parent;
    private final int depth;

    ChildNode(Node<T> parent, String s) {
        this.superstring = s;
        this.parent = parent;
        this.depth = parent.depth() + 1;
    }

    int depth() {
        return depth;
    }

    private char getChar() {
        return superstring.charAt(depth - 1);
    }

    @Override
    Node<T> getParent() {
        return parent;
    }

    @Override
    ChildNode<T> nextSibling() {
        Map.Entry<Character, ChildNode<T>> e = parent.children.higherEntry(getChar());
        return e == null ? null : e.getValue();
    }

    String getStringSoFar() {
        return superstring.substring(0, depth);
    }
}
