/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel.servlet;

import java.io.IOException;
import java.util.logging.FileHandler;

public class MyCustomHandler extends FileHandler {
    public MyCustomHandler(String pattern, int limit, int count, boolean append) throws SecurityException, IOException {
        super(pattern, limit, count, append);
    }
}