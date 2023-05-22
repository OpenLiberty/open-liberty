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

package io.openliberty.ejbcontainer.fat.checkpoint.other.ejb;

import static org.junit.Assert.assertEquals;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;

import io.openliberty.ejbcontainer.fat.checkpoint.ejb.CheckpointLocal;
import io.openliberty.ejbcontainer.fat.checkpoint.ejb.CheckpointStatistics;

/**
 * A singleton bean with the following configuration:
 * <ul>
 * <li>Startup : no</li>
 * <li>DependsOn : none</li>
 * <li>StartAtAppStart : true</li>
 * </ul
 *
 * The expected checkpoint phase startup behavior is:
 * <ul>
 * <li>FEATURES : initialized on module start and constructed on first use; may reference other beans in application</li>
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
public class SGCheckpointBeanJ implements CheckpointLocal {
    private static final String BEAN_NAME = SGCheckpointBeanJ.class.getSimpleName();
    private static final Logger logger = Logger.getLogger(SGCheckpointBeanJ.class.getName());

    static {
        logger.info(BEAN_NAME + ": static initializer");
        CheckpointStatistics.beanClassInitialized(BEAN_NAME, 1);
    }

    @EJB(beanName = "SGCheckpointBeanA")
    CheckpointLocal sgBeanA;

    @EJB(beanName = "SGCheckpointBeanG")
    CheckpointLocal sgBeanG;

    @EJB(beanName = "SGCheckpointBeanH")
    CheckpointLocal sgBeanH;

    @EJB(beanName = "SLCheckpointBeanA")
    CheckpointLocal slBeanA;

    @EJB(beanName = "SLCheckpointBeanG")
    CheckpointLocal slBeanG;

    @EJB(beanName = "SLCheckpointBeanH")
    CheckpointLocal slBeanH;

    @EJB(beanName = "SLCheckpointBeanI")
    CheckpointLocal slBeanI;

    public SGCheckpointBeanJ() {
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
        assertEquals("SGCheckpointBeanG", sgBeanG.getBeanName());
        assertEquals("SGCheckpointBeanH", sgBeanH.getBeanName());

        assertEquals("SLCheckpointBeanA", slBeanA.getBeanName());
        assertEquals("SLCheckpointBeanG", slBeanG.getBeanName());
        assertEquals("SLCheckpointBeanH", slBeanH.getBeanName());
        assertEquals("SLCheckpointBeanI", slBeanI.getBeanName());
    }

}
