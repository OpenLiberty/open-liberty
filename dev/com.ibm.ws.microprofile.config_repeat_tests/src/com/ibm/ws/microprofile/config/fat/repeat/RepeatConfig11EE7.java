/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.fat.repeat;

import componenttest.rules.repeater.EE7FeatureReplacementAction;

/**
 *
 */
public class RepeatConfig11EE7 extends EE7FeatureReplacementAction {

    public static final String ID = "CONFIG11_EE7";

    public RepeatConfig11EE7(String server) {
        super();
        removeFeature("mpConfig-1.2");
        removeFeature("mpConfig-1.3");
        removeFeature("mpConfig-1.4");
        addFeature("mpConfig-1.1");
        forServers(server);
        withID(ID);
    }
}
