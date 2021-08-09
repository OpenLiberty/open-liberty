/*******************************************************************************
 * Copyright (c) 1995,2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.tsx.tag;



import java.util.Vector;

/**
 * Manage the indexes for Template Syntax
 */

public class DefinedIndexManager {

    protected Vector indexNames;
    protected int lastIndexIndex, i;

    public DefinedIndexManager() {
        indexNames = new Vector();
        lastIndexIndex = 0;
    }
    // add a user defined index to the vector
    public void addIndex(String newIndex) {
        indexNames.addElement(newIndex);
    }

    // begin 150288
    // remove a user defined index from the vector
    public void removeIndex(String oldIndex) {
        indexNames.removeElement(oldIndex);
    }
    // end 150288

    // see if the passed in index exists in the vector
    public boolean exists(String index) {
        boolean exists = false;

        if (indexNames.isEmpty() == false) {
            for (i = 0; i < indexNames.size(); i++) {
                if (index.equals((String) indexNames.elementAt(i))) {
                    exists = true;
                    break;
                }
            }
        }

        return exists;
    }
    // return an index that does not already exist in the vector
    // we default to tsx# as an index if the user doesn't specify one
    public String getNextIndex() {
        String newIndex;

        do {
            newIndex = "tsx" + String.valueOf(lastIndexIndex);
            lastIndexIndex++;
        }
        while (exists(newIndex) == true);

        addIndex(newIndex);

        return newIndex;
    }
}
