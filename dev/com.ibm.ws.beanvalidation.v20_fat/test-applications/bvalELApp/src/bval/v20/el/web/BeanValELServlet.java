/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package bval.v20.el.web;

import static componenttest.annotation.SkipForRepeat.EE9_OR_LATER_FEATURES;
import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;
import static org.junit.Assert.assertEquals;

import java.util.Set;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;

/**
 * Bean Validation App to test Expression Language. There is behavior change with EL evaluation
 * between EE8 and EE9+, so this Servlet confirms the difference in behavior.
 */
@SuppressWarnings("serial")
@WebServlet("/BeanValELServlet")
public class BeanValELServlet extends FATServlet {

    @Inject
    Validator validator;

    @Inject
    ELBean bean;

    /**
     * Test that Expression Language can be evaluated in a validation method. This was changed
     * in Hibernate Validator 7.0, so it should only occur on EE8 (NO_MODIFICATION).
     */
    @SkipForRepeat(EE9_OR_LATER_FEATURES)
    @Test
    public void testELEvaluationEE8() {
        Set<ConstraintViolation<ELBean>> violations = validator.validate(bean);
        violations.iterator().forEachRemaining(v -> {
            assertEquals("Expression Language String should have evaluated to 3", "3", v.getMessage());
        });
    }

    /**
     * Test that Expression Language can not be evaluated in a validation method. This was changed
     * in Hibernate Validator 7.0, so it should not work on EE8 (NO_MODIFICATION).
     */
    @SkipForRepeat(NO_MODIFICATION)
    @Test
    public void testELEvaluationEE9plus() {
        Set<ConstraintViolation<ELBean>> violations = validator.validate(bean);
        violations.iterator().forEachRemaining(v -> {
            assertEquals("Expression Language String should not have been evaluated", "${1+2}", v.getMessage());
        });
    }

}
