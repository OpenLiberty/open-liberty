/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.lifecycle.apps.eventMetaDataWar;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.lang.annotation.Annotation;

import javax.enterprise.context.Initialized;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.EventMetadata;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 *
 */
@WebServlet("/")
@RequestScoped
public class MetaDataServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    private static EventMetadata beanStartMetaData = null;
    private static EventMetadata beanFiredMetaData = null;

    @Inject
    private RequestScopedBean bean;

    @Test
    public void testStartMetaData() {
        assertThat(beanStartMetaData, not(nullValue()));
        assertThat(beanStartMetaData.getQualifiers(), containsInAnyOrder(new AnyLiteral(), new InitializedLiteral(RequestScoped.class)));
        assertThat(beanStartMetaData.getInjectionPoint(), nullValue());
        assertThat(beanStartMetaData.getType(), equalTo(HttpServletRequest.class));
    }

    @Test
    public void testFiredMetaData() throws NoSuchFieldException {
        bean.fireEvent();
        assertThat(beanFiredMetaData, not(nullValue()));
        assertThat(beanFiredMetaData.getQualifiers(), containsInAnyOrder(new AnyLiteral(), new MetaQualifierLiteral()));
        assertThat(beanFiredMetaData.getInjectionPoint(), not(nullValue()));
        assertThat(beanFiredMetaData.getInjectionPoint().getMember(), equalTo(RequestScopedBean.class.getDeclaredField("event")));
        assertThat(beanFiredMetaData.getInjectionPoint().getQualifiers(), containsInAnyOrder(new MetaQualifierLiteral()));
        assertThat(beanFiredMetaData.getType(), equalTo(MyEvent.class));
    }

    public static void onStart(@Observes @Initialized(RequestScoped.class) Object e, EventMetadata em) {
        beanStartMetaData = em;
    }

    public static void onFired(@Observes MyEvent e, EventMetadata em) {
        beanFiredMetaData = em;
    }

    @SuppressWarnings("serial")
    private static class AnyLiteral extends AnnotationLiteral<Any> implements Any {}

    @SuppressWarnings("serial")
    private static class InitializedLiteral extends AnnotationLiteral<Initialized> implements Initialized {
        private final Class<? extends Annotation> value;

        public InitializedLiteral(Class<? extends Annotation> value) {
            this.value = value;
        }

        /** {@inheritDoc} */
        @Override
        public Class<? extends Annotation> value() {
            return value;
        }

    }

    @SuppressWarnings("serial")
    private static class MetaQualifierLiteral extends AnnotationLiteral<MetaQualifier> implements MetaQualifier {}

}
