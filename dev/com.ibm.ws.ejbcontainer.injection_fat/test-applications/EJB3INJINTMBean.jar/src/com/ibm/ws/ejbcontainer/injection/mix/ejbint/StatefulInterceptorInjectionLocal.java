/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.mix.ejbint;

import javax.ejb.Local;

@Local
public interface StatefulInterceptorInjectionLocal {
    public String getAnnotationInterceptorResults();

    public String getAnnotationInterceptor2Results();

    public String getXMLInterceptorResults();

    public String getXMLInterceptor2Results();

    public void finish();

    public String getXMLInterceptor4Results();

    public String getXMLInterceptor3Results();
}