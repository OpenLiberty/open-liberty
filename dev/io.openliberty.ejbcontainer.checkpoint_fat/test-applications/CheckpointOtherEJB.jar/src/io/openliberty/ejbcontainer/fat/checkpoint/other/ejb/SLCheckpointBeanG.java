/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.ejbcontainer.fat.checkpoint.other.ejb;

import static org.junit.Assert.assertEquals;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import io.openliberty.ejbcontainer.fat.checkpoint.ejb.CheckpointLocal;
import io.openliberty.ejbcontainer.fat.checkpoint.ejb.CheckpointStatistics;

/**
 * A stateless bean with the following configuration:
 * <ul>
 * <li>poolSize : not specified</li>
 * <li>StartAtAppStart : not specified</li>
 * </ul
 *
 * The expected checkpoint phase startup behavior is:
 * <ul>
 * <li>FEATURES : initialized and constructed on first use; no pool preload</li>
 * <li>DEPLOYMENT : initialized on module start and constructed on first use; no pool preload</li>
 * <li>APPLICATIONS : initialized on module start and pool preloaded (50) on application start</li>
 * </ul>
 *
 * Checkpoint causes behavior difference since StartAtAppStart not specified. <p>
 *
 * Note: may reference any other bean in the application since bean will not be constructed
 * until end of application start or first use.
 */
@Stateless
public class SLCheckpointBeanG implements CheckpointLocal {
    private static final String BEAN_NAME = SLCheckpointBeanG.class.getSimpleName();
    private static final Logger logger = Logger.getLogger(SLCheckpointBeanG.class.getName());

    static {
        logger.info(BEAN_NAME + ": static initializer");
        CheckpointStatistics.beanClassInitialized(BEAN_NAME, 50);
    }

    @EJB(beanName = "SLCheckpointBeanA")
    CheckpointLocal slBeanA;

    @EJB(beanName = "SLCheckpointBeanH")
    CheckpointLocal slBeanH;

    @EJB(beanName = "SLCheckpointBeanI")
    CheckpointLocal slBeanI;

    public SLCheckpointBeanG() {
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
        assertEquals("SLCheckpointBeanH", slBeanH.getBeanName());
        assertEquals("SLCheckpointBeanI", slBeanI.getBeanName());
    }

}
