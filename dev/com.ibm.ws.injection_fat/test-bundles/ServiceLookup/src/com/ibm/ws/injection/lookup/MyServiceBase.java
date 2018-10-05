/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.lookup;

public abstract class MyServiceBase implements MyService {

    private final String osgiName;
    private final int ranking;

    public MyServiceBase(String osgiName, int ranking) {
        this.osgiName = osgiName;
        this.ranking = ranking;
    }

    @Override
    public String run() {
        return "successfully ran MyService that has osgi name of " + osgiName + " and ranking of " + ranking;
    }
}
