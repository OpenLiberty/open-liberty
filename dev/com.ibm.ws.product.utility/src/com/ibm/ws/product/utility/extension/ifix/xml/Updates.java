/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package com.ibm.ws.product.utility.extension.ifix.xml;

import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.NodeList;

public class Updates {

    public static Updates fromNodeList(NodeList nl) {
        Set<UpdatedFile> files = new HashSet<UpdatedFile>();

        for (int i = 0; i < nl.getLength(); i++) //Updates Elements
            for (int j = 0; j < nl.item(i).getChildNodes().getLength(); j++) //File Elements
                if (nl.item(i).getChildNodes().item(j).getNodeName().equals("file"))
                    files.add(UpdatedFile.fromNode(nl.item(i).getChildNodes().item(j)));

        return new Updates(files);
    }

    private final Set<UpdatedFile> files;

    public Updates(Set<UpdatedFile> files) {
        this.files = files;
    }

    /**
     * @return the files
     */
    public Set<UpdatedFile> getFiles() {
        return files;
    }
}
