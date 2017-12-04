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
package configuratorApp.web.beanAttributes;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.inject.spi.configurator.BeanAttributesConfigurator;

public class ProcessBeanAttributesObserver implements Extension {

    public void observer(@Observes ProcessBeanAttributes<Square> pba) {
        BeanAttributesConfigurator<Square> configurator = pba.configureBeanAttributes();

        configurator.addQualifier(Quadrilateral.QuadrilateralLiteral.INSTANCE);
        configurator.addType(Shape.class);
    }
}