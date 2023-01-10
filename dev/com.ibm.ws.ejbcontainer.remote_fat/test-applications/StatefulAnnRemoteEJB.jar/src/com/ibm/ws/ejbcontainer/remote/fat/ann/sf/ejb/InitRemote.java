/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb;

/**
 * Remote business interface for Stateful Session bean for testing
 * Init/ejbCreate methods.
 **/
public interface InitRemote {
    /**
     * Default initialize method with no parameters.
     * <p>
     */
    public void initialize();

    /**
     * Default initialize method with one parameter.
     * <p>
     */
    public void initialize(String string);

    /**
     * Custom initialize method with no parameters.
     * <p>
     */
    public void initializeDefault();

    /**
     * Custom initialize method with one parameter.
     * <p>
     */
    public void initializeBasic(String string);

    /**
     * Custom initialize method with one parameter, different type.
     * <p>
     */
    public void initializeBasic(int integer);

    /**
     * Custom initialize method with two parameters.
     * <p>
     */
    public void initializeAdv(String string, int integer);

    /** Return the String value state of Stateful bean. **/
    public String getString();

    /** Return the int value state of Stateful bean. **/
    public int getInt();

    /** Designated 'Remove' method. **/
    public String finish(String string, int integer);
}
