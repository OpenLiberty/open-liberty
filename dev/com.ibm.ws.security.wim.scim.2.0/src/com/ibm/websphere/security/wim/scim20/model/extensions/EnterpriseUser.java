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

package com.ibm.websphere.security.wim.scim20.model.extensions;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ibm.ws.security.wim.scim20.model.extensions.EnterpriseUserImpl;

@JsonDeserialize(as = EnterpriseUserImpl.class)
public interface EnterpriseUser {
    public String getCostCenter();

    public String getDepartment();

    public String getDivision();

    public String getEmployeeNumber();

    public EnterpriseManager getManager();

    public String getOrganization();

    public void setCostCenter(String costCenter);

    public void setDepartment(String department);

    public void setDivision(String division);

    public void setEmployeeNumber(String employeeNumber);

    public void setManager(EnterpriseManager manager);

    public void setOrganization(String organization);
}
