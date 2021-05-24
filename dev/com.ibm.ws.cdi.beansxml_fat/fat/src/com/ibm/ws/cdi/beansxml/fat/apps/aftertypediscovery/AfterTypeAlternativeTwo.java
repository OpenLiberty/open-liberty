/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.fat.apps.aftertypediscovery;

import javax.enterprise.inject.Alternative;

@UseAlternative
@Alternative
public class AfterTypeAlternativeTwo implements AfterTypeAlternativeInterface {

    public static final String MSG = "alternative one";

    @Override
    public String getMsg() {
        return MSG;
    }
}
