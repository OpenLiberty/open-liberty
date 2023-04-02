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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package bells;

import java.util.Map;

import test.checkpoint.bells.bundle.TestInterface;

public class TestInterfaceImpl implements TestInterface {

    Map<String, String> updatedBellProps = null;

    public TestInterfaceImpl() {
        System.out.println(getClass() + " is being consumed by a service consumer using getService().");
    }

    public void updateBell(Map<String, String> ubProps) {
        updatedBellProps = ubProps;
        System.out.println("Updated bell properties: " + updatedBellProps);
    }
}
