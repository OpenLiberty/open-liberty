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

package com.ibm.ws.ejbcontainer.security.test;

/**
 * Interface for Enterprise Bean
 */
public interface SecurityEJBInterface {

    public abstract String denyAll();

    public abstract String denyAll(String input);

    public abstract String permitAll();

    public abstract String permitAll(String input);

    public abstract String checkAuthenticated();

    public abstract String permitAuthenticated();

    public abstract String manager();

    public abstract String manager(String input);

    public abstract String employee();

    public abstract String employee(String input);

    public abstract String employeeAndManager();

    public abstract String employeeAndManager(String input);

    public abstract String employeeAndManager(int i);

    public abstract String employeeAndManager(String i1, String i2);

    public abstract String declareRoles01();

    public abstract String runAsClient();

    public abstract String runAsSpecified();

}