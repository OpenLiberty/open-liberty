/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package io.openliberty.ejbcontainer.fat.checkpoint.ejb;

import static org.junit.Assert.assertEquals;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;

/**
 * A singleton bean with the following configuration:
 * <ul>
 * <li>Startup : no</li>
 * <li>DependsOn : none</li>
 * <li>StartAtAppStart : not specified</li>
 * </ul
 *
 * The expected checkpoint phase startup behavior is:
 * <ul>
 * <li>FEATURES : initialized and constructed on first use; may reference other beans in application</li>
 * <li>BEFORE_APP_START : initialized on module start and constructed on first use; may reference other beans in application</li>
 * <li>AFTER_APP_START : initialized on module start and constructed on application start; may reference other beans in application</li>
 * </ul>
 *
 * Checkpoint causes behavior difference since not a startup bean. <p>
 *
 * Note: may reference any other bean in the application since this is not a startup bean;
 * will not be constructed until end of application start or first use.
 */
@Singleton
public class SGCheckpointBeanC implements CheckpointLocal {
    private static final String BEAN_NAME = SGCheckpointBeanC.class.getSimpleName();
    private static final Logger logger = Logger.getLogger(SGCheckpointBeanC.class.getName());

    static {
        logger.info(BEAN_NAME + ": static initializer");
        CheckpointStatistics.beanClassInitialized(BEAN_NAME, 1);
    }

    @EJB(beanName = "SGCheckpointBeanA")
    CheckpointLocal sgBeanA;

    @EJB(beanName = "SGCheckpointBeanB")
    CheckpointLocal sgBeanB;

    @EJB(beanName = "SGCheckpointBeanG")
    CheckpointLocal sgBeanG;

    @EJB(beanName = "SLCheckpointBeanA")
    CheckpointLocal slBeanA;

    @EJB(beanName = "SLCheckpointBeanB")
    CheckpointLocal slBeanB;

    @EJB(beanName = "SLCheckpointBeanC")
    CheckpointLocal slBeanC;

    @EJB(beanName = "SLCheckpointBeanG")
    CheckpointLocal slBeanG;

    public SGCheckpointBeanC() {
        logger.info(BEAN_NAME + ": constructor");
        CheckpointStatistics.incrementInstanceCount(BEAN_NAME);
    }

    @PostConstruct
    public void postConstruct() {
        logger.info(BEAN_NAME + ": postConstruct");
        CheckpointStatistics.incrementPostConstructCount(BEAN_NAME);
    }

    @Override
    public String getBeanName() {
        return BEAN_NAME;
    }

    @Override
    public void verify() {
        assertEquals("SGCheckpointBeanA", sgBeanA.getBeanName());
        assertEquals("SGCheckpointBeanB", sgBeanB.getBeanName());
        assertEquals("SGCheckpointBeanG", sgBeanG.getBeanName());

        assertEquals("SLCheckpointBeanA", slBeanA.getBeanName());
        assertEquals("SLCheckpointBeanB", slBeanB.getBeanName());
        assertEquals("SLCheckpointBeanC", slBeanC.getBeanName());
        assertEquals("SLCheckpointBeanG", slBeanG.getBeanName());
    }

}
