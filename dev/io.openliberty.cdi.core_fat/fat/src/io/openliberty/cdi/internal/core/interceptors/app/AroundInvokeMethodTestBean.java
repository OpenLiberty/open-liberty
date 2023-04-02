/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi.internal.core.interceptors.app;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * For testing AROUND_INVOKE interception at the method level
 */
@ApplicationScoped
public class AroundInvokeMethodTestBean {

    @Inject
    private ExecutionRecorder recorder;

    @AroundInvokeBinding
    public void test() {
        recorder.record("beanInvoke");
    }

    public void testNotAnnotated() {
        recorder.record("beanInvokeNotAnnotated");
    }

}
