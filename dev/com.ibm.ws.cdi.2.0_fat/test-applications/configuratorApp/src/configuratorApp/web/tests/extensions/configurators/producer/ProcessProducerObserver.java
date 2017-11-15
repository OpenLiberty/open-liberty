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
package configuratorApp.web.tests.extensions.configurators.producer;

import java.lang.annotation.Annotation;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessProducer;

public class ProcessProducerObserver implements Extension {

    public static boolean accepted;
    public static boolean applied;

    public void observeProducer(@Observes ProcessProducer<ShapeProducer, Dodecahedron> event) {

        if (producerHasAnnotation(event, ThreeDimensional.ThreeDimensionalLiteral.INSTANCE)) {

            event.configureProducer().disposeWith(new Consumer<Dodecahedron>() {

                @Override
                public void accept(Dodecahedron t) {
                    accepted = true;
                }
            }).produceWith(new Function<CreationalContext<Dodecahedron>, Dodecahedron>() {

                @Override
                public Dodecahedron apply(CreationalContext<Dodecahedron> t) {
                    applied = true;
                    return new Dodecahedron(new ParameterInjectedBean(null));
                }
            });
        }
    }

    private boolean producerHasAnnotation(ProcessProducer<ShapeProducer, Dodecahedron> event, Annotation annotation) {
        return event.getAnnotatedMember().getAnnotations().contains(annotation);
    }
}