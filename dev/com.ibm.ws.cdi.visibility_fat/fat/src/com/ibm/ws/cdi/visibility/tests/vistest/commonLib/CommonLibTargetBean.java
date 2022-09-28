/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.visibility.tests.vistest.commonLib;

import javax.enterprise.context.ApplicationScoped;

import com.ibm.ws.cdi.visibility.tests.vistest.framework.TargetBean;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InCommonLib;

@ApplicationScoped
@InCommonLib
public class CommonLibTargetBean implements TargetBean {

}
