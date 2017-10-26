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
package com.ibm.ws.logging.utils;

import com.ibm.ws.logging.internal.impl.SpecialHandler;
import com.ibm.ws.logging.source.LogSource;

public class HandlerUtils {

    public static LogSource retrieveLogSource() {
        SpecialHandler sh = SpecialHandler.getInstance();
        return sh.getLogSource();
        //return null;
    }

}
