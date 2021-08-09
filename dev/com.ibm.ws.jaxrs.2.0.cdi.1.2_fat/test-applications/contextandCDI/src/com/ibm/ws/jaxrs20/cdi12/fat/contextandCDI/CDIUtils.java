/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi12.fat.contextandCDI;

import java.lang.annotation.Annotation;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

public class CDIUtils {

   public static <E> E getBean(Class<E> clazz, Annotation... qualifiers) {

    Instance<E> instance = CDI.current().select(clazz, qualifiers);

    if (instance.isUnsatisfied()) {
        throw new RuntimeException();
    }
    if (instance.isAmbiguous()) {
        throw new RuntimeException();
    }

    return instance.get();
  }
}
