/*******************************************************************************
 * Copyright (c) 2013, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jbatch.cdi;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.batch.operations.JobOperator;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBean;

import com.ibm.ws.cdi.CDIServiceUtils;

public class BatchCDIInjectionExtension implements Extension {

    private final static Logger logger = Logger.getLogger(BatchCDIInjectionExtension.class.getName());

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        AnnotatedType<BatchProducerBean> at = bm.createAnnotatedType(BatchProducerBean.class);
        bbd.addAnnotatedType(at, CDIServiceUtils.getAnnotatedTypeIdentifier(at, this.getClass()));
    }

    private Boolean foundJobOp = false;

    public <A> void processBean(final @Observes ProcessBean<A> processBeanEvent) {
        if (!foundJobOp) {
            if (processBeanEvent.getBean().getTypes().contains(JobOperator.class)) {
                if (processBeanEvent.getBean().getBeanClass().equals(JobOpProducerBean.class)) {
                    logger.log(Level.FINE, "BatchCDIInjectionExtension.processBean() detecting our own JobOpProducerBean");
                } else {
                    logger.log(Level.FINE, "BatchCDIInjectionExtension.processBean() Found JobOperator of class: " + processBeanEvent.getBean().getBeanClass());
                    foundJobOp = true;
                }
            }
        }
    }

    public void afterBeanDiscovery(final @Observes AfterBeanDiscovery abd, BeanManager bm) {
        if (foundJobOp) {
          logger.log(Level.FINE, "Deferring to other detected JobOperator Bean");
          return;
        }
        logger.log(Level.FINE, "Didn't find JobOperator Bean, registering JBatch one");
        abd.addBean(new JobOpProducerBean(bm));
    }

}
