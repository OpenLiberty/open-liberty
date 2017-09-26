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
package com.ibm.websphere.microprofile.faulttolerance_fat.validation;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

@ApplicationScoped
public class FallbackHandlerWrongType {

    @Fallback(TestFallbackHandler.class)
    public void badMethod() {}

    @ApplicationScoped
    public static class TestFallbackHandler implements FallbackHandler<String> {

        @Override
        public String handle(ExecutionContext context) {
            return "test";
        }

    }

}
