/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.helloworldra;

import javax.resource.cci.InteractionSpec;

public interface HelloWorldInteractionSpec extends InteractionSpec {

    public static final String SAY_HELLO_FUNCTION = "sayHello";

    public String getFunctionName();

    public void setFunctionName(String functionName);
}