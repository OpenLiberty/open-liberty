/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package test.app.prereq;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service = WidgetEnabler.class, configurationPolicy = REQUIRE)
public class WidgetEnabler {
    @Activate
    public WidgetEnabler(Map<String, Object> props) {
        System.out.println("### created WidgetEnabler");
    }

    @Deactivate
    public void deactivate() {
        System.out.println("### deactivated WidgetEnabler");
    }

}