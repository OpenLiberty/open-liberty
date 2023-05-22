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

package io.openliberty.ejbcontainer.fat.checkpoint.other.web;

import static org.junit.Assert.assertEquals;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import io.openliberty.ejbcontainer.fat.checkpoint.ejb.CheckpointLocal;
import io.openliberty.ejbcontainer.fat.checkpoint.ejb.CheckpointStatistics;

/**
 * A singleton bean with the following configuration:
 * <ul>
 * <li>Startup : no</li>
 * <li>DependsOn : dependency of T</li>
 * <li>StartAtAppStart : not specified</li>
 * <li>PostConstruct Transaction : NOT_SUPPORTED</li>
 * </ul
 *
 * The expected checkpoint phase startup behavior is:
 * <ul>
 * <li>INACTIVE : initialized and constructed on module start; may reference other beans in module</li>
 * <li>BEFORE_APP_START : initialized and constructed on module start; may reference other beans in module</li>
 * <li>AFTER_APP_START : initialized and constructed on module start; may reference other beans in module</li>
 * </ul>
 *
 * Checkpoint causes no behavior difference due to the dependency from a startup bean. <p>
 *
 * Note: may only reference other beans in the same module since constructed at module start.
 */
@Singleton
public class SGCheckpointBeanS implements CheckpointLocal {
    private static final String BEAN_NAME = SGCheckpointBeanS.class.getSimpleName();
    private static final Logger logger = Logger.getLogger(SGCheckpointBeanS.class.getName());

    static {
        logger.info(BEAN_NAME + ": static initializer");
        CheckpointStatistics.beanClassInitialized(BEAN_NAME, 1);
    }

    @EJB(beanName = "SLCheckpointBeanS")
    CheckpointLocal slBeanS;

    @EJB(beanName = "SLCheckpointBeanT")
    CheckpointLocal slBeanT;

    @EJB(beanName = "SLCheckpointBeanU")
    CheckpointLocal slBeanU;

    public SGCheckpointBeanS() {
        logger.info(BEAN_NAME + ": constructor");
        CheckpointStatistics.incrementInstanceCount(BEAN_NAME);
    }

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
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
        assertEquals("SLCheckpointBeanS", slBeanS.getBeanName());
        assertEquals("SLCheckpointBeanT", slBeanT.getBeanName());
        assertEquals("SLCheckpointBeanU", slBeanU.getBeanName());
    }

}
