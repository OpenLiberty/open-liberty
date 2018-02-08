/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package defaultbeanvalidationcdi.test;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.xml.bind.ValidationException;

import defaultbeanvalidationcdi.beans.TestBean;

@ApplicationScoped
public class BValAtInjection {

    @Inject
    TestBean bean;
    
    @Inject
    Validator validator;
    
    /**
     * Test that a ConstraintValidator is created as a CDI managed bean when
     * a custom constraint validator factory is not specified and the application
     * is CDI enabled.
     */
    public void testConstraintValidatorInjection() throws Exception {
        //Validator validator = injectValidatorFactory.getValidator();
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        if (!violations.isEmpty()) {
            StringBuffer msg = new StringBuffer();
            for (ConstraintViolation<TestBean> cv : violations) {
                msg.append("\n\t" + cv.toString());
            }
            throw new ValidationException("validating produced constraint violations: " + msg);
        }
    }
}