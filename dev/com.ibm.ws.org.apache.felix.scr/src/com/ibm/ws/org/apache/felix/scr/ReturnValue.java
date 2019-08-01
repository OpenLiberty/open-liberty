/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.org.apache.felix.scr;

import java.util.Map;

public interface ReturnValue {

    public static final ReturnValue VOID = new ReturnValue() {
        @Override
        public boolean isVoid() { return true; }

        @Override
        public Map<String, Object> getReturnValue() { return null; }
    };

    public boolean isVoid();
    public Map<String, Object> getReturnValue();
}