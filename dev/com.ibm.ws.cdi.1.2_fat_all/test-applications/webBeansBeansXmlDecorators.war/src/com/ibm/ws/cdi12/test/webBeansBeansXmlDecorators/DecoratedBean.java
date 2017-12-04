/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.webBeansBeansXmlDecorators;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * A bean with a simple message.
 */
@RequestScoped
@Named
public class DecoratedBean implements Bean {

    @Override
    public String getMessage() {
        return "message";
    }

}
