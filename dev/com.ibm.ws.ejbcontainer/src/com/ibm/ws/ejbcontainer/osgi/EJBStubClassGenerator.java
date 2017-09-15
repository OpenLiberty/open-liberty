/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi;

import java.util.Set;

public interface EJBStubClassGenerator {

    /**
     * Returns a set of Remote classes for which the dynamically generated
     * stub classes need to be compatible with RMIC generated stubs and ties
     * for the specified application. <p>
     *
     * @param appName application name
     * @return the RMIC compatible class names; or an empty set if none exist.
     */
    Set<String> getRMICCompatibleClasses(String appName);

    /**
     * Add a set of Remote classes for which the dynamically generated stub
     * classes need to be compatible with RMIC generated stubs and ties
     * when accessed by the specified classloader.
     *
     * In tWAS, pre-EJB 3 modules are processed by ejbdeploy, and rmic is
     * used to generate stubs for remote home and interface classes. These
     * stubs need to exist so that we do not dynamically generate stubs that
     * use the "WAS EJB 3" marshalling rules.
     *
     * In Liberty, there is no separate deploy step, so we need to ensure
     * that stubs for pre-EJB 3 modules are generated with as much
     * compatibility with RMIC as we can.
     *
     * @param loader application classloader for which the RMIC compatible stubs are required
     * @param rmicCompatibleClasses the RMIC compatible class names
     */
    void addRMICCompatibleClasses(ClassLoader loader, Set<String> rmicCompatibleClasses);
}
