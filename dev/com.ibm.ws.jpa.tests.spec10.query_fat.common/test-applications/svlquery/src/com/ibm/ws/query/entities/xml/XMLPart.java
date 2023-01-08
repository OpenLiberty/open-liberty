/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

package com.ibm.ws.query.entities.xml;

import java.util.ArrayList;
import java.util.Collection;

import com.ibm.ws.query.entities.interfaces.IPart;
import com.ibm.ws.query.entities.interfaces.IUsage;

abstract public class XMLPart implements IPart {
    protected int partno;
    protected String name;

    protected Collection<XMLUsage> usedIn = new ArrayList();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getPartno() {
        return partno;
    }

    @Override
    public void setPartno(int partno) {
        this.partno = partno;
    }

    @Override
    public Collection<XMLUsage> getUsedIn() {
        return usedIn;
    }

    @Override
    public void setUsedIn(Collection<? extends IUsage> usedIn) {
        this.usedIn = (Collection<XMLUsage>) usedIn;
    }

    private boolean equals(XMLPart p) {
        boolean rc = false;
        return partno == p.partno;
    }

}
