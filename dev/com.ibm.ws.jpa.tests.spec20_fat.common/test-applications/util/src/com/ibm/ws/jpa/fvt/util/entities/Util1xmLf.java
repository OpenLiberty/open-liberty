/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.util.entities;

import java.util.Collection;
import java.util.HashSet;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Version;

@Entity
public class Util1xmLf {

    private int id;

    private int version;

    private String firstName;

    public Collection<Util1xmRt> uniRight = new HashSet<Util1xmRt>();

    public Collection<Util1xmRt> uniRightEgr = new HashSet<Util1xmRt>();

    @Id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Version
    public int getVersion() {
        return version;
    }

    // Setter method needed by EclipseLink
    public void setVersion(int version) {
        this.version = version;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @OneToMany
    public Collection<Util1xmRt> getUniRight() {
        return uniRight;
    }

    public void setUniRight(Collection<Util1xmRt> uniRight) {
        this.uniRight = uniRight;
    }

    public void addUniRight(Util1xmRt uniRight) {
        this.uniRight.add(uniRight);
    }

    @OneToMany(fetch = FetchType.EAGER)
    public Collection<Util1xmRt> getUniRightEgr() {
        return uniRightEgr;
    }

    public void setUniRightEgr(Collection<Util1xmRt> uniRightEgr) {
        this.uniRightEgr = uniRightEgr;
    }

    public void addUniRightEgr(Util1xmRt uniRightEgr) {
        this.uniRightEgr.add(uniRightEgr);
    }

    @Override
    public String toString() {
        return "Util1xmLf[id=" + id + ",ver=" + version + "]";
    }
}
