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
 * Interface with methods to verify nested EJB injections.
 **/
public interface EJBNested {
    /**
     * Add level*10^(level-1)
     * e.g. Nested3 adds 300 (3*10^2)
     **/
    public int addField(int total);

    /**
     * Add (level*10^(level-1))*2
     * e.g. Nested3 adds 600 ((3*10^2)*2)
     **/
    public int addMethod(int total);
}
