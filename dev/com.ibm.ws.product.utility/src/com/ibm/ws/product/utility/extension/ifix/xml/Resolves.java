/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.extension.ifix.xml;

import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Representation of the &lt;resolves&gt; XML element in an iFix XML file.
 */
public class Resolves {

    public static Resolves fromNodeList(NodeList nl) {
        //Only return the first resolves tag we find
        if (nl.getLength() > 0) {
            Node n = nl.item(0);

            return new Resolves(Problem.fromNodeList(n.getChildNodes()));
        }
        return null;
    }

    private final boolean showList = true;

    private final String description = "This fix resolves APARS:";

    private int problemCount;

    private final List<Problem> problems;

    /**
     * @return the problems in this resolves element or <code>null</code> if there aren't any
     */
    public List<Problem> getProblems() {
        return problems;
    }

    public Resolves(List<Problem> problems) {
        this.problems = problems;
        if (problems != null) {
            problemCount = problems.size();
        } else {
            problemCount = 0;
        }
    }

}
