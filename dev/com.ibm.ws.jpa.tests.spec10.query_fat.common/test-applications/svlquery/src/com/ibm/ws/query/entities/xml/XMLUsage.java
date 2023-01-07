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

import com.ibm.ws.query.entities.interfaces.IPart;
import com.ibm.ws.query.entities.interfaces.IPartComposite;
import com.ibm.ws.query.entities.interfaces.IUsage;

public class XMLUsage implements IUsage {
    protected int id;
    protected int quantity;
    protected XMLPart child;
    protected XMLPartComposite parent;

    public XMLUsage(XMLPartComposite p, int quantity, XMLPart subpart) {
        this.id = p.getPartno() * 10000 + subpart.getPartno();
        this.quantity = quantity;
        setParent(p);
        p.getPartsUsed().add(this);
        setChild(subpart);
        subpart.getUsedIn().add(this);
    }

    public XMLUsage() {
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public XMLPartComposite getParent() {
        return parent;
    }

    @Override
    public void setParent(IPartComposite parent) {
        this.parent = (XMLPartComposite) parent;
    }

    @Override
    public int getQuantity() {
        return quantity;
    }

    @Override
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public XMLPart getChild() {
        return child;
    }

    @Override
    public void setChild(IPart child) {
        this.child = (XMLPart) child;
    }

    @Override
    public String toString() {
        return "Usage:" + id + " quantity:" + quantity + " child:" + child.getPartno() + " parent" + parent.getPartno();
    }

}
