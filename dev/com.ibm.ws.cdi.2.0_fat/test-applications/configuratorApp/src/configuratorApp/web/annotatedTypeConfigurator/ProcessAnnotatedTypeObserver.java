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
package configuratorApp.web.annotatedTypeConfigurator;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

public class ProcessAnnotatedTypeObserver implements Extension {

    void observer(@Observes ProcessAnnotatedType<Pen> event) {
        AnnotatedTypeConfigurator<Pen> atc = event.configureAnnotatedType();

        atc.add(RequestScoped.Literal.INSTANCE);
    }
}