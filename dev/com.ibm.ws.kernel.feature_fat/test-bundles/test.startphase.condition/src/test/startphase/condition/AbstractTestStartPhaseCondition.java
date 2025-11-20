/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.startphase.condition;

import static com.ibm.wsspi.kernel.service.condition.StartPhaseCondition.ID_NAME;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.condition.Condition;
import org.osgi.service.log.Logger;

public abstract class AbstractTestStartPhaseCondition {
    private final ServiceReference<Condition> startPhaseCondition;
    private final Logger logger;
    private final String expectedActivateStartPhase;
    private final String expectedDeactivateStartPhase;

    protected AbstractTestStartPhaseCondition(ServiceReference<Condition> startPhaseCondition, String expectedActivateStartPhase, String expectedDeactivateStartPhase,
                                              Logger logger) {
        // The start-phase that causes activation is always different than the one that causes it to deactivate.
        // This is because the start-phase that causes activation is one that matches the filter.
        // The one that causes it to deactivate will not match the filter.  These two start-phases cannot be the same.
        this.startPhaseCondition = startPhaseCondition;
        this.expectedActivateStartPhase = expectedActivateStartPhase;
        this.expectedDeactivateStartPhase = expectedDeactivateStartPhase;
        this.logger = logger;
    }

    private Enum<?> getStartPhase() {
        return (Enum<?>) startPhaseCondition.getProperty(ID_NAME);
    }

    @SuppressWarnings("unchecked")
    protected void checkStartPhaseCondition(boolean onActivate) {
        String testPrefix = onActivate ? "ON_ACTIVATE" : "ON_DEACTIVATE";
        String expectedStartPhase = onActivate ? expectedActivateStartPhase : expectedDeactivateStartPhase;
        Enum<?> startPhase = getStartPhase();
        if (startPhase.equals(Enum.valueOf(startPhase.getClass(), expectedStartPhase))) {
            logger.audit(testPrefix + " TESTING StartPhaseCondition: {} - PASSED", expectedActivateStartPhase);
        } else {
            logger.audit(testPrefix + " TESTING StartPhaseCondition: {} - FAILED - phase={}", expectedActivateStartPhase, startPhase);
        }
    }

    @Deactivate
    public void deactivate() {
        checkStartPhaseCondition(false);
    }
}
