/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity;

import com.ibm.websphere.simplicity.log.Log;

/**
 * 
 * @author SterlingBates
 * 
 */
public class Workspace {

    private static Class c = Workspace.class;

    private final Cell cell;
    private final String[] workspaceChanges;

    protected Workspace(Cell cell) throws Exception {
        Log.entering(c, "constructor");
        this.cell = cell;
        this.workspaceChanges = new String[0];
        Log.exiting(c, "constructor");
    }

    public void save() throws Exception {
        Log.entering(c, "save", this);
        Log.exiting(c, "save", this.workspaceChanges);
    }

    /**
     * This method saves the configuration and syncs the nodes in an ND topology. If the topology is
     * not {@link WebSphereTopologyType#ND}, this method will ONLY save and will not attempt to
     * sync any nodes.
     * 
     * @throws Exception
     */
    public void saveAndSync() throws Exception {
        save();
    }

    public void discard() throws Exception {
        Log.entering(c, "discard");
        Log.exiting(c, "discard");
    }

    public Cell getCell() {
        return this.cell;
    }

}
