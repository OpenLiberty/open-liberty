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
package com.ibm.ws.concurrent.mp.fat;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;

public class MPContextProp11RepeatAction extends FeatureReplacementAction {

    public static final String ID = "MP_CONTEXT_PROP_11";

    public MPContextProp11RepeatAction(String server) {
        super();
        addFeature("mpContextPropagation-1.1");
        addFeature("mpConfig-2.0");
        removeFeature("mpContextPropagation-1.0");
        removeFeature("mpConfig-1.3");
        removeFeature("mpConfig-1.4");
        withID(ID);
        withTestMode(TestMode.FULL);
        forServers(server);
    }

}
