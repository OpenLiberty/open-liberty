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

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * A stateless bean with the following configuration:
 * <ul>
 * <li>poolSize : not specified</li>
 * <li>StartAtAppStart : true</li>
 * </ul
 *
 * The expected checkpoint phase startup behavior is:
 * <ul>
 * <li>FEATURES : initialized on module start and constructed on first use; no pool preload</li>
 * <li>DEPLOYMENT : initialized on module start and constructed on first use; no pool preload</li>
 * <li>APPLICATIONS : initialized on module start and pool preloaded (50) on application start</li>
 * </ul>
 *
 * Checkpoint causes behavior difference since poolSize not specified. <p>
 *
 * Note: may reference any other bean in the application since bean will not be constructed
 * until end of application start or first use.
 */
@Stateless
public class SLCheckpointBeanC implements CheckpointLocal {
    private static final String BEAN_NAME = SLCheckpointBeanC.class.getSimpleName();
    private static final Logger logger = Logger.getLogger(SLCheckpointBeanC.class.getName());

    static {
        logger.info(BEAN_NAME + ": static initializer");
        CheckpointStatistics.beanClassInitialized(BEAN_NAME, 50);
    }

    @EJB(beanName = "SLCheckpointBeanA")
    CheckpointLocal slBeanA;

    @EJB(beanName = "SLCheckpointBeanB")
    CheckpointLocal slBeanB;

    @EJB(beanName = "SLCheckpointBeanG")
    CheckpointLocal slBeanG;

    public SLCheckpointBeanC() {
        logger.info(BEAN_NAME + ": constructor");
        CheckpointStatistics.incrementInstanceCount(BEAN_NAME);
    }

    @Override
    public String getBeanName() {
        return BEAN_NAME;
    }

    @Override
    public void verify() {
        assertEquals("SLCheckpointBeanA", slBeanA.getBeanName());
        assertEquals("SLCheckpointBeanB", slBeanB.getBeanName());
        assertEquals("SLCheckpointBeanG", slBeanG.getBeanName());
    }

}
