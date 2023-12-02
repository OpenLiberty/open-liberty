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
package componenttest.custom.junit.runner;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.CheckpointTest;
import componenttest.topology.impl.JavaInfo;

public class CheckpointSupportFilter extends Filter {

    private static final String CHECKPOINT_ONLY_PROPERTY_NAME = "fat.test.run.checkpoint.only";
    private static final boolean CHECKPOINT_ONLY_PROPERTY_VALUE;

    static {
        CHECKPOINT_ONLY_PROPERTY_VALUE = Boolean.valueOf(System.getProperty(CHECKPOINT_ONLY_PROPERTY_NAME));

        Log.info(CheckpointSupportFilter.class, "<clinit>", "System property: " + CHECKPOINT_ONLY_PROPERTY_NAME + " is " + CHECKPOINT_ONLY_PROPERTY_VALUE);
    }

    private static Class<?> getTestClass(Description desc) {
        try {
            return Class.forName(desc.getClassName(), false, CheckpointSupportFilter.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldRun(Description desc) {
        CheckpointTest checkpointTest = desc.getAnnotation(CheckpointTest.class);
        if (checkpointTest == null) {
            checkpointTest = getTestClass(desc).getAnnotation(CheckpointTest.class);
        }

        // A system property is set on systems that support InstantOn and want
        // to only run Checkpoint Tests.
        if (CHECKPOINT_ONLY_PROPERTY_VALUE) {
            return checkpointTest != null;
        }

        if (checkpointTest != null) {
            //This filter check is accurate if the fat framework and liberty server under test are running the
            //same jvm version. This simplifying assumption is already made elsewhere (e.g. the JavaLevelFilter).
            return checkpointTest.alwaysRun() || JavaInfo.forCurrentVM().isCriuSupported();
        }
        return true;
    }

    @Override
    public String describe() {
        return null;
    }
}
