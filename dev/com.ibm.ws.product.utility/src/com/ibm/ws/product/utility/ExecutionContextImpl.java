/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ExecutionContextImpl implements ExecutionContext {

    private final CommandConsoleFacade console;

    private final String[] arguments;

    private final CommandTaskRegistry commandTaskRegistry;

    private final Map<String, Object> attributes = new HashMap<String, Object>();

    private final Map<String, String> optionNameValueMap = new HashMap<String, String>();

    public ExecutionContextImpl(CommandConsole console, String[] arguments, CommandTaskRegistry commandTaskRegistry) {
        this.console = new CommandConsoleFacade(console);
        this.arguments = arguments;
        this.commandTaskRegistry = commandTaskRegistry;
        parseArguments();
    }

    /** {@inheritDoc} */
    @Override
    public CommandConsole getCommandConsole() {
        return console;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getArguments() {
        return arguments;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getOptionNames() {
        return optionNameValueMap.keySet();
    }

    /** {@inheritDoc} */
    @Override
    public String getOptionValue(String option) {
        return optionNameValueMap.get(option);
    }

    /** {@inheritDoc} */
    @Override
    public boolean optionExists(String option) {
        return optionNameValueMap.containsKey(option);
    }

    /** {@inheritDoc} */
    @Override
    public CommandTaskRegistry getCommandTaskRegistry() {
        return commandTaskRegistry;
    }

    /** {@inheritDoc} */
    @Override
    public <T> T getAttribute(String name, Class<T> cls) {
        return cls.cast(attributes.get(name));
    }

    /** {@inheritDoc} */
    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /** {@inheritDoc} */
    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    private void parseArguments() {
        int index = 0;
        while (index < arguments.length) {
            String argument = arguments[index];
            String[] splits = argument.split("=");
            String optionName = splits[0];
            String optionValue = "";
            if (splits.length > 1) {
                optionValue = argument.substring(argument.indexOf("=") + 1);
            }
            optionNameValueMap.put(optionName, optionValue);
            index++;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setOverrideOutputStream(PrintStream outputStream) {
        console.setOverrideOutputStream(outputStream);
    }
}
