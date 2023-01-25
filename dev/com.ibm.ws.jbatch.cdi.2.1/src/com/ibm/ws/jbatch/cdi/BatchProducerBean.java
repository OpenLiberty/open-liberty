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
package com.ibm.ws.jbatch.cdi;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import com.ibm.jbatch.container.cdi.DependencyInjectionUtilityCdi;
import com.ibm.jbatch.container.cdi.ProxyFactoryCdi;
import com.ibm.jbatch.jsl.model.Property;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.InjectionPoint;

public class BatchProducerBean {

    @Produces
    @Dependent
    @BatchProperty
    public Boolean produceBooleanProperty(InjectionPoint injectionPoint) {
        String propValStr = getStringProperty(injectionPoint);
        return Boolean.valueOf(propValStr);
    }

    @Produces
    @Dependent
    @BatchProperty
    public Double produceDoubleProperty(InjectionPoint injectionPoint) {
        String propValStr = getStringProperty(injectionPoint);
        return Double.valueOf(propValStr);
    }

    @Produces
    @Dependent
    @BatchProperty
    public Float produceFloatProperty(InjectionPoint injectionPoint) {
        String propValStr = getStringProperty(injectionPoint);
        return Float.valueOf(propValStr);
    }

    @Produces
    @Dependent
    @BatchProperty
    public Integer produceIntProperty(InjectionPoint injectionPoint) {
        String propValStr = getStringProperty(injectionPoint);
        return Integer.valueOf(propValStr);
    }

    @Produces
    @Dependent
    @BatchProperty
    public Long produceLongProperty(InjectionPoint injectionPoint) {
        String propValStr = getStringProperty(injectionPoint);
        return Long.valueOf(propValStr);
    }

    @Produces
    @Dependent
    @BatchProperty
    public Short produceShortProperty(InjectionPoint injectionPoint) {
        String propValStr = getStringProperty(injectionPoint);
        return Short.valueOf(propValStr);
    }

    @Produces
    @Dependent
    @BatchProperty
    public String produceStringProperty(InjectionPoint injectionPoint) {
        String propValStr = getStringProperty(injectionPoint);
        return propValStr;
    }

    private String getStringProperty(InjectionPoint injectionPoint) {

        //Seems like this is a CDI bug where null injection points are getting passed in.
        //We should be able to ignore these as a workaround.
        if (injectionPoint != null) {
            if (ProxyFactoryCdi.getInjectionReferences() == null) {
                return null;
            }

            BatchProperty batchPropAnnotation = null;
            String batchPropName = null;
            Annotated annotated = injectionPoint.getAnnotated();
            if (annotated != null) {
                batchPropAnnotation = annotated.getAnnotation(BatchProperty.class);

                // If a name is not supplied the batch property name defaults to
                // the field name
                if (batchPropAnnotation.name().equals("")) {
                    batchPropName = injectionPoint.getMember().getName();
                } else {
                    batchPropName = batchPropAnnotation.name();
                }
            } else {

                // No attempt to match by field name in this path.
                Set<Annotation> qualifiers = injectionPoint.getQualifiers();
                for (Annotation a : qualifiers.toArray(new Annotation[0])) {
                    if (a instanceof BatchProperty) {
                        BatchProperty batchPropertyAnno = (BatchProperty) a;
                        batchPropName = ((BatchProperty) a).name();
                        break;
                    }
                }
            }

            if (batchPropName != null) {
                List<Property> propList = ProxyFactoryCdi.getInjectionReferences().getProps();
                return DependencyInjectionUtilityCdi.getPropertyValue(propList, batchPropName);
            }
        }

        return null;
    }

    @Produces
    @Dependent
    public JobContext getJobContext() {

        if (ProxyFactoryCdi.getInjectionReferences() != null) {
            return ProxyFactoryCdi.getInjectionReferences().getJobContext();
        } else {
            return null;
        }
    }

    @Produces
    @Dependent
    public StepContext getStepContext() {

        if (ProxyFactoryCdi.getInjectionReferences() != null) {
            return ProxyFactoryCdi.getInjectionReferences().getStepContext();
        } else {
            return null;
        }

    }

}
