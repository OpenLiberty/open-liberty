/*
 * Copyright (c) 2015,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package test.iiop.server;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import test.iiop.common.LogMessages;

@Component(immediate = true)
public class FeatureCanary {
    @Activate
    protected void activate() {
        System.out.println(LogMessages.TEST_FEATURE_ACTIVATING);
    }

    @Deactivate
    protected void deactivate() {
        System.out.println(LogMessages.TEST_FEATURE_DEACTIVATING);
    }
}
