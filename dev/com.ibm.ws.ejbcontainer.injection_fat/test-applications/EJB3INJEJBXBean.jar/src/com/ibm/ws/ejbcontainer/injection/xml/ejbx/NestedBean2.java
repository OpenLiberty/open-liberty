/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.xml.ejbx;

/**
 * Basic Stateless Bean implementation for testing nested EJB Injections via XML
 **/
public class NestedBean2 {
    private EJBNested SLFieldNested3;
    private EJBNested SLMethodNested3;

    @SuppressWarnings("unused")
    private void setSLMethodNested3(EJBNested ejb) {
        SLMethodNested3 = ejb;
    }

    /**
     * Add level*10^(level-1)
     * e.g. Nested3 adds 300 (3*10^2)
     **/
    public int addField(int total) {
        total = total + 20;

        try {
            total = SLFieldNested3.addField(total);
        } catch (Throwable t) {
            t.printStackTrace(System.out);
        }

        return total;
    }

    /**
     * Add (level*10^(level-1))*2
     * e.g. Nested3 adds 600 ((3*10^2)*2)
     **/
    public int addMethod(int total) {
        total = total + 40;

        try {
            total = SLMethodNested3.addMethod(total);
        } catch (Throwable t) {
            t.printStackTrace(System.out);
        }

        return total;
    }

    public NestedBean2() {
        // intentionally blank
    }
}
