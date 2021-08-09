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
package com.ibm.ws.security.wim.xpath;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.wim.xpath.mapping.datatype.LogicalNode;
import com.ibm.ws.security.wim.xpath.mapping.datatype.XPathNode;

public class FederationLogicalNode extends LogicalNode {

    @Override
    @Trivial
    public short getNodeType() {
        return XPathNode.NODE_FED_LOGICAL;
    }
}
