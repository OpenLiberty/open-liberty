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
package test.jakarta.data.validation.web;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Set;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.Test;

import componenttest.app.FATServlet;
import test.jakarta.data.validation.web.Entitlement.Frequency;

@DataSourceDefinition(name = "java:module/jdbc/DerbyDataSource",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                      databaseName = "memory:testdb",
                      user = "dbuser1",
                      password = "dbpwd1",
                      properties = "createDatabase=create")
@SuppressWarnings("serial")
@WebServlet("/*")
public class DataValidationTestServlet extends FATServlet {

    @Inject
    Creatures creatures; // for Java class without an entity annotation

    @Inject
    Entitlements entitlements; // for @Entity from Jakarta Persistence

    @Inject
    Rectangles rectangles; // for records

    Validator validator;

    @Override
    public void init(ServletConfig config) throws ServletException {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    /**
     * Attempt to save a Java class (no entity annotation) that violates constraints for PastOrPresent.
     */
    @Test
    public void testSaveInvalidPastOrPresentClass() {
        Creature c = new Creature(100l, "Black Bear", "Ursus americanus", //
                        BigDecimal.valueOf(44107730l, 6), BigDecimal.valueOf(-92489272l, 6), //
                        ZonedDateTime.now(ZoneId.of("America/Chicago")).plusHours(1).toOffsetDateTime(), //
                        172.5f);

        Set<?> violations = Collections.emptySet();
        try {
            creatures.save(c);
            // TODO the following should be unnecessary once it is done by the Jakarta Data provider:
            violations = validator.validate(c);
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(violations.toString(), 1, violations.size());
    }

    /**
     * Attempt to save a Jakarta Persistence entity that violated constraints for Pattern.
     */
    @Test
    public void testSaveInvalidPatternPersistenceEntity() {
        Entitlement e = new Entitlement(2, "MEDICARE", "person1@openliberty.io", Frequency.AS_NEEDED, 65, null, null, null);
        Set<?> violations = Collections.emptySet();
        try {
            entitlements.save(e);
            // TODO omit the next line once Jakarta Data provider does the validation for us
            violations = validator.validate(e);
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(violations.toString(), 1, violations.size());
    }

    /**
     * Attempt to save a Jakarta Persistence entity that violated constraints for Pattern.
     */
    @Test
    public void testSaveInvalidPositiveMaxRecord() {
        Rectangle r = new Rectangle("R1", 100l, 150l, -10, 30000);
        Set<?> violations = Collections.emptySet();
        try {
            rectangles.save(r);
            // TODO omit the next line once Jakarta Data provider does the validation for us
            violations = validator.validate(r);
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(violations.toString(), 2, violations.size());
    }

    /**
     * Save a Java class (no entity annotation) that has no constraint violations.
     */
    @Test
    public void testSaveValidClass() {
        Creature c = new Creature(200l, "White-Tailed Deer", "Odocoileus virginianus", //
                        BigDecimal.valueOf(44040061l, 6), BigDecimal.valueOf(-92470320l, 6), //
                        ZonedDateTime.of(2023, 7, 12, 14, 51, 41, 100, ZoneId.of("America/Chicago")).toOffsetDateTime(), //
                        68.3f);

        Set<?> violations = Collections.emptySet();
        try {
            creatures.save(c);
            // TODO the following should be unnecessary once it is done by the Jakarta Data provider:
            violations = validator.validate(c);
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(Collections.EMPTY_SET, violations);
    }

    /**
     * Save a Jakarta Persistence entity that has no constraint violations.
     */
    @Test
    public void testSaveValidPersistenceEntity() {
        Entitlement e = new Entitlement(1, "US-SOCIALSECURITY", "person2@openliberty.io", Frequency.MONTHLY, 62, null, Float.valueOf(0), BigDecimal.valueOf(4555));
        Set<?> violations = Collections.emptySet();
        try {
            entitlements.save(e);
            // TODO the following should be unnecessary once it is done by the Jakarta Data provider:
            violations = validator.validate(e);
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(Collections.EMPTY_SET, violations);
    }

    /**
     * Save a record that has no constraint violations.
     */
    @Test
    public void testSaveValidRecord() {
        Rectangle r = new Rectangle("R2", 0l, 5l, 40, 50);
        Set<?> violations = Collections.emptySet();
        try {
            rectangles.save(r);
            // TODO the following should be unnecessary once it is done by the Jakarta Data provider:
            violations = validator.validate(r);
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(Collections.EMPTY_SET, violations);
    }
}
