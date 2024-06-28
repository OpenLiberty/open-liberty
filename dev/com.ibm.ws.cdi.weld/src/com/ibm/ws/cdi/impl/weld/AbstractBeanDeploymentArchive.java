package com.ibm.ws.cdi.impl.weld;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;

/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
//this class exists to be extended by unit tests
public abstract class AbstractBeanDeploymentArchive implements WebSphereBeanDeploymentArchive {

    //use the real impl class because it will look nicer in the logs.
    protected static final TraceComponent tc = Tr.register(BeanDeploymentArchiveImpl.class);

    protected boolean scanned = false;
    protected boolean visited = false;

    @Override
    @Trivial
    public boolean hasBeenVisited() {
        return visited;
    }

    @Override
    public Iterator<WebSphereBeanDeploymentArchive> visit() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "visit");
        }
        visited = true;

        Set<WebSphereBeanDeploymentArchive> accessibleBDAs = getDescendetBDAs();

        Iterator<WebSphereBeanDeploymentArchive> toReturn = null;
        //Runtime Extensions can see everything so they break even our rough dependency graph.
        //Return no children so they get scanned immediately.
        if (getArchive().getType() != ArchiveType.RUNTIME_EXTENSION) {

            toReturn = accessibleBDAs.iterator();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, getHumanReadableName() + " visited, found these children: []");
            }
            toReturn = Collections.emptyListIterator();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            String children = getArchive().getType() != ArchiveType.RUNTIME_EXTENSION ? accessibleBDAs.toString() : "[]";
            Tr.exit(tc, getHumanReadableName() + " visited, found these children: " + children);
        }

        return toReturn;
    }

    @Override
    public abstract CDIArchive getArchive();

    protected abstract String getHumanReadableName();

    protected abstract Set<WebSphereBeanDeploymentArchive> getDescendetBDAs();

}
