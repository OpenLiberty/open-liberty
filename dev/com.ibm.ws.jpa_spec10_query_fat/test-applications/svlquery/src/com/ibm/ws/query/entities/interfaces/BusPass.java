/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.query.entities.interfaces;

import java.util.Date;

public class BusPass implements java.io.Serializable, Cloneable {
    public String owner;
    public Date expires;

    public BusPass() {
        super();
    }

    public BusPass(String owner, Date expires) {
        super();
        this.owner = owner;
        this.expires = expires;
    }

    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return getOwner() + ", " + getExpires();
    }

    public boolean equals(BusPass arg) {
        return arg.getOwner() == this.getOwner() && arg.getExpires() == this.getExpires();
    }

}
