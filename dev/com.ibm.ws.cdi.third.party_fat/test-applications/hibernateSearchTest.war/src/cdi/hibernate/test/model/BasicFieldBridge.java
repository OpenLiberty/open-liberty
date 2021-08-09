/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.hibernate.test.model;

import org.hibernate.search.bridge.builtin.StringBridge;
import cdi.hibernate.test.web.SimpleTestServlet;

//This class just need to exist to trigger some hibernate codepaths
public class BasicFieldBridge extends StringBridge {

    private static int i = 0;

    public String objectToString (Object o) {
        SimpleTestServlet.registerFieldBridgeCalled();
        i++;
        return ""+i;
    }

    public Object stringToObject (String o) {
        SimpleTestServlet.registerFieldBridgeCalled();
        return new Integer(i);
    }

}
