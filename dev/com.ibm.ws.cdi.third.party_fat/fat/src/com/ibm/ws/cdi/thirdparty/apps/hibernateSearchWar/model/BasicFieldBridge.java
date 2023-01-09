/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.cdi.thirdparty.apps.hibernateSearchWar.model;

import org.hibernate.search.bridge.builtin.StringBridge;

import com.ibm.ws.cdi.thirdparty.apps.hibernateSearchWar.web.HibernateSearchTestServlet;

//This class just need to exist to trigger some hibernate codepaths
public class BasicFieldBridge extends StringBridge {

    private static int i = 0;

    public String objectToString (Object o) {
        HibernateSearchTestServlet.registerFieldBridgeCalled();
        i++;
        return ""+i;
    }

    public Object stringToObject (String o) {
        HibernateSearchTestServlet.registerFieldBridgeCalled();
        return new Integer(i);
    }

}
