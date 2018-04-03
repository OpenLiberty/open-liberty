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
package com.ibm.ws.microprofile.config.fat.suite;

import componenttest.rules.repeater.EE7FeatureReplacementAction;

/**
 *
 */
public class RepeatConfig12EE7 extends EE7FeatureReplacementAction {

    public RepeatConfig12EE7(String server) {
        super();
        removeFeature("mpConfig-1.1");
        addFeature("mpConfig-1.2");
        forServers(server);
    }

}
