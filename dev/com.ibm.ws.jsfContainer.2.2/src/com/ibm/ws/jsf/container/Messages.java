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
package com.ibm.ws.jsf.container;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class Messages {
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("com.ibm.ws.jsf.container.resources.JSFContainerMessages");

    public static String get(final String key, String... inserts) {
        String message = BUNDLE.getString(key);
        if (inserts.length != 0)
            message = MessageFormat.format(message, (Object[]) inserts);
        return message;
    }
}
