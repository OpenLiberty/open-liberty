/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.cdi.extension.apps.observer;

/**
 *
 */
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessProducerMethod;

public class ObserverExtension implements Extension {

    private AnnotatedMethod<Car> producerMethod;

    void processProducerMethod(@Observes ProcessProducerMethod<Car, CarFactory> event) {
        AnnotatedMethod<Car> producerMethod = event.getAnnotatedProducerMethod();
        System.out.println("PlainExtension: processProducerMethod: " + producerMethod);
        this.producerMethod = producerMethod;
    }

    void processAnnotatedType(@Observes ProcessAnnotatedType<?> event) {
        System.out.println("PlainExtension: processAnnotatedType: " + event);
    }

    public AnnotatedMethod<Car> getProducerMethod() {
        return producerMethod;
    }

    void processInjectionTarget(@Observes ProcessInjectionTarget<?> event) {
        System.out.println("PlainExtension: processInjectionTarget: " + event.getInjectionTarget());
        System.out.println("PlainExtension: processInjectionTarget: " + event.getInjectionTarget().getInjectionPoints());

        setCustomInjectionTarget(event);
    }

    <T> void setCustomInjectionTarget(ProcessInjectionTarget<T> event) {
        InjectionTarget<T> original = event.getInjectionTarget();
        InjectionTarget<T> custom = new CustomInjectionTarget<T>(original);
        event.setInjectionTarget(custom);
    }

}
