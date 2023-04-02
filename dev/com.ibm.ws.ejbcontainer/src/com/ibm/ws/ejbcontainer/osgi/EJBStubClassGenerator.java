/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.osgi;

import java.util.Set;

public interface EJBStubClassGenerator {

    /**
     * Returns a set of Remote classes for which the dynamically generated
     * stub classes need to be compatible with RMIC generated stubs and ties
     * for the specified application. <p>
     *
     * Starting with Jakarta EE 9 / Enterprise Beans 4.0 all remote bean
     * interfaces are RMIC compatible so an application at EE 9 or later
     * should not use this method; UnsupportedOperationException will be thrown.
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
     * Starting with Jakarta EE 9 / Enterprise Beans 4.0 all remote bean
     * interfaces are RMIC compatible so an application at EE 9 or later
     * should not use this method; UnsupportedOperationException will be thrown.
     *
     * @param loader                application classloader for which the RMIC compatible stubs are required
     * @param rmicCompatibleClasses the RMIC compatible class names
     */
    void addRMICCompatibleClasses(ClassLoader loader, Set<String> rmicCompatibleClasses);
}
