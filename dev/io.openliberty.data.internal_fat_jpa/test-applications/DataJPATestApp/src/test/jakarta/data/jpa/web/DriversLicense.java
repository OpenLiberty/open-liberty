/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.time.LocalDate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

/**
 * Entity that has one-to-one mapping with the Driver entity.
 */
@Entity
public class DriversLicense {

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "license")
    public Driver driver;

    @Column
    public LocalDate expiresOn;

    @Column
    public LocalDate issuedOn;

    @Id
    public String licenseNum;

    @Column
    public String stateName;

    public DriversLicense() {
    }

    @Override
    public String toString() {
        return stateName + " license #" + licenseNum + " for " + (driver == null ? null : driver.fullName) +
               " valid from " + issuedOn + " to " + expiresOn;
    }
}
