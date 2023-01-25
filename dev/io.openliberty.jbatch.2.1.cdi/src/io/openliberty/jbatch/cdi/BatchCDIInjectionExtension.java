/**
 * Copyright 2013, 2022 International Business Machines Corp.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
