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
package bval.v20.valueextractor.web;

import java.util.Arrays;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Assert;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/ValueExtractorServlet")
public class ValueExtractorServlet extends FATServlet {

    @Inject
    ValueExtractorBean valExtractBean;

    @Resource
    ValidatorFactory atResourceVF;

    @Test
    public void testCustomValueExtractorFromXml() throws Exception {
        ListValueExtractorWithInjection.counter = 0;

        if (this.valExtractBean == null) {
            throw new Exception("CDI didn't inject the bean ValueExtractorBean into this servlet");
        }
        this.valExtractBean.testCustomValueExtractorFromXml(Arrays.asList("a", "b", "c"));

        Assert.assertEquals(1, ListValueExtractorWithInjection.counter);

        try {
            this.valExtractBean.testCustomValueExtractorFromXml(Arrays.asList("x", null, null));
            throw new Exception("Call to ValueExtractorBean.testCustomValueExtractor() should have thrown CVE's");
        } catch (ConstraintViolationException e) {
            Set<ConstraintViolation<?>> cvs = e.getConstraintViolations();
            if (cvs.size() != 2) {
                int i = 0;
                for (ConstraintViolation<?> cv : cvs) {
                    System.out.println("Constraint violation " + ++i + ":");
                    System.out.println(cv.getMessage());
                }
                throw new Exception("interceptor validated method parameters and caught constraint violations, but size wasn't 2.");
            }
        }

        Assert.assertEquals(2, ListValueExtractorWithInjection.counter);
    }

    @Test
    public void testCustomValueExtractorFromServiceReg() throws Exception {
        DoubleWrapperValueExtractor.counter = 0;

        if (this.valExtractBean == null) {
            throw new Exception("CDI didn't inject the bean ValueExtractorBean into this servlet");
        }
        this.valExtractBean.testCustomValueExtractorFromServiceReg(new DoubleWrapper(9));

        Assert.assertEquals(1, DoubleWrapperValueExtractor.counter);

        try {
            this.valExtractBean.testCustomValueExtractorFromServiceReg(new DoubleWrapper(11));
            throw new Exception("Call to ValueExtractorBean.testCustomValueExtractor() should have thrown CVE's");
        } catch (ConstraintViolationException e) {
            Set<ConstraintViolation<?>> cvs = e.getConstraintViolations();
            if (cvs.size() != 1) {
                int i = 0;
                for (ConstraintViolation<?> cv : cvs) {
                    System.out.println("Constraint violation " + ++i + ":");
                    System.out.println(cv.getMessage());
                }
                throw new Exception("interceptor validated method parameters and caught constraint violations, but size wasn't 1.");
            }
        }

        Assert.assertEquals(2, DoubleWrapperValueExtractor.counter);
    }

    @Test
    public void testCustomValueExtractorOnValidatorFactory() throws Exception {
        IntWrapperValueExtractor.counter = 0;

        Configuration<?> configuration = Validation.byDefaultProvider().configure().addValueExtractor(new IntWrapperValueExtractor());
        Validator validator = configuration.buildValidatorFactory().getValidator();

        //Test with valid data.
        valExtractBean.intWrapper = new IntWrapper(1);
        valExtractBean.stringWrapper = new StringWrapper("abc");
        Set<ConstraintViolation<ValueExtractorBean>> violations = validator.validate(valExtractBean);

        Assert.assertNotNull(violations);

        StringBuffer msg = new StringBuffer();
        for (ConstraintViolation<ValueExtractorBean> cv : violations) {
            msg.append("\n\t" + cv.toString());
        }

        Assert.assertEquals(0, violations.size());

        Assert.assertEquals(1, IntWrapperValueExtractor.counter);

        //Test with invalid data.
        valExtractBean.intWrapper = new IntWrapper(0);
        violations = validator.validate(valExtractBean);

        Assert.assertNotNull(violations);

        msg = new StringBuffer();
        for (ConstraintViolation<ValueExtractorBean> cv : violations) {
            msg.append("\n\t" + cv.toString());
        }

        Assert.assertEquals(1, violations.size());

        Assert.assertEquals(2, IntWrapperValueExtractor.counter);
    }

    @Test
    public void testCustomValueExtractorOnValidator() throws Exception {
        StringWrapperValueExtractor.counter = 0;

        Validator validator = atResourceVF.usingContext() //
                        .addValueExtractor(new StringWrapperValueExtractor()) //
                        .addValueExtractor(new IntWrapperValueExtractor()) //
                        .getValidator();

        //Test with valid data.
        valExtractBean.intWrapper = new IntWrapper(1);
        valExtractBean.stringWrapper = new StringWrapper("abc");
        Set<ConstraintViolation<ValueExtractorBean>> violations = validator.validate(valExtractBean);

        Assert.assertNotNull(violations);

        StringBuffer msg = new StringBuffer();
        for (ConstraintViolation<ValueExtractorBean> cv : violations) {
            msg.append("\n\t" + cv.toString());
        }

        Assert.assertEquals(0, violations.size());
        Assert.assertEquals(1, StringWrapperValueExtractor.counter);

        //Test with invalid data.
        valExtractBean.stringWrapper = new StringWrapper(null);
        violations = validator.validate(valExtractBean);

        Assert.assertNotNull(violations);

        msg = new StringBuffer();
        for (ConstraintViolation<ValueExtractorBean> cv : violations) {
            msg.append("\n\t" + cv.toString());
        }

        Assert.assertEquals(1, violations.size());
        Assert.assertEquals(2, StringWrapperValueExtractor.counter);
    }
}
