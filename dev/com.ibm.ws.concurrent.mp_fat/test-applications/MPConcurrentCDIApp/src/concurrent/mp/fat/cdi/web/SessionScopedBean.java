/*******************************************************************************
 * Copyright (c) 2018,2019 IBM Corporation and others.

 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.mp.fat.cdi.web;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;

@SessionScoped
public class SessionScopedBean extends AbstractBean implements Serializable {

    private static final long serialVersionUID = -2175930188464022407L;

}
