/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package val31.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Set;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@SuppressWarnings("serial")
@WebServlet("/Validation31TestServlet")
public class Validation31TestServlet extends FATServlet {

    @Inject
    Validator validator;

    /**
     * Basic test for using Jakarta Validation on a Record.
     */
    @Test
    public void basicRecordTest() throws Exception {

        Person p = new Person("SampleName");
        Person np = new Person(null);

        assertTrue("Record Person(\"SampleName\") should have validated with no violations", validator.validate(p).size() == 0);
        Set<ConstraintViolation<Person>> violations = validator.validate(np);
        assertTrue("Record Person(null) should have validated with one violation", violations.size() == 1);
    }

    /**
     * Test that validateProperty and validateValue work on a record
     */
    @Test
    public void recordValidatePropertyAndValueTest() throws Exception {
        // TODO create a test using the Validator.validateProperty and Validator.validateValue methods
        // on a record
        Person propertydata = new Person(null);
        Set<ConstraintViolation<Person>> propertyViolations = validator.validateProperty(propertydata, "name");
        Set<ConstraintViolation<Person>> valueViolations = validator.validateValue(Person.class, "name", null);
        assertTrue("Record Person(null) should have validated with one violation", propertyViolations.size() == 1);
        assertEquals("Record Person(null) should have validated with one violation", 1, valueViolations.size());
    }

    /**
     * Test that the result of a method on a record can be validated.
     */

    @Test
    public void recordMethodValidationTest() throws Exception {
        // TODO Add a method that includes validation to a record class and confirm that it can be validated

        Person object = new Person("x");
        Method method = Person.class.getMethod("getName");
        String returnValue = object.getName();
        Set<ConstraintViolation<Person>> violations = validator.forExecutables()
                        .validateReturnValue(object, method, returnValue);
        assertEquals("Record Person('x') should have validated with one violation", 1, violations.size());
    }

    /**
     * Test that cascade validation works with records.
     */
    @Test
    public void nestedRecordsTest() throws Exception {
        // TODO test that cascade validation occurs by creating a record which contains another record class
        // (For example a Person that has an Email address) then confirm that it validates both.
        // See section 5.6.4 of the Bean Validation specification

        Employee emp = new Employee(null, new EmailAddress("emp1@example.com"));
        Set<ConstraintViolation<Employee>> violations = validator.validate(emp);
        assertEquals("Record Employee() should have validated with one violation", 1, violations.size());
    }

    /**
     * Test that a group specified on a record can be validated separately, and that group conversion
     * works on a record.
     */
    @Test
    public void convertGroupsRecordsTest() throws Exception {
        // TODO See section 5.4 of the Bean Validation specification for groups
        // and section 5.4.5 for group conversion

        Registration reg = new Registration("x1asas", false);
        Set<ConstraintViolation<Registration>> constraintViolations = validator.validate(reg);
        assertTrue("Record Registration should have validated with no violations", constraintViolations.size() == 0);
        Company cmp2 = new Company(" sds", reg);
        Set<ConstraintViolation<Company>> constraintViolations2 = validator.validate(cmp2, RegistrationChecks.class);
        assertTrue("Record Company should have validated with 2 violations after conversion", constraintViolations2.size() == 2);
    }

}