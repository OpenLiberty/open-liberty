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
package app.injection.ee10;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.api.listener.AbstractJobListener;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.context.JobContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import app.injection.NoActiveBatchJobException;
import app.injection.beans.AbstractScopedBean;

/**
 * Groups inner classes consisting of batch artifacts all sharing common injections.
 */
public class InjectablesNonStringProps {

    private static final String expectedColor = "blue";
    private static final Integer expectedQuantity = 4;
    private static final Short expectedShort = 13;
    private static final Long expectedLong = 2048000L;
    private static final Float expectedFloat = 60.305F;
    private static final Double expectedDouble = 120.61D;
    private static final Boolean expectedBool = true;

    public static Logger logger = Logger.getLogger("test");

    @Dependent
    @Named("InjectionNonStringPropsJobListener")
    public static class JobListener extends AbstractJobListener {

        @Inject
        NonStringPropsBean bean;

        @Inject
        JobContext jobCtx;

        @Inject
        @BatchProperty
        protected String color;

        @Inject
        @BatchProperty
        protected Integer quantity;

        @Inject
        @BatchProperty
        Short shortProp;

        @Inject
        @BatchProperty
        Long longProp;

        @Inject
        @BatchProperty
        Float floatProp;

        @Inject
        @BatchProperty
        Double doubleProp;

        @Inject
        @BatchProperty
        Boolean boolProp;

        @Override
        public void beforeJob() throws Exception {
            super.beforeJob();
            logger.fine("In JobListener beforeJob(), color = " + color + ", quantity = " + quantity);
            validateJobName(bean, jobCtx.getJobName());
            validatePropertyValues(bean);
        }
    }

    @Dependent
    @Named("InjectionNonStringPropsBatchlet")
    public static class Batchlet extends AbstractBatchlet {

        @Inject
        NonStringPropsBean bean;

        @Inject
        JobContext jobCtx;

        @Inject
        JobOperator jobOp;

        @Inject
        @BatchProperty
        String color;
        @Inject
        @BatchProperty
        Integer quantity;
        @Inject
        @BatchProperty
        Short shortProp;
        @Inject
        @BatchProperty
        Long longProp;
        @Inject
        @BatchProperty
        Float floatProp;
        @Inject
        @BatchProperty
        Double doubleProp;
        @Inject
        @BatchProperty
        Boolean boolProp;

        @Override
        public String process() throws Exception {
            logger.fine("In Batchlet process(), color = " + color + ", quantity = " + quantity);
            validateJobName(bean, jobCtx.getJobName());
            validatePropertyValues(bean);
            assertNotNull("Failed to inject default JobOperator", jobOp);
            return null;
        }

    }

    /**
     * For each bean passed in, validate that {@link AbstractScopedBean#getJobName()} returns the expected job name
     * from the {@link JobContext}.
     *
     * This proves that the batch runtime, via CDI, was able to inject the active JobContext into each of these beans.
     *
     * @param beans           list of beans to validate
     * @param expectedJobName expected job name from JSL
     * @throws NoActiveBatchJobException if there is no active job (JobContext)
     */
    private static void validateJobName(NonStringPropsBean bean, String expectedJobName) throws NoActiveBatchJobException {
        logger.fine("Validating bean = " + bean);
        assertEquals("Incorrect job name value for bean: " + bean, expectedJobName, bean.getJobName());
    }

    /**
     * Validate that each property has been injected with the expected value, coming from either the JSL or runtime parameters.
     *
     * @param bean
     */
    private static void validatePropertyValues(NonStringPropsBean bean) {
        logger.fine("Validating properties of bean = " + bean);
        assertEquals("Incorrect color value for bean: " + bean, expectedColor, bean.getColor());
        assertEquals("Incorrect quantity value for bean: " + bean, expectedQuantity, bean.getQuantity());
        assertEquals("Incorrect short value for bean: " + bean, expectedShort, bean.getShortProp());
        assertEquals("Incorrect long value for bean: " + bean, expectedLong, bean.getLongProp());
        assertEquals("Incorrect float value for bean: " + bean, expectedFloat, bean.getFloatProp());
        assertEquals("Incorrect double value for bean: " + bean, expectedDouble, bean.getDoubleProp());
        assertEquals("Incorrect boolean value for bean: " + bean, expectedBool, bean.getBoolProp());
    }

}
