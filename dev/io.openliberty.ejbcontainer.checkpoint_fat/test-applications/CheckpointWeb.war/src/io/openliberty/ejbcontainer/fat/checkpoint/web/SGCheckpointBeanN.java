/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

package io.openliberty.ejbcontainer.fat.checkpoint.web;

import static org.junit.Assert.assertEquals;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import io.openliberty.ejbcontainer.fat.checkpoint.ejb.CheckpointLocal;
import io.openliberty.ejbcontainer.fat.checkpoint.ejb.CheckpointStatistics;

/**
 * A singleton bean with the following configuration:
 * <ul>
 * <li>Startup : yes</li>
 * <li>DependsOn : M</li>
 * <li>StartAtAppStart : not specified</li>
 * <li>PostConstruct Transaction : BEAN</li>
 * </ul
 *
 * The expected checkpoint phase startup behavior is:
 * <ul>
 * <li>INACTIVE : initialized and constructed on module start; may reference other beans in module</li>
 * <li>BEFORE_APP_START : initialized and constructed on module start; may reference other beans in module</li>
 * <li>AFTER_APP_START : initialized and constructed on module start; may reference other beans in module</li>
 * </ul>
 *
 * Checkpoint causes no behavior difference because of the use of `@Startup`. <p>
 *
 * Note: may only reference other beans in the same module since constructed at module start.
 */
@Startup
@Singleton
@DependsOn("SGCheckpointBeanM")
@TransactionManagement(TransactionManagementType.BEAN)
public class SGCheckpointBeanN implements CheckpointLocal {
    private static final String BEAN_NAME = SGCheckpointBeanN.class.getSimpleName();
    private static final Logger logger = Logger.getLogger(SGCheckpointBeanN.class.getName());

    static {
        logger.info(BEAN_NAME + ": static initializer");
        CheckpointStatistics.beanClassInitialized(BEAN_NAME, 1);
    }

    @EJB(beanName = "SGCheckpointBeanM")
    CheckpointLocal sgBeanM;

    @EJB(beanName = "SLCheckpointBeanM")
    CheckpointLocal slBeanM;

    @EJB(beanName = "SLCheckpointBeanN")
    CheckpointLocal slBeanN;

    @EJB(beanName = "SLCheckpointBeanO")
    CheckpointLocal slBeanO;

    public SGCheckpointBeanN() {
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
        assertEquals("SGCheckpointBeanM", sgBeanM.getBeanName());

        assertEquals("SLCheckpointBeanM", slBeanM.getBeanName());
        assertEquals("SLCheckpointBeanN", slBeanN.getBeanName());
        assertEquals("SLCheckpointBeanO", slBeanO.getBeanName());
    }

}
