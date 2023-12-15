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
import java.util.Arrays;
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
     * Use a repository method with constraints on the different method parameters and the return type.
     * Verify that these methods raise ConstraintViolationException when the constraints are violated, but otherwise run successfully.
     */
    @Test
    public void testConstraintsOnRepositoryMethod() {
        Creature c1 = new Creature(1001l, "Mink", "Neogale vison", //
                        BigDecimal.valueOf(44037855L, 6), BigDecimal.valueOf(-92508962L, 6), //
                        ZonedDateTime.now(ZoneId.of("America/Chicago")).minusMinutes(10).toOffsetDateTime(), //
                        1.9f);
        Creature c2 = new Creature(1002l, "Mink", "Neogale vison", //
                        BigDecimal.valueOf(44036829L, 6), BigDecimal.valueOf(-92507138L, 6), //
                        ZonedDateTime.now(ZoneId.of("America/Chicago")).minusMinutes(20).toOffsetDateTime(), //
                        2.2f);
        Creature c3 = new Creature(1003l, "Double-crested Cormorant", "Nannopterum auritum", //
                        BigDecimal.valueOf(44028468L, 6), BigDecimal.valueOf(-92506186L, 6), //
                        ZonedDateTime.now(ZoneId.of("America/Chicago")).minusMinutes(30).toOffsetDateTime(), //
                        2.3f);
        Creature c4 = new Creature(1004l, "Mink", "Neogale vison", //
                        BigDecimal.valueOf(44036987L, 6), BigDecimal.valueOf(-92503694L, 6), //
                        ZonedDateTime.now(ZoneId.of("America/Chicago")).minusMinutes(40).toOffsetDateTime(), //
                        1.4f);
        creatures.saveAll(List.of(c1, c2, c3, c4));

        List<Creature> found;

        // no constraints violated:
        found = creatures.findByScientificNameStartsWithAndWeightBetween("Neogale ", 1.0f, 2.0f);
        assertEquals(2, found.size());

        try {
            found = creatures.findByScientificNameStartsWithAndWeightBetween(" ", 1.0f, 2.0f);
            fail("Did not detect violation of constraint on first parameter to be non-blank.");
        } catch (ConstraintViolationException x) {
            // expected
        }

        try {
            found = creatures.findByScientificNameStartsWithAndWeightBetween("Neogale ", -1.0f, 2.5f);
            fail("Did not detect violation of constraint on second parameter to be positive.");
        } catch (ConstraintViolationException x) {
            // expected
        }

        try {
            found = creatures.findByScientificNameStartsWithAndWeightBetween("Neogale ", 2.0f, -3.0f);
            fail("Did not detect violation of constraint on third parameter to be positive.");
        } catch (ConstraintViolationException x) {
            // expected
        }

        try {
            found = creatures.findByScientificNameStartsWithAndWeightBetween("N%", 1.0f, 3.0f);
            fail("Did not detect violation on constraint on return value to have a maximum size of 3.");
        } catch (ConstraintViolationException x) {
            // expected
        }
    }

    /**
     * Verify that a constraint placed on the parameterized type variable for the Id type
     * enforces validation on that type when methods from the repository are used.
     */
    //TODO enable once Jakarta Validation allows constraints on type variables of interface
    //@Test
    public void testIdTypeVariableWithConstraint() {
        // invalid method argument type
        try {
            int count = creatures.countById(-1L);
            fail("Did not detect violated constraint. Instead found: " + count);
        } catch (ConstraintViolationException x) {
            Set<?> violations = x.getConstraintViolations();
            if (violations.isEmpty())
                throw x;
        }

        assertEquals(0, creatures.countById(1000000L));
    }

    /**
     * Checks whether there is automatic integration between Jakarta Persistence and
     * Jakarta Validation when Jakarta Data isn't involved. In this case, the entity
     * should be automatically validated.
     */
    @Test
    public void testJakartaPersistenceAndValidation() throws Exception {
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
    //TODO enable once Jakarta Validation allows @Valid on type variables of interface
    //@Test
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
     * Save a Java class (no entity annotation) without any constraint violations.
     * Modify the entity in the database such that it violates constraints for Positive.
     * Verify that the invalid entity can be successfully removed.
     */
    @Test
    public void testRemoveInvalidNegative_Class() {
        Creature c = new Creature(800l, "Hooded Merganser", "Lophodytes cucullatus", //
                        BigDecimal.valueOf(47187965l, 6), BigDecimal.valueOf(-95193545l, 6), //
                        ZonedDateTime.of(2023, 8, 7, 15, 17, 18, 888, ZoneId.of("America/Chicago")).toOffsetDateTime(), //
                        0.81f);
        creatures.save(c);

        // Validation does not apply to updates made in ways that do not involve the class
        assertEquals(true, creatures.updateByIdSetWeight(800L, -0.823f));

        // Validation does not apply to find operations.
        c = creatures.findById(800l).orElseThrow();

        // The application can choose to validate entities from find operations manually:
        Set<?> violations = validator.validate(c);
        assertEquals(violations.toString(), 1, violations.size());

        // It must be possible to remove invalid entities:
        creatures.delete(c);
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
     * Save a Java class (no entity annotation) without any constraint violations.
     * Modify the entity in the database such that it violates constraints for Positive.
     * Verify that the invalid entity can be updated to be made valid again.
     */
    @Test
    public void testUpdateFixInvalidNegative_Class() {
        creatures.save(new Creature(900l, "Moose", "Alces alces", //
                        BigDecimal.valueOf(47513117l, 6), BigDecimal.valueOf(-91169845l, 6), //
                        ZonedDateTime.of(2023, 8, 7, 15, 18, 19, 999, ZoneId.of("America/Chicago")).toOffsetDateTime(), //
                        459.2f));

        // Validation does not apply to updates made in ways that do not involve the class
        assertEquals(true, creatures.updateByIdSetWeight(900L, -452.9f));

        // Validation does not apply to find operations.
        Creature c = creatures.findById(900l).orElseThrow();

        // The application can choose to validate entities from find operations manually:
        Set<?> violations = validator.validate(c);
        assertEquals(violations.toString(), 1, violations.size());

        // Should be able to update the entity to fix it.
        c.weight = 452.9f;
        c = creatures.save(c);
        assertEquals(452.9f, c.weight, 0.00001f);

        violations = validator.validate(c);
        assertEquals(violations.toString(), 0, violations.size());
    }

    /**
     * Attempt to save updates to Jakarta Persistence entities where the updates violate one or more constraints.
     */
    @Test
    public void testUpdateSaveInvalidMinAndEmail_Entity() {
        Entitlement[] e = new Entitlement[2];
        e[0] = new Entitlement(4, "US-SNAP", "person4@openliberty.io", Frequency.AS_NEEDED, 50, null, 23.00f, BigDecimal.valueOf(43.00f));
        e[1] = new Entitlement(5, "US-TANF", "person5@openliberty.io", Frequency.MONTHLY, 13, null, 1549.00f, BigDecimal.valueOf(5266.00f));
        e = entitlements.save(e);

        Set<?> violations = Collections.emptySet();

        // First valid, second not valid:
        e[0].beneficiaryEmail = "person-4@openliberty.io";
        e[1].setMinAge(-2); // less than minimum of 0

        try {
            entitlements.save(e);
            fail("ConstraintViolationException was not raised.");
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(violations.toString(), 1, violations.size());

        // The violation on e[1] must prevent the valid update to e[0]:
        e[0] = entitlements.findById(4).orElseThrow();
        assertEquals("person4@openliberty.io", e[0].beneficiaryEmail);

        // First invalid, second valid:
        e[0].beneficiaryEmail = "person4"; // invalid email address
        e[0].frequency = null; // invalid null
        e[0].type = "US-?"; // invalid length
        e[1].setMinAge(15); // valid update

        try {
            entitlements.save(e);
            fail("ConstraintViolationException was not raised.");
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
        }
        assertEquals(violations.toString(), 3, violations.size());

        // The violations on e[0] must prevent the otherwise valid update to e[1]:
        e[1] = entitlements.findById(5).orElseThrow();
        assertEquals(13, e[1].getMinAge());
    }

    /**
     * Attempt to save updates to Java class entities (no entity annotation) where the updates violate one or more constraints.
     */
    //TODO enable once Jakarta Validation allows @Valid on type variables of interface
    //@Test
    public void testUpdateSaveInvalidPatternAndMax_Class() {
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
    public void testUpdateSaveInvalidZeroWidth_Records() {
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
            if (violations.isEmpty())
                throw x;
        }
        assertEquals(Collections.EMPTY_SET, violations);

        c2.longitude = BigDecimal.valueOf(-92647748, 6);
        try {
            creatures.save(c2);
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
            if (violations.isEmpty())
                throw x;
        }
        assertEquals(Collections.EMPTY_SET, violations);
    }

    /**
     * Save updates to entities where the updates have no constraint violations.
     */
    @Test
    public void testUpdateValidEntities() {
        Entitlement[] e = new Entitlement[3];
        e[0] = new Entitlement(5, "US-SOCIALSECURITY", "person4@openliberty.io", Frequency.MONTHLY, 65, null, Float.valueOf(3100), BigDecimal.valueOf(4555));
        e[1] = new Entitlement(6, "US-SOCIALSECURITY", "person5@openliberty.io", Frequency.MONTHLY, 66, null, Float.valueOf(3100), BigDecimal.valueOf(4218));
        e[2] = new Entitlement(7, "US-SOCIALSECURITY", "person6@openliberty.io", Frequency.MONTHLY, 67, null, Float.valueOf(3100), BigDecimal.valueOf(3905));
        e = entitlements.save(e);

        Set<?> violations = Collections.emptySet();
        try {
            e[1].beneficiaryEmail = "person-5@openliberty.io";
            e[2].minBenefit = 3000.0f;
            e[2].maxBenefit = BigDecimal.valueOf(3499.0f);
            entitlements.save(new Entitlement[] { e[1], e[2] });
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
            if (violations.isEmpty())
                throw x;
        }
        assertEquals(Collections.EMPTY_SET, violations);

        try {
            e[0].frequency = Frequency.YEARLY;
            e[0].minBenefit *= 12.0f;
            e[0].maxBenefit = e[0].maxBenefit.multiply(BigDecimal.valueOf(12.0f));
            entitlements.save(e[0]);
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
            if (violations.isEmpty())
                throw x;
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
            if (violations.isEmpty())
                throw x;
        }
        assertEquals(Collections.EMPTY_SET, violations);

        try {
            rectangles.save(new Rectangle("R3", 300l, 330l, 33, 30));
        } catch (ConstraintViolationException x) {
            violations = x.getConstraintViolations();
            if (violations.isEmpty())
                throw x;
        }
        assertEquals(Collections.EMPTY_SET, violations);
    }

    /**
     * Verify that a repository method parameter that is annotated with a constraint is validated.
     */
    @Test
    public void testValidateMethodParameters() {
        // valid parameter
        rectangles.findByWidth(10);

        // invalid parameter
        try {
            List<Rectangle> found = rectangles.findByWidth(-20);
            fail("Did not detect violated constraint. Instead found: " + found);
        } catch (ConstraintViolationException x) {
            Set<?> violations = x.getConstraintViolations();
            if (violations.isEmpty())
                throw x;
        }
    }

    /**
     * Verify that a repository method return type that is annotated with a constraint is validated.
     */
    @Test
    public void testValidateReturnType() {
        // invalid empty return value
        try {
            Rectangle[] found = rectangles.findByIdStartsWith("R8");
            fail("Did not detect violated constraint. Instead found: " + Arrays.toString(found));
        } catch (ConstraintViolationException x) {
            Set<?> violations = x.getConstraintViolations();
            if (violations.isEmpty())
                throw x;
        }

        rectangles.save(new Rectangle("R8", 800L, 880L, 18, 80));

        // valid non-null return value
        Rectangle[] r = rectangles.findByIdStartsWith("R8");
        assertEquals(1, r.length);
        assertEquals("R8", r[0].id());
        assertEquals(800L, r[0].x());
        assertEquals(Long.valueOf(880L), r[0].y());
        assertEquals(18, r[0].width());
        assertEquals(Integer.valueOf(80), r[0].height());
    }
}
