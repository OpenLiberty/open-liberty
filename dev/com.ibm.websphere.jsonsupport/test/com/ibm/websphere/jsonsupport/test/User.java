/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.jsonsupport.test;

public class User {

    public enum Role {
        Admin,
        Moderator,
        None
    }

    public float height;
    public int age;
    public double weight;
    public boolean isActive = false;
    protected String firstName;
    private String lastName;
    protected Role role;
    private String url;

    public String getLastName() {
        return lastName;
    }

    @SuppressWarnings("unused")
    private void setLastName(String value) {
        this.lastName = value;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String value) {
        firstName = value;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role r) {
        role = r;
    }

    public String getUPPER() {
        return url;
    }
}