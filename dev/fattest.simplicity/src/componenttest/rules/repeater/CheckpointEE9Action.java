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

/**
 * Test repeat action with ID suffixed with _Checkpoint:
 * <ol>
 * <li>Invoke the Jakarta transformer on all war/ear files under the autoFVT/publish/ folder</li>
 * <li>Update all server.xml configs under the autoFVT/publish/ folder to use EE 9 features</li>
 * </ol>
 */
public class CheckpointEE9Action extends JakartaEE9Action {
    public static final String ID = EE9_ACTION_ID + "_Checkpoint";

    public CheckpointEE9Action() {
        super();
        withID(ID);
    }

    @Override
    public String toString() {
        return "JakartaEE9 FAT checkpoint repeat action (" + getID() + ")";
    }

    @Override
    public boolean isEnabled() {
        return JavaInfo.forCurrentVM().isCriuSupported() && checkEnabled();
    }

    public static boolean isActive() {
        return RepeatTestFilter.isRepeatActionActive(ID);
    }
}
