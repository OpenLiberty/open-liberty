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
package bval.v20.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.validation.ClockProvider;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.FutureOrPresent;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/BeanVal20TestServlet" },
            loadOnStartup = 1)
public class BeanVal20TestServlet extends FATServlet {

    @Resource(name = "TestValidatorFactory")
    ValidatorFactory ivVFactory;

    @Resource(name = "TestValidator")
    Validator ivValidator;

    Instant checkpointBvalNow = null;
    ZoneId checkpointBvalZoneId = null;
    TimeZone checkpointSystemTimeZone = null;
    FieldValidatedBean checkpointBean = null;

    /**
     * During checkpoint capture this instant using the clock provided by the
     * default validation configuration and exercise a temporal constraint in
     * order to set its clock instance.
     *
     * This method implicitly verifies Validator resource injection.
     */
    @Override
    public void init() {

        // The clock provided by the default BeanVal config is systemDefaultZone.
        // This clock imparts dependency on time-zone, which we do not care about
        // for now because Java CRIU support does not adjust the system time zone
        // within restored JVMs.
        checkpointBvalNow = ivVFactory.getClockProvider().getClock().instant().now();
        checkpointBvalZoneId = ivVFactory.getClockProvider().getClock().getZone();
        checkpointSystemTimeZone = TimeZone.getDefault();
        checkpointBean = new FieldValidatedBean();

        FieldValidatedBean b = new FieldValidatedBean();
        b.futureOrPresent = checkpointBvalNow.minus(10, ChronoUnit.MILLIS);
        assertViolations(ivValidator.validate(b), FutureOrPresent.class);

        System.out.println("--- BeanVal20TestServlet init now: " + checkpointBvalNow + " ---");
    }

    /**
     * Verify the default BeanVal clock tracks time as expected and that time zone
     * zone does not change from checkpoint to restore. The clock should not be fixed
     * within the DefaultClockProvider nor any constraint validators exercised during
     * checkpoint.
     *
     * This test also exercises basic constraint validation for Java Beans.
     */
    @Test
    public void testDefaultClockProvider() throws Exception {

        // Validate the system clock instant set by the bean during checkpoint is in the past
        assertViolations(ivValidator.validate(checkpointBean), FutureOrPresent.class);

        // Validate the bval clock instance captured during checkpoint is in the past
        FieldValidatedBean b = new FieldValidatedBean();
        b.futureOrPresent = checkpointBvalNow;
        assertViolations(ivValidator.validate(b), FutureOrPresent.class);

        ZoneId restoreBvalZoneId = ivVFactory.getClockProvider().getClock().getZone();
        assertEquals("The time zone id for the default bval clock changed between checkpoint and restore.",
                     checkpointBvalZoneId, restoreBvalZoneId);

        TimeZone restoreSystemTimeZone = TimeZone.getDefault();
        assertEquals("The system time zone changed between checkpoint and restore.",
                     checkpointSystemTimeZone, restoreSystemTimeZone);
    }

    /**
     * Verify the module ValidatorFactory may be looked up at:
     * java:comp/env/TestValidatorFactory
     */
    @Test
    public void testDefaultValidatorFactoryLookup() throws Exception {
        assertNotNull(InitialContext.doLookup("java:comp/env/TestValidatorFactory"));
    }

    /**
     * Verify the module Validator may be looked up at:
     * java:comp/env/TestValidator
     */
    @Test
    public void testDefaultValidatorLookup() throws Exception {
        assertNotNull(InitialContext.doLookup("java:comp/env/TestValidator"));
    }

    private void assertViolations(Set<ConstraintViolation<FieldValidatedBean>> violations, Class<?>... constraintTypes) {
        assertEquals(constraintTypes.length, violations.size());

        Set<String> foundConstraints = new HashSet<>();
        for (ConstraintViolation<FieldValidatedBean> v : violations) {
            String constraintAnno = v.getConstraintDescriptor().getAnnotation().toString();
            System.out.println("Found constraint violation '" + v.getMessage() + "' from annotation " + constraintAnno);
            foundConstraints.add(constraintAnno);
        }

        for (Class<?> expectedConstraint : constraintTypes)
            assertTrue("Did not find expected constraint " + expectedConstraint.getCanonicalName() + " in " + foundConstraints,
                       foundConstraints.stream().anyMatch(s -> s.contains(expectedConstraint.getCanonicalName())));
    }
}
