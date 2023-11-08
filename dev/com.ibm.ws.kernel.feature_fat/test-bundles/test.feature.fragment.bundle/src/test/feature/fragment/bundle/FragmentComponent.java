/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.feature.fragment.bundle;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 *
 */
@Component
public class FragmentComponent {

    @Activate
    public FragmentComponent(/* @Reference FeatureProvisioner featureProvisioner */) {
        System.out.println("TEST - FragmentComponent activated");
    }

    @Deactivate
    public void deactivate() {
        System.out.println("TEST - FragmentComponent deactivated");
    }
}
