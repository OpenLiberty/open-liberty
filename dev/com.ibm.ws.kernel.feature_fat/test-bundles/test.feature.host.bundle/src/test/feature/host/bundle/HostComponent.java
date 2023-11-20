/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.feature.host.bundle;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 *
 */
@Component
public class HostComponent {

    @Activate
    public HostComponent() {
        System.out.println("TEST - HostComponent activated");
    }

    @Deactivate
    public void deactivate() {
        System.out.println("TEST - HostComponent deactivated");
    }
}
