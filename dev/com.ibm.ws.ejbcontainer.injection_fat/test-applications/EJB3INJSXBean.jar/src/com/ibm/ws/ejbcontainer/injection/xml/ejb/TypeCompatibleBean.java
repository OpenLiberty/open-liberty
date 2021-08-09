/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.xml.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Basic Stateless bean with interface that has a parent for subclass injection testing.
 **/
// @Stateful
// @Local( TypeCompatible.class )
public class TypeCompatibleBean {
    private String ivMethodOrFieldTypeInjected = null;

    private Animal ivAnimal;

    /**
     * Returns the field or method and type that was used to inject.
     **/
    public String getMethodOrFieldTypeInjected() {
        if (ivAnimal != null) {
            assertNull("---> multiple injections occurred : field-Animal",
                       ivMethodOrFieldTypeInjected);
            assertEquals("---> injected animal is not correct",
                         "Cat", ivAnimal.getName());
            ivMethodOrFieldTypeInjected = "field-Animal";
        }
        return ivMethodOrFieldTypeInjected;
    }

    @SuppressWarnings("unused")
    private void setObject(Object animal) {
        assertNull("---> multiple injections occurred : setObject-Object",
                   ivMethodOrFieldTypeInjected);
        assertNotNull("---> injected animal is null.", animal);
        assertEquals("---> injected animal is not correct",
                     "Cat", ((Animal) animal).getName());
        ivMethodOrFieldTypeInjected = "setObject-Object";
    }

    @SuppressWarnings("unused")
    private void setObject(Animal animal) {
        assertNull("---> multiple injections occurred : setObject-Animal",
                   ivMethodOrFieldTypeInjected);
        assertNotNull("---> injected animal is null.", animal);
        assertEquals("---> injected animal is not correct",
                     "Cat", animal.getName());
        ivMethodOrFieldTypeInjected = "setObject-Animal";
    }

    @SuppressWarnings("unused")
    private void setObject(Cat animal) {
        assertNull("---> multiple injections occurred : setObject-Cat",
                   ivMethodOrFieldTypeInjected);
        assertNotNull("---> injected animal is null.", animal);
        assertEquals("---> injected animal is not correct",
                     "Cat", animal.getName());
        ivMethodOrFieldTypeInjected = "setObject-Cat";
    }

    @SuppressWarnings("unused")
    private void setAnimal(Animal animal) {
        assertNull("---> multiple injections occurred : setAnimal-Animal",
                   ivMethodOrFieldTypeInjected);
        assertNotNull("---> injected animal is null.", animal);
        assertEquals("---> injected animal is not correct",
                     "Cat", animal.getName());
        ivMethodOrFieldTypeInjected = "setAnimal-Animal";
    }

    @SuppressWarnings("unused")
    private void setCat(Object animal) {
        assertNull("---> multiple injections occurred : setCat-Object",
                   ivMethodOrFieldTypeInjected);
        assertNotNull("---> injected animal is null.", animal);
        assertEquals("---> injected animal is not correct",
                     "Cat", ((Animal) animal).getName());
        ivMethodOrFieldTypeInjected = "setCat-Object";
    }

    @SuppressWarnings("unused")
    private void setCat(Animal animal) {
        assertNull("---> multiple injections occurred : setCat-Animal",
                   ivMethodOrFieldTypeInjected);
        assertNotNull("---> injected animal is null.", animal);
        assertEquals("---> injected animal is not correct",
                     "Cat", animal.getName());
        ivMethodOrFieldTypeInjected = "setCat-Animal";
    }

    @SuppressWarnings("unused")
    private void setPrimitive(Object integer) {
        assertNull("---> multiple injections occurred : setPrimitive-Object",
                   ivMethodOrFieldTypeInjected);
        int intValue = ((Integer) integer).intValue();
        assertEquals("---> injected integer is not correct",
                     5566, intValue);
        ivMethodOrFieldTypeInjected = "setPrimitive-Object";
    }

    @SuppressWarnings("unused")
    private void setPrimitive(Integer integer) {
        assertNull("---> multiple injections occurred : setPrimitive-Integer",
                   ivMethodOrFieldTypeInjected);
        int intValue = integer.intValue();
        assertEquals("---> injected integer is not correct",
                     5566, intValue);
        ivMethodOrFieldTypeInjected = "setPrimitive-Integer";
    }

    @SuppressWarnings("unused")
    private void setPrimitive(int integer) {
        assertNull("---> multiple injections occurred : setPrimitive-int",
                   ivMethodOrFieldTypeInjected);
        assertEquals("---> injected integer is not correct",
                     5566, integer);
        ivMethodOrFieldTypeInjected = "setPrimitive-int";
    }

    @SuppressWarnings("unused")
    private void setPrimitiveInt(Object integer) {
        assertNull("---> multiple injections occurred : setPrimitiveInt-Object",
                   ivMethodOrFieldTypeInjected);
        int intValue = ((Integer) integer).intValue();
        assertEquals("---> injected integer is not correct",
                     5566, intValue);
        ivMethodOrFieldTypeInjected = "setPrimitiveInt-Object";
    }

    @SuppressWarnings("unused")
    private void setPrimitiveInt(int integer) {
        assertNull("---> multiple injections occurred : setPrimitiveInt-int",
                   ivMethodOrFieldTypeInjected);
        assertEquals("---> injected integer is not correct",
                     5566, integer);
        ivMethodOrFieldTypeInjected = "setPrimitiveInt-int";
    }
}
