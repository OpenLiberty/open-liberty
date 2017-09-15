/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.metagen;

import java.util.HashMap;
import java.util.Map;

/**
 * Calls to this class should only be used by the Ant target generateMetatype
 * for generating metatype at build time.
 * 
 * @see <a href="http://was.pok.ibm.com/xwiki/bin/view/LibertyCookbook/Metatype-Generator">Metatype Generator Documentation</a>
 */
public class BuildTimeMetaGen {
    public static void main(String[] args) throws Exception {
        Map<String, Object> props = new HashMap<String, Object>();

        if (args.length == 1) {
            args = args[0].split(";");

            for (String arg : args) {
                arg = arg.trim();
                if (!arg.isEmpty()) {
                    String[] split = arg.split("=");
                    if (!split[1].startsWith("${metagen."))
                        props.put(split[0], split[1]);
                }
            }
        } else
            throw new IllegalArgumentException("Invalid number of arguments");

        //TODO fix
        if (MetatypeGenerator.generateMetatype(props, null) == null)
            System.exit(-1);
    }
}
