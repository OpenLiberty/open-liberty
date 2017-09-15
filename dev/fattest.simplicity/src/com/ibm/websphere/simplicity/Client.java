/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity;

import com.ibm.websphere.simplicity.exception.NotImplementedException;
import componenttest.topology.impl.LibertyClient;

/**
 * This abstract class is the parent for all Clients in the Topology. This includes application
 * Clients, web Clients, and generic Clients.
 */
public abstract class Client extends Scope {

    private static final Class c = Client.class;

    protected LibertyClient instance;
    protected Node node = null;

    /**
     * Constructor to create a new Client
     * 
     * @param cell The {@link Cell} that this Client belongs to
     * @param node The {@link Node} that the Client belongs to
     * @param Client The underlying Client instance
     */
    protected Client(Cell cell, Node node, componenttest.topology.impl.LibertyClient client) throws Exception {
        super(node, cell);
        this.node = node;
        this.instance = client;
    }

    @Override
    public String getObjectNameFragment() {
        return this.node.getObjectNameFragment() + ",process=" + this.getName();
    }

    public componenttest.topology.impl.LibertyClient getBackend() {
        return this.instance;
    }

    /**
     * Translates a predefined variable into its corresponding string value.
     * 
     * @param variable The variable to translate.
     * @return The value of the variable, or null if it does not exist.
     * @throws Exception
     */
    public String expandVariable(VariableType variable) throws Exception {
        return expandVariable(variable.getValue());
    }

    /**
     * Translates a custom variable into its corresponding string value.
     * 
     * @param variable The variable name to translate.
     * @return The value of the variable, or null if it does not exist.
     * @throws Exception
     */
    public String expandVariable(String variable) throws Exception {
        // Remove the standard prefix & postfix, if any
        variable = variable.replace("${", "").replace("$(", "").replace("}", "").replace(")", "");
        throw new NotImplementedException();
    }

    /**
     * Recursively expands all variables in the string to the corresponding value
     * from the Client, and replaces backslashes with forward slashes. The
     * resulting string does not contain any variables.
     * 
     * @param str A string that contains zero or more variables.
     * @return The fully-expanded version of the string.
     * @throws Exception
     */
    public String expandString(String str) throws Exception {
        return Scope.expandString(this, str);
    }
}