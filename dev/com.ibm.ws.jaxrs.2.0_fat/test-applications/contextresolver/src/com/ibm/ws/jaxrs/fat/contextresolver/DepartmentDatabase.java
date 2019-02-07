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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DepartmentDatabase {

    private static Map<String, Department> departmentDB = new HashMap<String, Department>();

    public static Collection<Department> getDepartments() {
        return departmentDB.values();
    }

    public static void addDepartment(Department department) {
        departmentDB.put(department.getDepartmentId(), department);
    }

    public static Department getDepartment(String departmentId) {
        return departmentDB.get(departmentId);
    }

    public static Department removeDepartment(String departmentId) {
        return departmentDB.remove(departmentId);
    }

    public static void clearEntries() {
        departmentDB.clear();
    }

}
