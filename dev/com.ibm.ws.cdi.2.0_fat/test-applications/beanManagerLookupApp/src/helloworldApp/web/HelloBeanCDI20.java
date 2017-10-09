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
package helloworldApp.web;

import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;

@RequestScoped
public class HelloBeanCDI20 {

    public String greeting() {
        return "Hello World CDI 2.0!";
    }

    public int getBeanMangerViaJNDI() throws Exception {
        BeanManager beanManager = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
        Set<Bean<?>> beans = beanManager.getBeans(Object.class);
        return beans.size();
    }
}
