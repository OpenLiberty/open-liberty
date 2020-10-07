/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp23.fat.testel.beans;

import javax.el.ELProcessor;
import javax.el.LambdaExpression;

public class Employee {

    String firstname;
    String lastname;
    int age;
    String name;

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = getFirstname() + getLastname();
    }

    public Employee() {
        // Default constructor.
    }

    public Employee(String name) {
        this.setFirstname(name);
        this.setName(name);
    }

    public Employee(String name, String fn, String ln) {

        this.setName(name);
        this.setFirstname(fn);
        this.setLastname(ln);

    }

    public Employee(String name, String fn, String ln, int Age) {

        this.setName(name);
        this.setFirstname(fn);
        this.setLastname(ln);
        this.setAge(Age);

    }

    public String sanitizeNames(LambdaExpression expression) {

        System.out.println("expression: " + expression);
        ELProcessor elp = new ELProcessor();
        Boolean result = (Boolean) expression.invoke(this);
        System.out.println(" RESULT: " + result.toString());

        if (result) {
            return "NAME MATCHES: " + this.firstname;
        } else {
            return "NAME DOES NOT MATCH";
        }

    }

}
