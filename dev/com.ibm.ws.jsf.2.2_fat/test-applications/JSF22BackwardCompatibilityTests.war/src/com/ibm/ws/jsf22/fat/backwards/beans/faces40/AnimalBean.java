/*
 * Copyright (c) 2015, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.ibm.ws.jsf22.fat.backwards.beans.faces40;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

/**
 * Simple bean to test the getType method from the ELResolver class
 */
@Named
@SessionScoped
public class AnimalBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public static abstract class Animal {
    }

    public static abstract class Dog extends Animal {
    }

    public static class Pitbull extends Dog {
    }

    private Pitbull dog = new Pitbull();

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

    /*
     * For testGetTypeFromCompositeComponentELResolver Expression Language 5.0 explicitly states that:
     * ELResolver.getType() must return null if either the ELResolver or the resolved
     * property is read-only. BeanELResolver is created with read-only as true; the following need setters to pass:
     *
     * untypedXwideEL="#{animalBean.animal}"
     * untypedXmediumEL="#{animalBean.dog}"
     * untypedXnarrowEL="#{animalBean.pitbull}"
     * untypedXnullEL="#{animalBean.lostDog}"
     */

    public void setAnimal(Animal dog) {
        this.dog = (Pitbull) dog;
    }

    public void setAnimal(Dog dog) {
        this.dog = (Pitbull) dog;
    }

    public void setAnimal(Pitbull dog) {
        this.dog = dog;
    }

    public void setDog(Dog dog) {
        this.dog = (Pitbull) dog;
    }

    public void setPitbull(Pitbull dog) {
        this.dog = dog;
    }

    public void setLostDog(Dog dog) {
        this.dog = (Pitbull) dog;
    }

}
