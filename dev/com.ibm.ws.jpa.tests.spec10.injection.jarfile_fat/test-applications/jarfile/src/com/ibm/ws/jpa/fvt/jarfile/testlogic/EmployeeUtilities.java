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

package com.ibm.ws.jpa.fvt.jarfile.testlogic;

import javax.persistence.EntityManager;

import com.ibm.ws.jpa.commonentities.jpa10.employee.Department;
import com.ibm.ws.jpa.commonentities.jpa10.employee.Employee;
import com.ibm.ws.jpa.commonentities.jpa10.employee.Manager;

/**
 * Utility class for easy population of Employees and Departments.
 *
 */
public final class EmployeeUtilities {
    private static final int EMPLOYEEFIELDCOUNT = 8;
    private static final int DEPARTMENTFIELDCOUNT = 4;

    @SuppressWarnings("deprecation")
    private static final Object[] employeeData = new Object[] {
                                                                // ID, Last Name, First Name, Position, Birth Date, Manager (id ref), Department (id ref), IsManager [8 fields]
                                                                1, "Adams", "John", "Second President of the US", new java.sql.Date(1735, 10, 30), null, 1, true,
                                                                2, "Adams", "Samuel", "American Revolutionary", new java.sql.Date(1722, 9, 27), 39, 3, false,
                                                                3, "Adams", "John Quincy", "Sixth President of the US", new java.sql.Date(1767, 7, 11), null, 1, true,
                                                                4, "Adams", "Abigail Smith", "Wife of John Adams, second president of the United States",
                                                                new java.sql.Date(1765, 7, 14), 39, 3, false,
                                                                5, "Allen", "William", "Loyalist - former mayor of Philadelphia", new java.sql.Date(1704, 8, 5), 17, 2, false,
                                                                6, "Allen", "Ethan", "American Revolutionary War patriot, hero, and politician", new java.sql.Date(1738, 1, 21), 39,
                                                                3, false,
                                                                7, "Arnold", "Benedict", "American General who defected from the American to the British side",
                                                                new java.sql.Date(1741, 1, 14), 17, 2, false,
                                                                8, "Askin", "John", "Loyalist fur trader, merchant and official in Upper Canada", new java.sql.Date(1739, 1, 1), 17,
                                                                2, false,
                                                                9, "Bowles", "William Augustus", "Loyalist and Maryland-born English adventurer", new java.sql.Date(1763, 1, 1), 17,
                                                                2, false,
                                                                10, "Brant", "Joseph", "Mohawk leader and Loyalist during the American Revolution", new java.sql.Date(1743, 3, 1),
                                                                17, 2, false,
                                                                11, "Brown", "Thomas Burnfoot", "British Loyalist during the American Revolution", new java.sql.Date(1750, 5, 27),
                                                                17, 2, false,
                                                                12, "Cooper", "Myles", "Loyalist and President of King's College (Columbia University)",
                                                                new java.sql.Date(1735, 1, 1), 17, 2, false,
                                                                13, "Dickinson", "John", "American Statesman and member of the Pennsylvania Assembly",
                                                                new java.sql.Date(1732, 11, 13), 39, 3, false,
                                                                14, "Draper", "Mary", "Woman who helped soldiers during The American Revolution", new java.sql.Date(1, 1, 1), 39, 3,
                                                                false,
                                                                15, "Franklin", "Benjamin", "American Statesman", new java.sql.Date(1, 1, 1), 39, 3, false,
                                                                16, "Franklin", "William", "Loyalist, Govenor of New Jersey, Son of Ben Franklin", new java.sql.Date(1, 1, 1), 17,
                                                                2, false,
                                                                17, "Frederick", "George William", "King of Great Britain and King of Ireland during the American Revolution",
                                                                new java.sql.Date(1, 1, 1), null, 2, true,
                                                                18, "Galloway", "Joseph", "Loyalist and politician", new java.sql.Date(1, 1, 1), 17, 2, false,
                                                                19, "Girty", "Simon", "Loyalist and Liaison between Native Americans and Britain", new java.sql.Date(1, 1, 1), 17,
                                                                2, false,
                                                                20, "Greene", "Nathanael", "American General during the American Revolutionary War", new java.sql.Date(1, 1, 1), 39,
                                                                3, false,
                                                                21, "Hancock", "John", "American Politician, Entrepreneur, and Soldier", new java.sql.Date(1, 1, 1), 39, 3, false,
                                                                22, "Henry", "Patrick", "Founding father of American Revolutionary War and governor of Virginia",
                                                                new java.sql.Date(1, 1, 1), 39, 3, false,
                                                                23, "Howe", "John", "Loyalist printer during the American Revolution", new java.sql.Date(1, 1, 1), 17, 2, false,
                                                                24, "Hutchinson", "Thomas", "Last British royal governor of colonial Massachusetts", new java.sql.Date(1, 1, 1), 17,
                                                                2, false,
                                                                25, "Jay", "John", "The First Chief Justice of the United States", new java.sql.Date(1, 1, 1), 39, 3, false,
                                                                26, "Jefferson", "Thomas", "Third US President and Co-Author of the Declaration of Independence",
                                                                new java.sql.Date(1, 1, 1), null, 1, true,
                                                                27, "Jones", "John Paul", "Captain of the American Navy - 'I have not yet begun to fight'",
                                                                new java.sql.Date(1, 1, 1), 39, 3, false,
                                                                28, "Knox", "Henry", "First United States Secretary of War", new java.sql.Date(1, 1, 1), 39, 3, false,
                                                                29, "Lee", "Richard Henry", "American Statesman", new java.sql.Date(1, 1, 1), 39, 3, false,
                                                                30, "Lee", "Francis Lightfoot", "Active in Virginia politics and signer of the Declaration of Independence",
                                                                new java.sql.Date(1, 1, 1), 39, 3, false,
                                                                31, "Madison", "James", "Fourth President of the United States", new java.sql.Date(1, 1, 1), null, 1, true,
                                                                32, "Monroe", "James", "Fifth President of the United States", new java.sql.Date(1, 1, 1), null, 1, true,
                                                                33, "Moore", "Margaret Catharine", "Helped the colonists during the Battle of Cowpens.", new java.sql.Date(1, 1, 1),
                                                                39, 3, false,
                                                                34, "Paine", "Thomas", "Author of 'Common Sense' and Revolutionary", new java.sql.Date(1, 1, 1), 39, 3, false,
                                                                35, "Prescott", "William", "American colonel in the Revolutionary War", new java.sql.Date(1, 1, 1), 39, 3, false,
                                                                36, "Revere", "Paul", "American Activist and Artisan", new java.sql.Date(1, 1, 1), 39, 3, false,
                                                                37, "Rush", "Benjamin", "Signatory of the Declaration of Independence", new java.sql.Date(1, 1, 1), 39, 3, false,
                                                                38, "Warren", "Joseph", "Doctor, Soldier and Statesman of the American Revolution", new java.sql.Date(1, 1, 1), 39,
                                                                3, false,
                                                                39, "Washington", "George", "First President of the United States", new java.sql.Date(1, 1, 1), null, 1, true,
                                                                40, "Wheatley", "Phillis", "First published African author in America", new java.sql.Date(1, 1, 1), 39, 3, false,
                                                                41, "Wilson", "James", "Signer of the Declaration of Independence", new java.sql.Date(1, 1, 1), 39, 3, false,

                    // Data obtained from http://www.theamericanrevolution.org/people.aspx and Wikipedia
    };

    private static final Object[] departmentData = new Object[] {
                                                                  // ID, Department Name, Description, Manager (id ref) [4 fields]
                                                                  1, "Presidents Club", "Department of Presidents", 39,
                                                                  2, "Loyalists", "Loyalists of the British Crown", 17,
                                                                  3, "Revolutionaries", "US Revolutionaries", 39,
    };

    /*
     * Department Record Definition Access
     */
    public static int getDepartmentCount() {
        return (departmentData.length / DEPARTMENTFIELDCOUNT);
    }

    public static int getDepartmentID(int index) {
        return (Integer) departmentData[(index * DEPARTMENTFIELDCOUNT)];
    }

    public static String getDepartmentName(int index) {
        return (String) departmentData[(index * DEPARTMENTFIELDCOUNT) + 1];
    }

    public static String getDepartmentDescription(int index) {
        return (String) departmentData[(index * DEPARTMENTFIELDCOUNT) + 2];
    }

    public static int getDepartmentManager(int index) {
        return (Integer) departmentData[(index * DEPARTMENTFIELDCOUNT) + 3];
    }

    /*
     * Employee Record Definition Access
     */
    public static int getEmployeeCount() {
        return (employeeData.length / EMPLOYEEFIELDCOUNT);
    }

    public static int getEmployeeID(int index) {
        return (Integer) employeeData[(index * EMPLOYEEFIELDCOUNT)];
    }

    public static String getEmployeeLastName(int index) {
        return (String) employeeData[(index * EMPLOYEEFIELDCOUNT) + 1];
    }

    public static String getEmployeeFirstName(int index) {
        return (String) employeeData[(index * EMPLOYEEFIELDCOUNT) + 2];
    }

    public static String getEmployeePosition(int index) {
        return (String) employeeData[(index * EMPLOYEEFIELDCOUNT) + 3];
    }

    public static java.sql.Date getEmployeeBirthDate(int index) {
        return (java.sql.Date) employeeData[(index * EMPLOYEEFIELDCOUNT) + 4];
    }

    public static int getEmployeeManagerID(int index) {
        Object obj = employeeData[(index * EMPLOYEEFIELDCOUNT) + 5];
        return (Integer) ((obj == null) ? 0 : obj);
    }

    public static int getEmployeeDepartmentID(int index) {
        return (Integer) employeeData[(index * EMPLOYEEFIELDCOUNT) + 6];
    }

    public static boolean getEmployeeIsManager(int index) {
        return (Boolean) employeeData[(index * EMPLOYEEFIELDCOUNT) + 7];
    }

    public static void populateEmployeeDatabase(EntityManager em) {
        System.out.println("Populating Employee & Department entities...");

        int empCount = getEmployeeCount();
        int deptCount = getDepartmentCount();

        // Create Employee Entities
        for (int index = 0; index < empCount; index++) {
            Employee emp = getEmployeeIsManager(index) ? new Manager() : new Employee();

            emp.setId(getEmployeeID(index));
            emp.setFirstName(getEmployeeFirstName(index));
            emp.setLastName(getEmployeeLastName(index));
            emp.setPosition(getEmployeePosition(index));
            emp.setBirthdate(getEmployeeBirthDate(index));

            em.persist(emp);
        }

        // Create Departments
        for (int index = 0; index < deptCount; index++) {
            Department dept = new Department();

            dept.setId(getDepartmentID(index));
            dept.setDepartmentName(getDepartmentName(index));
            dept.setDescription(getDepartmentDescription(index));
            dept.setDepartmentManager(em.find(Manager.class, getDepartmentManager(index)));

            em.persist(dept);
        }

        // Link all employees to their departments & managers
        for (int index = 0; index < empCount; index++) {
            int id = getEmployeeID(index);
            int deptId = getEmployeeDepartmentID(index);
            int mgrId = getEmployeeManagerID(index);

            Employee emp = em.find(Employee.class, id);
            if (emp != null) {
                if (deptId != 0) {
                    Department dept = em.find(Department.class, deptId);
                    emp.setDepartment(dept);
                }

                if (mgrId != 0) {
                    Manager mgr = em.find(Manager.class, mgrId);
                    emp.setManager(mgr);
                }

            } else {
                System.out.println("**WARNING: em.find() returned null for id=" + id);
            }
        }

        System.out.println("Population complete (" + empCount + " new employee entries and " + deptCount + " dept entries.)");
    }

    public static void main(String[] args) {
        System.out.println("Test Data Integrity");

        int employeeCount = getEmployeeCount();
        System.out.println("Employee Count: " + employeeCount + " (length = " + employeeData.length + ")");

        int departmentCount = getDepartmentCount();
        System.out.println("Department Count: " + departmentCount);

        for (int i = 0; i < employeeCount; i++) {
            System.out.println(i + 1 + ") " + getEmployeeLastName(i) + ", " + getEmployeeFirstName(i) +
                               ", " + getEmployeePosition(i) + " MgrId=" + getEmployeeManagerID(i));
        }

    }
}
