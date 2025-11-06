/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.concurrent.no.vt.disabler;

import java.util.Map;

import com.ibm.wsspi.threading.ThreadTypeOverride;

/**
 * Disables Liberty's creation of virtual threads.
 */
@SuppressWarnings("deprecation")
public class VirtualThreadDisabler implements ThreadTypeOverride {

    protected void activate(Map<String, Object> properties) {
        System.out.println("VirtualThreadDisabler activated");
    }

    public boolean allowVirtualThreadCreation() {
        System.out.println("VirtualThreadDisabler.allowVirtualThreadCreation: false");
        return false;
    }

    protected void deactivate() {
        System.out.println("VirtualThreadDisabler dectivated");
    }
}