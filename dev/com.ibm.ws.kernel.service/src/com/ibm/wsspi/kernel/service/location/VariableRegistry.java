/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.location;

/**
 * A Service for resolving variables to their values. Variables may be predefined by the
 * runtime, or be read from bootstrap.properties or the server.xml configuration.
 * </p>
 */
public interface VariableRegistry {
    /** The variable literal for obtaining the server name. */
    String SERVER_NAME = "${wlp.server.name}";
    /** The variable literal for obtaining the server install location. */
    String INSTALL_DIR = "${wlp.install.dir}";
    /** The variable literal for obtaining the server user directory. */
    String USER_DIR = "${wlp.user.dir}";
    /** The variable literal for obtaining the usr product extension directory. */
    String USER_EXTENSION_DIR = "${usr.extension.dir}";
    /** The variable literal for obtaining the server configuration directory. */
    String SERVER_CONFIG_DIR = "${server.config.dir}";
    /** The variable literal for obtaining the server output directory. */
    String SERVER_OUTPUT_DIR = "${server.output.dir}";
    /** The variable literal for obtaining the shared applications directory. */
    String SHARED_APPS_DIR = "${shared.app.dir}";
    /** The variable literal for obtaining the shared configuration directory. */
    String SHARED_CONFIG_DIR = "${shared.config.dir}";
    /** The variable literal for obtaining the shared resources directory. */
    String SHARED_RESC_DIR = "${shared.resource.dir}";

    /**
     * Add a variable to the registry with the specified value if it does not exist already.
     * 
     * @param variable the name of the variable.
     * @param value the value of the variable.
     * @return true if it was added, false otherwise.
     */
    public boolean addVariable(String variable, String value);

    /**
     * Update the variable in the registry with the specified value. If it does not exist already this
     * will add the variable, if it already exists it will be overwritten.
     * 
     * @param variable the name of the variable.
     * @param value the value of the variable.
     */
    public void replaceVariable(String variable, String value);

    /**
     * Resolve the variables in the given string. This can be used either to resolve a string
     * that contains variables in it, such as <i>The server is called ${wlp.server.name}.</i> or
     * to specifically discover the value of a variable by wrapping it in ${ and } for example
     * <i>${wlp.server.name}</i>. If the variable is not defined then the variable substitution will
     * remain, so <i>${this.does.not.exist}</i> would return <i>${this.does.not.exist}</i>. During
     * variable resolution the value of the variable will be path normalized by this call. If path
     * normalization is not required use resolveRawString instead.
     * 
     * @param string the string to resolve.
     * @return the resolved string
     */
    public String resolveString(String string);

    /**
     * Resolve the variables in the given string. This can be used either to resolve a string
     * that contains variables in it, such as <i>The server is called ${wlp.server.name}.</i> or
     * to specifically discover the value of a variable by wrapping it in ${ and } for example
     * <i>${wlp.server.name}</i>. If the variable is not defined then the variable substitution will
     * remain, so <i>${this.does.not.exist}</i> would return <i>${this.does.not.exist}</i>. If path
     * normalization of variable values is required use resolveString instead.
     * 
     * @param string the string to resolve.
     * @return the resolved string
     */
    public String resolveRawString(String string);
    
    /**
     * Remove the specified variable from the registry.
     * 
     * @param variable
     */
    public void removeVariable(String variable);

}
