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
package com.ibm.ws.jpa.olgh19998.model;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Version;

@Entity
@NamedQuery(name = "updateActiveEcallAvailableFlag", query = "UPDATE SimpleEntityOLGH19998 s SET s.ecallAvailableFlag = :flag WHERE s.pk = :pk")
public class SimpleEntityOLGH19998 {

    @Id
    private String pk;

    private int ecallAvailableFlag;

    @Version
    @Column(name = "sys_update_timestamp")
    private Timestamp sysUpdateTimestamp;

    public SimpleEntityOLGH19998() {
    }

    public SimpleEntityOLGH19998(String pk, int ecallAvailableFlag) {
        this.pk = pk;
        this.ecallAvailableFlag = ecallAvailableFlag;
    }

    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    public int getEcallAvailableFlag() {
        return ecallAvailableFlag;
    }

    public void setEcallAvailableFlag(int ecallAvailableFlag) {
        this.ecallAvailableFlag = ecallAvailableFlag;
    }

    public Timestamp getSysUpdateTimestamp() {
        return this.sysUpdateTimestamp;
    }

    public void setSysUpdateTimestamp(Timestamp sysUpdateTimestamp) {
        this.sysUpdateTimestamp = sysUpdateTimestamp;
    }
}
