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
package com.ibm.ws.jaxrs.fat.contextresolver;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DepartmentListWrapper {

    @XmlElement
    private final List<Department> departmentList = new LinkedList<Department>();

    public List<Department> getDepartmentList() {
        return departmentList;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Department dept : departmentList) {
            sb.append("ID: " + dept.getDepartmentId());
            sb.append("\n");
            sb.append("NAME: " + dept.getDepartmentName());
            sb.append("\n");
        }

        return sb.toString();
    }

}
