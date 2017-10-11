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
package beanManagerLookupApp.web;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MyBeanCDI20 {

    public String greeting() {
        return "You found the CDI20 bean!";
    }

}
