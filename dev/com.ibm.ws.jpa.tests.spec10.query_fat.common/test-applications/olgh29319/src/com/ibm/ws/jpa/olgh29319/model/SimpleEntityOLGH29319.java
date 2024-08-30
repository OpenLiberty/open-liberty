/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.jpa.olgh19342.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;

@Entity
@NamedQuery(name = "ECLIPSELINK_CASE_QUERY", query = "FROM Annuity WHERE annuityHolderId = :holderId")
public class SimpleEntityOLGH29319 {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "annuityHolderId")
    private String annuityHolderId;

    @Column(name = "amount")
    private double amount;

    // Getter and Setter for id
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    // Getter and Setter for annuityHolderId
    public String getAnnuityHolderId() {
        return annuityHolderId;
    }

    public void setAnnuityHolderId(String annuityHolderId) {
        this.annuityHolderId = annuityHolderId;
    }

    // Getter and Setter for amount
    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

}
