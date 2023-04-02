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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Version;

@Entity
public class Util1x1Lf {

    private int id;

    private int version;

    private String firstName;

    public Util1x1Rt uniRight;

    public Util1x1Rt uniRightLzy;

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

    @OneToOne
    public Util1x1Rt getUniRight() {
        return uniRight;
    }

    public void setUniRight(Util1x1Rt uniRight) {
        this.uniRight = uniRight;
    }

    @OneToOne(fetch = FetchType.LAZY)
    public Util1x1Rt getUniRightLzy() {
        return uniRightLzy;
    }

    public void setUniRightLzy(Util1x1Rt uniRightLzy) {
        this.uniRightLzy = uniRightLzy;
    }

    @Override
    public String toString() {
        return "Util1x1Lf[id=" + id + ",ver=" + version + "]";
    }
}
