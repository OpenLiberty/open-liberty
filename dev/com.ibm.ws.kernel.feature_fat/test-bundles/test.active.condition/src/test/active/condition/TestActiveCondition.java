/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.active.condition;

import static com.ibm.ws.kernel.LibertyProcess.CONDITION_LIBERTY_PROCESS_ACTIVE;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.propertytypes.SatisfyingConditionTarget;
import org.osgi.service.condition.Condition;

@Component
@SatisfyingConditionTarget("(" + Condition.CONDITION_ID + "=" + CONDITION_LIBERTY_PROCESS_ACTIVE + ")")
public class TestActiveCondition {

    @Activate
    public void activate(BundleContext context) {
        int currentFWStartLevel = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkStartLevel.class).getStartLevel();
        int currentBundleStartLevel = context.getBundle().adapt(BundleStartLevel.class).getStartLevel();
        if (currentFWStartLevel == currentBundleStartLevel) {
            System.out.println("ACTIVE CONDITION - activate passed");
        } else {
            System.out.println("ACTIVE CONDITION - activate failed - wrong start level: " + currentFWStartLevel + "!=" + currentBundleStartLevel);
        }
    }

    @Deactivate
    public void deactivate(BundleContext context) {
        int currentFWStartLevel = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkStartLevel.class).getStartLevel();
        int currentBundleStartLevel = context.getBundle().adapt(BundleStartLevel.class).getStartLevel();
        Bundle systemBundle = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        if (systemBundle.getState() == Bundle.STOPPING) {
            if (currentFWStartLevel > currentBundleStartLevel) {
                System.out.println("ACTIVE CONDITION - deactivate passed");
            } else {
                System.out.println("ACTIVE CONDITION - deactivate failed: " + currentFWStartLevel + "<=" + currentBundleStartLevel);
            }
        } else {
            System.out.println("ACTIVE CONDITION - deactivate failed: Framework is not stopping");
        }
    }
}
