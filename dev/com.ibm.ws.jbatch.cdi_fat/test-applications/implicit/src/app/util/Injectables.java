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
package app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.api.listener.AbstractJobListener;
import javax.batch.runtime.context.JobContext;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

import app.beans.AbstractBean;
import app.beans.AbstractScopedBean;
import app.beans.AppScopedBean;
import app.beans.AppScopedLocalEJB;
import app.beans.DependentBean;

/**
 * Groups inner classes consisting of batch artifacts and a non-batch artifact all sharing common injections.
 */
public class Injectables {

    public static Logger logger = Logger.getLogger("test");

    /**
     * We hard-code the set of classes for which we expect injection to occur, to assert that we do in fact see
     * the complete set inject via CDI.
     */
    private final static Set<Class> EXPECTED_BEANS = new HashSet<Class>(Arrays.asList(new Class[] { AppScopedBean.class, AppScopedLocalEJB.class, DependentBean.class }));
    private final static int EXPECTED_BEAN_COUNT = EXPECTED_BEANS.size();

    /**
     * These methods will be invoked outside the context of a batch job, and so will assert that the
     * batch context and properties haven't been set.
     */
    @Dependent
    public static class NonBatchArtifact {

        @Inject
        DependentBean depBean;

        @Inject
        JobContext jobCtx;

        @Inject
        @BatchProperty
        protected String color;

        public void assertBatchInjectionsNull() {

            assertNull("job ctx", jobCtx);
            assertNull("color", color);

            logger.fine("Getting job name for bean = " + depBean);

            try {
                depBean.getJobName();
                fail("Should have thrown an exception since no job is active");
            } catch (NoActiveBatchJobException e) {
            }

        }
    }

    @Dependent
    @Named("JobListener")
    public static class JobListener extends AbstractJobListener {

        @Inject
        @Any
        Instance<AbstractScopedBean> beans;

        @Inject
        JobContext jobCtx;

        @Inject
        @BatchProperty
        protected String color;

        @Override
        public void beforeJob() throws Exception {
            super.beforeJob();
            logger.fine("In JobListener beforeJob(), color = " + color);
            validateJobName(beans, jobCtx.getJobName());
            validateBeanCount(beans);
        }
    }

    @Dependent
    @Named("Batchlet")
    public static class Batchlet extends AbstractBatchlet {

        @Inject
        @Any
        Instance<AbstractScopedBean> beans;

        @Inject
        JobContext jobCtx;

        @Inject
        @BatchProperty
        protected String color;

        @Override
        public String process() throws Exception {
            logger.fine("In Batchlet process(), color = " + color);
            validateJobName(beans, jobCtx.getJobName());
            validateBeanCount(beans);
            return null;
        }

    }

    /**
     * For each bean passed in, validate that {@link AbstractScopedBean#getJobName()} returns the expected job name
     * from the {@link JobContext}.
     *
     * This proves that the batch runtime, via CDI, was able to inject the active JobContext into each of these beans.
     *
     * @param beans list of beans to validate
     * @param expectedJobName expected job name from JSL
     * @throws NoActiveBatchJobException if there is no active job (JobContext)
     */
    private static void validateJobName(Iterable<AbstractScopedBean> beans, String expectedJobName) throws NoActiveBatchJobException {
        for (AbstractBean b : beans) {
            logger.fine("Validating bean = " + b);
            assertEquals("Incorrect job name value for bean: " + b, expectedJobName, b.getJobName());
        }
    }

    /**
     * @param beans
     *
     *            Simply counts the number of injected beans and validates this against the hard-coded value calculated from
     *            our pre-determined set.
     *
     * @see Injectables#EXPECTED_BEANS
     *
     */
    private static void validateBeanCount(Instance<AbstractScopedBean> beans) {
        int count = 0;
        for (AbstractBean b : beans) {
            count++;
        }
        assertEquals("Didn't see expected # of AbstractScopedBeans injected", EXPECTED_BEAN_COUNT, count);
    }

    // TODO - validate batch properties?
}
