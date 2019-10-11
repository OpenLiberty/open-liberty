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

package com.ibm.ws.ejbcontainer.injection.xml.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.injection.xml.ejb.TypeCompatible;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> TypeCompatibleInjectionTest .
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the injecting into fields and methods
 * that are declared as compatible types. <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testFieldTypeCompatibleInjection
 * - verifies that an EJB may be injected into a Field that has been
 * declared as a parent type (compatible).
 * <li>testMethodSpecificTypeWithCompatibleInjection
 * - verifies that an EJB may be injected into a Method that has been
 * declared as the proper type, but there are other methods with
 * compatible types.
 * <li>testMethodTypeCompatibleInjection
 * - verifies that an EJB may be injected into a Method that has been
 * declared as a parent type (compatible).
 * <li>testMethodTypeAmbiguousInjection
 * - verifies that an EJB will fail to be injected into a Method that
 * has been declared as a parent type (compatible), but there are
 * multiple methods with compatible types.
 * <li>testMethodObjectTypeWithSpecificPrimitiveAndCompatibleInjection
 * - verifies that a primitive object may be injected into a Method
 * that has been declared as the primitive object type, but there
 * are other methods with compatible types.
 * <li>testMethodObjectTypeWithPrimitiveAndCompatibleInjection
 * - verifies that a primitive object may be injected into a Method
 * that has been declared as the primitive type, but there are
 * other methods with compatible types.
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/TypeCompatibleInjectionServlet")
public class TypeCompatibleInjectionServlet extends FATServlet {

    /**
     * Test that an EJB may be injected into a Field that has been declared
     * as a parent type (compatible). <p>
     */
    @Test
    public void testFieldTypeCompatibleInjection() throws Exception {
        // Obtain reference of bean configured to inject into field
        // of parent type.
        TypeCompatible bean = (TypeCompatible) FATHelper.lookupLocalBinding("FieldTypeCompatibleInjectBean");
        assertNotNull("1 ---> SLLSB failed to inject.", bean);

        // Verify that the field was injected into properly.
        assertEquals("2 ---> Incorrect injection occurred.",
                     "field-Animal", bean.getMethodOrFieldTypeInjected());
    }

    /**
     * Test that an EJB may be injected into a Method that has been declared
     * as the proper type, but there are other methods with compatible types. <p>
     */
    @Test
    public void testMethodSpecificTypeWithCompatibleInjection() throws Exception {
        // Obtain reference of bean configured to inject into a method when there
        // are other compatible parameter type methods.
        TypeCompatible bean = (TypeCompatible) FATHelper.lookupLocalBinding("MethodSpecificTypeWithCompatibleInjectBean");
        assertNotNull("1 ---> SLLSB failed to inject.", bean);

        // Verify that the method was injected into properly.
        assertEquals("2 ---> Incorrect injection occurred.",
                     "setObject-Cat", bean.getMethodOrFieldTypeInjected());
    }

    /**
     * Test that an EJB may be injected into a Method that has been declared
     * as a parent type (compatible). <p>
     */
    @Test
    public void testMethodTypeCompatibleInjection() throws Exception {
        // Obtain reference of bean configured to inject into a method when the
        // parameter type is a parent of the bean type.
        TypeCompatible bean = (TypeCompatible) FATHelper.lookupLocalBinding("MethodTypeCompatibleInjectBean");
        assertNotNull("1 ---> SLLSB failed to inject.", bean);

        // Verify that the method was injected into properly.
        assertEquals("2 ---> Incorrect injection occurred.",
                     "setAnimal-Animal", bean.getMethodOrFieldTypeInjected());
    }

    /**
     * Test that an EJB will fail to be injected into a Method that has been declared
     * as a parent type (compatible), but there are multiple methods with compatible
     * types. <p>
     */
    @Test
    @ExpectedFFDC({ "com.ibm.wsspi.injectionengine.InjectionException", "com.ibm.ejs.container.EJBConfigurationException" })
    public void testMethodTypeAmbiguousInjection() throws Exception {
        try {
            // Obtain reference of bean configured to inject into a method when the
            // parameter type is a parent of the bean type and there are multiple
            // compatible methods.
            TypeCompatible bean = (TypeCompatible) FATHelper.lookupLocalBinding("MethodTypeAmbiguousInjectBean");
            fail("1 ---> SLLSB should not have been injected: " + bean);

        } catch (Exception cioex) {
            Throwable root;
            for (root = cioex; root.getCause() != null; root = root.getCause()) {
            }
            assertTrue("2 ---> Unexpected root exception : " + root,
                       root.getClass().getName().endsWith("InjectionException"));
            String msg = root.getMessage();
            assertTrue("3 ---> Exception text does not contain target-name",
                       (msg.contains("cat")));
            assertTrue("4 ---> Exception text does not contain target-class",
                       (msg.contains("com.ibm.ws.ejbcontainer.injection.xml.ejb.TypeCompatibleBean")));
            assertTrue("5 ---> Exception text does not contain set method name",
                       (msg.contains("setCat")));
            assertTrue("6 ---> Exception text does not contain ambiguous",
                       (msg.contains("ambiguous")));
        }
    }

    /**
     * Test that a primitive object may be injected into a Method that has been declared
     * as the primitive object type, but there are other methods with compatible types. <p>
     */
    @Test
    public void testMethodObjectTypeWithSpecificPrimitiveAndCompatibleInjection() throws Exception {
        // Obtain reference of bean configured to inject into a method when there
        // are other compatible parameter type methods.
        TypeCompatible bean = (TypeCompatible) FATHelper.lookupLocalBinding("MethodObjectTypeWithSpecificPrimitiveAndCompatibleInjectBean");
        assertNotNull("1 ---> SLLSB failed to inject.", bean);

        // Verify that the method was injected into properly.
        assertEquals("2 ---> Incorrect injection occurred.",
                     "setPrimitive-Integer", bean.getMethodOrFieldTypeInjected());
    }

    /**
     * Test that a primitive object may be injected into a Method that has been declared
     * as the primitive type, but there are other methods with compatible types. <p>
     */
    @Test
    public void testMethodObjectTypeWithPrimitiveAndCompatibleInjection() throws Exception {
        // Obtain reference of bean configured to inject into a method when there
        // are other compatible parameter type methods.
        TypeCompatible bean = (TypeCompatible) FATHelper.lookupLocalBinding("MethodObjectTypeWithPrimitiveAndCompatibleInjectBean");
        assertNotNull("1 ---> SLLSB failed to inject.", bean);

        // Verify that the method was injected into properly.
        assertEquals("2 ---> Incorrect injection occurred.",
                     "setPrimitiveInt-int", bean.getMethodOrFieldTypeInjected());
    }

}
