/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.security.fat.common.utils;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonTest;

public class OSSkipRules extends CommonTest {

    protected static Class<?> thisClass = OSSkipRules.class;

    private static boolean skipIfOS(OperatingSystem osToCheck) {

        OperatingSystem theOS = null;
        try {
            theOS = Machine.getLocalMachine().getOperatingSystem();
            Log.info(thisClass, "skipIfOS", "Current OS is: " + theOS);
        } catch (Exception e) {
            Log.info(thisClass, "skipIfOS", "Exception encountered trying to determine operating system: " + e.getMessage());
            Log.info(thisClass, "skipIfOS", "Assuming this is the OS that the caller requested to be skip.");
            testSkipped();
            return true;
        }
        if (osToCheck == theOS) {
            Log.info(thisClass, "skipIfOS", "NOT Running on " + osToCheck + " - Skip test");
            testSkipped();
            return true;
        }

        Log.info(thisClass, "skipIfOS", "Running on " + osToCheck + " - Run test");
        return false;

    }

    public static class SkipIfAIX extends MySkipRule {

        @Override
        public Boolean callSpecificCheck() {

            return skipIfOS(OperatingSystem.AIX);

        }
    }

    public static class SkipIfHP extends MySkipRule {

        @Override
        public Boolean callSpecificCheck() {

            return skipIfOS(OperatingSystem.HP);

        }
    }

    public static class SkipIfLinux extends MySkipRule {

        @Override
        public Boolean callSpecificCheck() {

            return skipIfOS(OperatingSystem.LINUX);

        }
    }

    public static class SkipIfISeries extends MySkipRule {

        @Override
        public Boolean callSpecificCheck() {

            return skipIfOS(OperatingSystem.ISERIES);

        }
    }

    public static class SkipIfMac extends MySkipRule {

        @Override
        public Boolean callSpecificCheck() {

            return skipIfOS(OperatingSystem.MAC);
        }
    }

    public static class SkipIfSolaris extends MySkipRule {

        @Override
        public Boolean callSpecificCheck() {

            return skipIfOS(OperatingSystem.SOLARIS);
        }
    }

    public static class SkipIfWindows extends MySkipRule {

        @Override
        public Boolean callSpecificCheck() {

            return skipIfOS(OperatingSystem.WINDOWS);
        }
    }

    public static class SkipIfZOS extends MySkipRule {

        @Override
        public Boolean callSpecificCheck() {

            return skipIfOS(OperatingSystem.ZOS);
        }
    }
}
