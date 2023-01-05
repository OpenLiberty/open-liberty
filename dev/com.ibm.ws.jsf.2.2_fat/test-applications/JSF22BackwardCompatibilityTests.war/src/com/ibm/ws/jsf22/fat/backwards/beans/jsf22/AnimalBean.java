/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.backwards.beans.jsf22;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 * Simple bean to test the getType method from the ELResolver class
 */
@ManagedBean
@SessionScoped
public class AnimalBean {
    public static abstract class Animal {}

    public static abstract class Dog extends Animal {}

    public static class Pitbull extends Dog {}

    private final Pitbull dog = new Pitbull();

    public Animal getAnimal() {
        return dog;
    }

    public Dog getDog() {
        return dog;
    }

    public Pitbull getPitbull() {
        return dog;
    }

    public Dog getLostDog() {
        return null;
    }
}