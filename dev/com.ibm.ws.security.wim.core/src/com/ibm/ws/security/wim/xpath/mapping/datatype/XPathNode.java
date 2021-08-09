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
package com.ibm.ws.security.wim.xpath.mapping.datatype;

import java.util.HashMap;
import java.util.Iterator;

public interface XPathNode {

    /**
     * The node is a property node.
     */
    final static short NODE_PROPERTY = 0;
    /**
     * The node is a logical node.
     */
    final static short NODE_LOGICAL = 1;
    /**
     * The node is a parenthesis node.
     */
    final static short NODE_PARENTHESIS = 2;
    /**
     * The node is federation logical node.
     */
    final static short NODE_FED_LOGICAL = 4;
    /**
     * The node is federation parenthesis node.
     */
    final static short NODE_FED_PARENTHESIS = 8;

    //  void genSearchString(Object hint, StringBuffer searchBuffer);
    Iterator getPropertyNodes(HashMap propNodeMap);

    short getNodeType();
}
