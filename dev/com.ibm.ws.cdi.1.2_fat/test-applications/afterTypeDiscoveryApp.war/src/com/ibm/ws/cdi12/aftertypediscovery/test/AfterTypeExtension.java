/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.aftertypediscovery.test;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

public class AfterTypeExtension implements Extension {

    void afterTypeDiscovery(@Observes AfterTypeDiscovery event, BeanManager bm) {

        event.addAnnotatedType(bm.createAnnotatedType(AfterTypeInterface.class), AfterTypeInterface.class.getName());
        event.addAnnotatedType(bm.createAnnotatedType(AfterTypeBean.class), AfterTypeBean.class.getName());
        event.addAnnotatedType(bm.createAnnotatedType(AfterTypeBeanDecorator.class), AfterTypeBeanDecorator.class.getName());
        event.getDecorators().add(AfterTypeBeanDecorator.class);

        event.addAnnotatedType(bm.createAnnotatedType(AfterTypeInterceptorImpl.class), AfterTypeInterceptorImpl.class.getName());
        event.addAnnotatedType(bm.createAnnotatedType(InterceptedAfterType.class), InterceptedAfterType.class.getName());
        event.addAnnotatedType(bm.createAnnotatedType(InterceptedBean.class), InterceptedBean.class.getName());
        event.getInterceptors().add(AfterTypeInterceptorImpl.class);

        event.addAnnotatedType(bm.createAnnotatedType(AfterTypeAlternativeInterface.class), AfterTypeAlternativeInterface.class.getName());
        event.addAnnotatedType(bm.createAnnotatedType(AfterTypeAlternativeOne.class), AfterTypeAlternativeOne.class.getName());
        event.addAnnotatedType(bm.createAnnotatedType(AfterTypeAlternativeTwo.class), AfterTypeAlternativeTwo.class.getName());
        event.addAnnotatedType(bm.createAnnotatedType(AfterTypeNotAlternative.class), AfterTypeNotAlternative.class.getName());
        event.addAnnotatedType(bm.createAnnotatedType(UseAlternative.class), UseAlternative.class.getName());
        event.getAlternatives().add(AfterTypeAlternativeOne.class);
        event.getAlternatives().add(AfterTypeAlternativeTwo.class);

    }

}
