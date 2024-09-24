/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

package test.checkpoint.config.bundle;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.propertytypes.SatisfyingConditionTarget;
import org.osgi.service.condition.Condition;

import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 *
 */
@Component
@SatisfyingConditionTarget("(" + Condition.CONDITION_ID + "=" + CheckpointPhase.CONDITION_BEFORE_CHECKPOINT_ID + ")")
public class TestBeforeCheckpointComponent {
    @Activate
    public TestBeforeCheckpointComponent(ComponentContext cc) {
        System.out.println("TESTING - activate before checkpoint condition component");
    }

    @Deactivate
    public void deactivate() {
        System.out.println("TESTING - deactivate before checkpoint condition component");
    }
}
