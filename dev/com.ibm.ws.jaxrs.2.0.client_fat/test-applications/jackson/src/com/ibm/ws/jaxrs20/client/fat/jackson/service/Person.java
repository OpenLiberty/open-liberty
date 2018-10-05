/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.fat.jackson.service;

@SuppressWarnings("deprecation")
public class Person {

    private String name;
    private int age;
    private Manager m;
    private String random = "randomValue";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String randomProp() {
        return random;
    }

    public void setRandomProp(String s) {
        random = s;
    }

    public Manager getManager() {
        return m;
    }

    public void setManager(Manager m) {
        this.m = m;
    }
}
