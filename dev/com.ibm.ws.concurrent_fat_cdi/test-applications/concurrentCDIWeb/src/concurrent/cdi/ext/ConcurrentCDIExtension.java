/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package concurrent.cdi.ext;

import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.Schedule;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import jakarta.enterprise.util.AnnotationLiteral;

/**
 * A CDI extension provided by the application that adds an Asynchronous method
 * without annotating it.
 */
public class ConcurrentCDIExtension implements Extension {
    /**
     * Programmatically defined literal instance of the Asynchronous annotation.
     */
    public static final class AsyncLiteral extends AnnotationLiteral<Asynchronous> //
                    implements Asynchronous {
        public static final AsyncLiteral INSTANCE = new AsyncLiteral();

        private static final long serialVersionUID = 1L;

        private AsyncLiteral() {
        }

        @Override
        public String executor() {
            return "java:comp/DefaultManagedExecutorService";
        }

        @Override
        public Schedule[] runAt() {
            return new Schedule[] {};
        }
    }

    /**
     * Observer method that makes a bean method that is named "asyncByExtension"
     * into an Asynchronous method.
     */
    public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> event) {
        AnnotatedTypeConfigurator<T> typeConfigurator = //
                        event.configureAnnotatedType();

        typeConfigurator.methods()
                        .stream()
                        .filter(methodConfigurator -> methodConfigurator
                                        .getAnnotated()
                                        .getJavaMember()
                                        .getName()
                                        .equals("asyncByExtension"))
                        .forEach(methodConfigurator -> {
                            System.out.println("CDI extension found: " +
                                               methodConfigurator.getAnnotated());
                            methodConfigurator.add(AsyncLiteral.INSTANCE);
                            System.out.println("  added literal Asynchronous");
                        });
    }
}
