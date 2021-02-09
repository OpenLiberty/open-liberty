/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * CDI Testing: Type for servlet field injection.
 */
@RequestScoped
@Named
@ServletType
public class ServletFieldBean extends FieldBean {
    // EMPTY
}
