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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
@LifecycleBinding
public class LifecycleTestBean {

    @Inject
    private ExecutionRecorder recorder;

    public boolean test() {
        recorder.record("beanInvocation");
        return true;
    }

    @PostConstruct
    private void postConstruct() {
        recorder.record("beanPostConstruct");
    }

    @PreDestroy
    private void preDestroy() {
        recorder.record("beanPreDestroy");
    }

}
