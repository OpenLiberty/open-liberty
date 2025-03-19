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
package componenttest.rules.repeater;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.JavaInfo;

public class CheckpointEE8Action extends EE8FeatureReplacementAction {
    public static final String ID = "EE8_FEATURES" + "_Checkpoint";

    public CheckpointEE8Action() {
        super();
        withID(ID);
    }

    @Override
    public String toString() {
        return "JavaEE8 FAT checkpoint repeat action (" + getID() + ")";
    }

    @Override
    public boolean isEnabled() {
        return JavaInfo.forCurrentVM().isCriuSupported() && checkEnabled();
    }

    public static boolean isActive() {
        return RepeatTestFilter.isRepeatActionActive(ID);
    }

}
