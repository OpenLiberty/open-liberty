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
public class FallbackDefinesHandlerAndMethod {

    @Fallback(value = TestFallbackHandler.class, fallbackMethod = "fallbackMethod")
    public void badMethod() {}

    public void fallbackMethod() {}

    @ApplicationScoped
    public static class TestFallbackHandler implements FallbackHandler<Void> {

        @Override
        public Void handle(ExecutionContext context) {
            return null;
        }

    }

}
