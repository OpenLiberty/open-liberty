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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/


package com.ibm.ws.wssecurity.fat.utils.common;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.actions.LargeProjectRepeatActions;

import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.rules.repeater.RepeatActions.EEVersion;
import componenttest.topology.impl.JavaInfo;

/**
 *
 */
public class WSSecuritySamlRepeatActions extends LargeProjectRepeatActions {
    
    public static RepeatTests createEE9OrEE10WSSecSamlRepeats(String cbhVersion) {
        boolean doIDPTransform = true;
        RepeatTests rTests = null;

        OperatingSystem currentOS = null;
        try {
            currentOS = Machine.getLocalMachine().getOperatingSystem();
        } catch (Exception e) {
            Log.info(thisClass, "createEE9OrEE10WSSecSamlRepeats", "Encountered and exception trying to determine OS type - assume we'll need to run: " + e.getMessage());
        }
        Log.info(thisClass, "createEE9OrEE10WSSecSamlRepeats", "OS: " + currentOS.toString());

        if (OperatingSystem.WINDOWS == currentOS) {
            Log.info(thisClass, "createEE9OrEE10WSSecSamlRepeats", "Enabling the default EE7/EE8 test instance since we're running on Windows");
            rTests = addRepeat(rTests, new EmptyAction());
        } else {
            if (JavaInfo.forCurrentVM().majorVersion() > 8) {
                if (TestModeFilter.FRAMEWORK_TEST_MODE == TestMode.LITE) {
                    Log.info(thisClass, "createEE9OrEE10WSSecSamlRepeats", "Enabling the EE9 test instance (Not on Windows, Java > 8, Lite Mode)");
                    rTests = addRepeat(rTests, adjustFeatures(JakartaEE9Action.ID, null, null, null, null));
                    if (doIDPTransform) {
                        idpWarTransform(EEVersion.EE9);
                    }
                } else {
                    Log.info(thisClass, "createEE9OrEE10WSSecSamlRepeats", "Enabling the EE10 test instance (Not on Windows, Java > 8, FULL Mode)");
                    rTests = addRepeat(rTests, adjustFeatures(JakartaEE10Action.ID, null, null, null, null));
                    rTests = repeatWithCBH(rTests, cbhVersion);
                    if (doIDPTransform) {
                        idpWarTransform(EEVersion.EE10);
                    }
                }
            } else {
                Log.info(thisClass, "createEE9OrEE10WSSecSamlRepeats", "Enabling the default EE7/EE8 test instance (Not on Windows, Java = 8, any Mode)");
                rTests = addRepeat(rTests, new EmptyAction());
                rTests = repeatWithCBH(rTests, cbhVersion);
            } 
        }

        return rTests;
    }
    
    public static RepeatTests repeatWithCBH(RepeatTests rTests, String cbhVersion) {
        if("cbh10".equals(cbhVersion)) {
            Log.info(thisClass, "createEE9OrEE10WSSecSamlRepeats", "Enabling the RepeatWithEE7cbh10 test instance (Not on Windows, Java >= 8, any Mode)");
            rTests = addRepeat(rTests, new RepeatWithEE7cbh10());
        } else if ("cbh20".equals(cbhVersion)) {
            Log.info(thisClass, "createEE9OrEE10WSSecSamlRepeats", "Enabling the RepeatWithEE7cbh20 test instance (Not on Windows, Java >= 8, any Mode)");
            rTests = addRepeat(rTests, new RepeatWithEE7cbh20()); 
        } else if("both".equals(cbhVersion)) {            
            if (TestModeFilter.FRAMEWORK_TEST_MODE == TestMode.LITE) {
                Log.info(thisClass, "createEE9OrEE10WSSecSamlRepeats", "Enabling the RepeatWithEE7cbh10 test instance (Not on Windows, Java >= 8, lite Mode)");
                rTests = addRepeat(rTests, new RepeatWithEE7cbh10());
            } else {
                Log.info(thisClass, "createEE9OrEE10WSSecSamlRepeats", "Enabling the RepeatWithEE7cbh10 test instance (Not on Windows, Java >= 8, full Mode)");
                rTests = addRepeat(rTests, new RepeatWithEE7cbh20()); 
            }           
        }
        return rTests;
    }

}
