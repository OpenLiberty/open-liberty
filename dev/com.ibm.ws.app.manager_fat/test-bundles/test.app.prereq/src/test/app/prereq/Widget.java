/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.app.prereq;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.util.Map;

import com.ibm.wsspi.application.lifecycle.ApplicationPrereq;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Obviously, a widget can't work without a doodad.
 */

@Component(configurationPolicy = REQUIRE, immediate = true)
public class Widget implements ApplicationPrereq {
    final String id;

    @Activate
    public Widget(Map<String, Object> props, @Reference WidgetEnabler enabler) {
        id = (String) props.getOrDefault("id", "ERROR: we didn't find an id in the properties for Widget");
        System.out.println("### Widget[" + id + "] created");
    }

    public String getApplicationPrereqID() { return id; }
}