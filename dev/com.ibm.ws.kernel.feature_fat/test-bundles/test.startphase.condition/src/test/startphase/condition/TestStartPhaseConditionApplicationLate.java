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

import static com.ibm.wsspi.kernel.service.condition.StartPhaseCondition.APPLICATION_LATE_FILTER;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.condition.Condition;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;

@Component
public class TestStartPhaseConditionApplicationLate extends AbstractTestStartPhaseCondition {

    @Activate
    public TestStartPhaseConditionApplicationLate(@Reference(service = Condition.class, target = APPLICATION_LATE_FILTER) ServiceReference<Condition> servicesEarlyCondition, //
                                                  @Reference(service = LoggerFactory.class) Logger logger) {
        super(servicesEarlyCondition, "APPLICATION_LATE", "APPLICATION", logger);
        checkStartPhaseCondition(true);
    }

    @Override
    @Deactivate
    public void deactivate() {
        checkStartPhaseCondition(false);
    }
}
