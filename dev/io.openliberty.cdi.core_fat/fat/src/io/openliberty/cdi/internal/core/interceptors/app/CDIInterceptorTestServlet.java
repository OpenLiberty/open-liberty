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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/interceptor")
public class CDIInterceptorTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private ExecutionRecorder recorder;

    @Inject
    private Instance<LifecycleTestBean> lifeCycleInstance;

    @Inject
    private AroundInvokeClassTestBean aroundInvokeClassBean;

    @Inject
    private AroundInvokeMethodTestBean aroundInvokeMethodBean;

    @Test
    public void testLifeCycleInterception() {
        assertThat(recorder.getExecutions(), hasSize(0));

        LifecycleTestBean testBean = lifeCycleInstance.get();
        assertThat(recorder.getExecutions(), contains("interceptorPostConstruct", "beanPostConstruct"));

        lifeCycleInstance.destroy(testBean);
        assertThat(recorder.getExecutions(), contains("interceptorPostConstruct", "beanPostConstruct", "interceptorPreDestroy", "beanPreDestroy"));
    }

    @Test
    public void testAroundInvokeClassInterception() {
        assertThat(recorder.getExecutions(), hasSize(0));

        aroundInvokeClassBean.test1();
        assertThat(recorder.getExecutions(), contains("interceptorPreInvoke", "beanInvoke1", "interceptorPostInvoke"));

        aroundInvokeClassBean.test2();
        assertThat(recorder.getExecutions(), contains("interceptorPreInvoke", "beanInvoke1", "interceptorPostInvoke",
                                                      "interceptorPreInvoke", "beanInvoke2", "interceptorPostInvoke"));
    }

    @Test
    public void testAroundInvokeMethodInterception() {
        assertThat(recorder.getExecutions(), hasSize(0));

        aroundInvokeMethodBean.test();
        assertThat(recorder.getExecutions(), contains("interceptorPreInvoke", "beanInvoke", "interceptorPostInvoke"));

        aroundInvokeMethodBean.testNotAnnotated();
        assertThat(recorder.getExecutions(), contains("interceptorPreInvoke", "beanInvoke", "interceptorPostInvoke",
                                                      "beanInvokeNotAnnotated"));
    }

}
