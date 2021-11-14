/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.custom.junit.runner;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.topology.impl.JavaInfo;

public class CheckpointSupportFilter extends Filter {

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
        if (desc.getAnnotation(SkipIfCheckpointNotSupported.class) != null
            || getTestClass(desc).getAnnotation(SkipIfCheckpointNotSupported.class) != null) {
            if (!JavaInfo.forCurrentVM().isCriuSupported()) {
                //This filter check is accurate if the fat framework and liberty server under test are running the
                //same jvm version. This simplifying assumption is already made elsewhere (e.g. the JavaLevelFilter).
                return false;
            }
        }
        return true;
    }

    @Override
    public String describe() {
        return null;
    }
}
