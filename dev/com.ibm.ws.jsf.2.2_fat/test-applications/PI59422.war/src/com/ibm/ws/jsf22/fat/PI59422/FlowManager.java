/*
 * Copyright (c)  2016  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.PI59422;

import javax.enterprise.context.Dependent;
import javax.inject.Named;

@Named
@Dependent
public class FlowManager {

    public void init() {
        System.out.println("PI59422: SampleFlow initialized.");
    }

    public void destroy() {
        System.out.println("PI59422: SampleFlow finalized.");
    }
}
