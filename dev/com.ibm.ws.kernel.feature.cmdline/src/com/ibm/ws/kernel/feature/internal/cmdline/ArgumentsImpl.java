/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.cmdline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.ibm.ws.kernel.boot.cmdline.Arguments;

/**
 *
 */
public class ArgumentsImpl implements Arguments {
    private final String _command;
    private final Map<String, String> _options = new HashMap<String, String>();
    private final List<String> _args = new ArrayList<String>();

    public ArgumentsImpl(String[] args) {
        if (args.length > 0) {
            _command = args[0];
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];

                if (arg.startsWith("--")) {
                    int index = arg.indexOf('=');
                    String value = "";
                    String key;
                    if (index != -1) {
                        key = arg.substring(2, index);
                        value = arg.substring(index + 1);
                    } else {
                        key = arg.substring(2);
                    }
                    _options.put(key.toLowerCase(Locale.ENGLISH), value);
                } else {
                    _args.add(args[i]);
                }
            }
        } else {
            _command = null;
        }
    }

    @Override
    public String getOption(String name) {
        return _options.get(name);
    }

    @Override
    public List<String> findInvalidOptions(List<String> expectedOptions) {

        List<String> processedEO = new ArrayList<String>();
        for (String op : expectedOptions) {
            if (op.startsWith("--")) {
                processedEO.add(op.substring(2).toLowerCase(Locale.ENGLISH));
            }
        }

        List<String> arguments = new ArrayList<String>(_options.keySet());
        arguments.removeAll(processedEO);

        return arguments;
    }

    @Override
    public List<String> getPositionalArguments() {
        return _args;
    }

    @Override
    public String getAction() {
        return _command;
    }
}