/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.scim20.model.extensions;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.websphere.security.wim.scim20.model.extensions.EnterpriseManager;
import com.ibm.websphere.security.wim.scim20.model.extensions.EnterpriseUser;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "employeeNumber", "costCenter", "organization", "division", "department", "manager" })
public class EnterpriseUserImpl implements EnterpriseUser {

    public static final String SCHEMA_URN = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";

    @JsonProperty("costCenter")
    private String costCenter;

    @JsonProperty("department")
    private String department;

    @JsonProperty("division")
    private String division;

    @JsonProperty("employeeNumber")
    private String employeeNumber;

    @JsonProperty("manager")
    private EnterpriseManager manager;

    @JsonProperty("organization")
    private String organization;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        EnterpriseUserImpl other = (EnterpriseUserImpl) obj;
        if (costCenter == null) {
            if (other.costCenter != null) {
                return false;
            }
        } else if (!costCenter.equals(other.costCenter)) {
            return false;
        }
        if (department == null) {
            if (other.department != null) {
                return false;
            }
        } else if (!department.equals(other.department)) {
            return false;
        }
        if (division == null) {
            if (other.division != null) {
                return false;
            }
        } else if (!division.equals(other.division)) {
            return false;
        }
        if (employeeNumber == null) {
            if (other.employeeNumber != null) {
                return false;
            }
        } else if (!employeeNumber.equals(other.employeeNumber)) {
            return false;
        }
        if (manager == null) {
            if (other.manager != null) {
                return false;
            }
        } else if (!manager.equals(other.manager)) {
            return false;
        }
        if (organization == null) {
            if (other.organization != null) {
                return false;
            }
        } else if (!organization.equals(other.organization)) {
            return false;
        }
        return true;
    }

    @Override
    public String getCostCenter() {
        return costCenter;
    }

    @Override
    public String getDepartment() {
        return department;
    }

    @Override
    public String getDivision() {
        return division;
    }

    @Override
    public String getEmployeeNumber() {
        return employeeNumber;
    }

    @Override
    public EnterpriseManager getManager() {
        return manager;
    }

    @Override
    public String getOrganization() {
        return organization;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((costCenter == null) ? 0 : costCenter.hashCode());
        result = prime * result + ((department == null) ? 0 : department.hashCode());
        result = prime * result + ((division == null) ? 0 : division.hashCode());
        result = prime * result + ((employeeNumber == null) ? 0 : employeeNumber.hashCode());
        result = prime * result + ((manager == null) ? 0 : manager.hashCode());
        result = prime * result + ((organization == null) ? 0 : organization.hashCode());
        return result;
    }

    @Override
    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }

    @Override
    public void setDepartment(String department) {
        this.department = department;
    }

    @Override
    public void setDivision(String division) {
        this.division = division;
    }

    @Override
    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    @Override
    public void setManager(EnterpriseManager manager) {
        this.manager = manager;
    }

    @Override
    public void setOrganization(String organization) {
        this.organization = organization;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EnterpriseUserImpl [");
        if (costCenter != null) {
            builder.append("costCenter=");
            builder.append(costCenter);
            builder.append(", ");
        }
        if (department != null) {
            builder.append("department=");
            builder.append(department);
            builder.append(", ");
        }
        if (division != null) {
            builder.append("division=");
            builder.append(division);
            builder.append(", ");
        }
        if (employeeNumber != null) {
            builder.append("employeeNumber=");
            builder.append(employeeNumber);
            builder.append(", ");
        }
        if (manager != null) {
            builder.append("manager=");
            builder.append(manager);
            builder.append(", ");
        }
        if (organization != null) {
            builder.append("organization=");
            builder.append(organization);
        }
        builder.append("]");
        return builder.toString();
    }
}
