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
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
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

    @PersistenceUnit(unitName = "MyPersistenceUnit")
    EntityManagerFactory emf;

    @Override
    public void init(ServletConfig config) throws ServletException {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    /**
     * Checks whether there is automatic integration between Jakarta Persistence and
     * Jakarta Validation when Jakarta Data isn't involved. In this case, the entity
     * should be automatically validated.
     */
    @Test
    public void testJakartaPersistenceAndValidation() {
        Entitlement e = new Entitlement(3, "ACA", "person3@openliberty.io", Frequency.AS_NEEDED, 0, null, null, null);
        EntityManager em = emf.createEntityManager();
        try {
            em.persist(e);
            fail("Did not find the expected violations.");
        } catch (ConstraintViolationException x) {
            assertEquals("found: " + x.getConstraintViolations(), 2, x.getConstraintViolations().size());
        } finally {
            em.close();
        }
    }

    /**
     * Attempt to save a Java class (no entity annotation) that violates constraints for PastOrPresent.
     */
    @Test
    public void testInsertInvalidPastOrPresent_Class() {
        Creature c = new Creature(100l, "Black Bear", "Ursus americanus", //
                        BigDecimal.valueOf(44107730l, 6), BigDecimal.valueOf(-92489272l, 6), //
                        ZonedDateTime.now(ZoneId.of("America/Chicago")).plusHours(1).toOffsetDateTime(), //
                        172.5f);

        Set<?> violations = Collections.emptySet();
        try {
            creatures.save(c);
            fail("ConstraintViolationException was not raised.");
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(violations.toString(), 1, violations.size());
    }

    /**
     * Attempt to save a Jakarta Persistence entity that violated constraints for Pattern.
     */
    @Test
    public void testInsertInvalidPattern_PersistenceEntity() {
        Entitlement e = new Entitlement(2, "MEDICARE", "person1@openliberty.io", Frequency.AS_NEEDED, 65, null, null, null);
        Set<?> violations = Collections.emptySet();
        try {
            entitlements.save(e);
            fail("ConstraintViolationException was not raised.");
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(violations.toString(), 1, violations.size());
    }

    /**
     * Attempt to save a Jakarta Persistence entity that violated constraints for Pattern.
     */
    @Test
    public void testInsertInvalidPositiveMax_Record() {
        Rectangle r = new Rectangle("R1", 100l, 150l, -10, 30000);
        Set<?> violations = Collections.emptySet();
        try {
            rectangles.save(r);
            fail("ConstraintViolationException was not raised.");
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
    public void testInsertValidClass() {
        Creature c = new Creature(200l, "White-Tailed Deer", "Odocoileus virginianus", //
                        BigDecimal.valueOf(44040061l, 6), BigDecimal.valueOf(-92470320l, 6), //
                        ZonedDateTime.of(2023, 7, 12, 14, 51, 41, 100, ZoneId.of("America/Chicago")).toOffsetDateTime(), //
                        68.3f);

        Set<?> violations = Collections.emptySet();
        try {
            creatures.save(c);
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(Collections.EMPTY_SET, violations);
    }

    /**
     * Save a Jakarta Persistence entity that has no constraint violations.
     */
    @Test
    public void testInsertValidPersistenceEntity() {
        Entitlement e = new Entitlement(1, "US-SOCIALSECURITY", "person2@openliberty.io", Frequency.MONTHLY, 62, null, Float.valueOf(0), BigDecimal.valueOf(4555));
        Set<?> violations = Collections.emptySet();
        try {
            entitlements.save(e);
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
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(Collections.EMPTY_SET, violations);
    }

    /**
     * Attempt to save updates to Java class entities (no entity annotation) where the updates violate one or more constraints.
     */
    @Test
    public void testUpdateInvalidPatternAndMax_Class() {
        final ZoneId CENTRAL = ZoneId.of("America/Chicago");
        Iterable<Creature> added = creatures.saveAll(List.of( //
                                                             new Creature(600l, "Beaver", "Castor canadensis", //
                                                                             BigDecimal.valueOf(44035314, 6), BigDecimal.valueOf(-92468053, 6), //
                                                                             ZonedDateTime.of(2023, 7, 27, 12, 54, 6, 600, CENTRAL).toOffsetDateTime(), //
                                                                             27.5f), //
                                                             new Creature(700l, "Raccoon", "Procyon lotor", //
                                                                             BigDecimal.valueOf(44078777, 6), BigDecimal.valueOf(-92480836, 6), //
                                                                             ZonedDateTime.of(2023, 7, 27, 12, 56, 7, 700, CENTRAL).toOffsetDateTime(), //
                                                                             18.7f)));

        Iterator<Creature> it = added.iterator();
        assertEquals(true, it.hasNext());
        Creature c1 = it.next();
        assertEquals(true, it.hasNext());
        Creature c2 = it.next();
        assertEquals(false, it.hasNext());

        Set<?> violations = Collections.emptySet();

        // First not valid, second valid:
        c1.scientificName = "Castor Canadensis"; // invalid capital letter at beginning of species name
        c2.weight = 18.8f; // valid update

        try {
            Iterable<Creature> updated = creatures.saveAll(List.of(c1, c2));
            fail("ConstraintViolationException was not raised. Updated: " + updated);
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(violations.toString(), 1, violations.size());

        // The violation on c1 must prevent the valid update to c2:
        c2 = creatures.findById(c2.id).orElseThrow();
        assertEquals(18.7f, c2.weight, 0.00001f);

        // First valid, second not valid:
        c1.scientificName = "Castor canadensis"; // original value
        c1.weight = 27.6f; // valid update
        c2.latitude = BigDecimal.valueOf(94078777, 6); // invalid update

        try {
            Iterable<Creature> updated = creatures.saveAll(List.of(c1, c2));
            fail("ConstraintViolationException was not raised. Updated: " + updated);
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(violations.toString(), 1, violations.size());

        // The violation on c2 must prevent the valid update to c1:
        c1 = creatures.findById(c1.id).orElseThrow();
        assertEquals(27.5f, c1.weight, 0.00001f);
        assertEquals("Castor canadensis", c1.scientificName);
    }

    /**
     * Attempt to save updates to record entities where the updates violate one or more constraints.
     */
    @Test
    public void testUpdateInvalidZeroWidth_Records() {
        rectangles.saveAll(new Rectangle("R6", 600l, 660l, 16, 60),
                           new Rectangle("R7", 700l, 770l, 17, 70));

        Set<?> violations = Collections.emptySet();

        // First not valid, second valid:
        Rectangle r6 = new Rectangle("R6", 600l, 660l, 0, 60);
        Rectangle r7 = new Rectangle("R7", 700l, 770l, 7, 70);

        try {
            rectangles.saveAll(r6, r7);
            fail("ConstraintViolationException was not raised.");
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(violations.toString(), 1, violations.size());

        // The violation on r6 must prevent the valid update to r7:
        assertEquals(17, rectangles.findWidthById("R7"));

        // First valid, second not valid:
        r6 = new Rectangle("R6", 600l, 660l, 6, 60);
        r7 = new Rectangle("R7", 700l, 770l, 0, 70);

        try {
            rectangles.saveAll(r6, r7);
            fail("ConstraintViolationException was not raised.");
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(violations.toString(), 1, violations.size());

        // The violation on r7 must prevent the valid update to r6:
        assertEquals(16, rectangles.findWidthById("R6"));
    }

    /**
     * Save updates to Java class entities (no entity annotation) where the updates have no constraint violations.
     */
    @Test
    public void testUpdateValidClasses() {
        final ZoneId CENTRAL = ZoneId.of("America/Chicago");
        Iterable<Creature> added = creatures.saveAll(List.of( //
                                                             new Creature(300l, "Groundhog", "Marmota monax", //
                                                                             BigDecimal.valueOf(44074047, 6), BigDecimal.valueOf(-92646106, 6), //
                                                                             ZonedDateTime.of(2023, 7, 27, 10, 28, 30, 300, CENTRAL).toOffsetDateTime(), //
                                                                             5.6f), //
                                                             new Creature(400l, "Ruby-throated Hummingbird", "Archilochus colubris", //
                                                                             BigDecimal.valueOf(44078371, 6), BigDecimal.valueOf(-92648177, 6), //
                                                                             ZonedDateTime.of(2023, 7, 27, 10, 32, 40, 400, CENTRAL).toOffsetDateTime(), //
                                                                             0.0037f),
                                                             new Creature(500l, "Pileated Woodpecker", "Dyrocopus pileatus", //
                                                                             BigDecimal.valueOf(44040137, 6), BigDecimal.valueOf(-92471988, 6), //
                                                                             ZonedDateTime.of(2023, 7, 27, 10, 35, 50, 500, CENTRAL).toOffsetDateTime(), //
                                                                             0.292f)));
        Iterator<Creature> it = added.iterator();
        assertEquals(true, it.hasNext());
        Creature c1 = it.next();
        assertEquals(true, it.hasNext());
        Creature c2 = it.next();
        assertEquals(true, it.hasNext());
        Creature c3 = it.next();
        assertEquals(false, it.hasNext());

        Set<?> violations = Collections.emptySet();

        c1.weight = 5.7f;
        c3.latitude = BigDecimal.valueOf(44040337, 6);
        try {
            creatures.saveAll(Set.of(c1, c3));
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(Collections.EMPTY_SET, violations);

        c2.longitude = BigDecimal.valueOf(-92647748, 6);
        try {
            creatures.save(c2);
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(Collections.EMPTY_SET, violations);
    }

    /**
     * Save updates to records where the updates have no constraint violations.
     */
    @Test
    public void testUpdateValidRecords() {
        rectangles.saveAll(new Rectangle("R3", 300l, 330l, 13, 30),
                           new Rectangle("R4", 400l, 440l, 14, 40),
                           new Rectangle("R5", 500l, 550l, 15, 50));

        Set<?> violations = Collections.emptySet();
        try {
            rectangles.saveAll(new Rectangle("R4", 400l, 440l, 14, 44),
                               new Rectangle("R5", 500l, 550l, 15, 55));
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(Collections.EMPTY_SET, violations);

        try {
            rectangles.save(new Rectangle("R3", 300l, 330l, 33, 30));
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(Collections.EMPTY_SET, violations);
    }
}
