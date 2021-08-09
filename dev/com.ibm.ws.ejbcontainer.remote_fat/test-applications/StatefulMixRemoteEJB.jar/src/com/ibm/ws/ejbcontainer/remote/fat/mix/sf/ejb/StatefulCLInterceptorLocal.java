/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb;

public interface StatefulCLInterceptorLocal {
    void interceptorStart(String key);

    void interceptorEnd(String key);

    public void interceptorStaticStart(String key);

    void interceptorStaticEnd(String key);

    void finish();
}
