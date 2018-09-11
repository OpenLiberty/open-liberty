/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.social;

import java.io.File;

import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.social.internal.Oauth2LoginConfigImpl;

public class SocialConstants extends Constants {

    public static final String CONFIGS_DIR = "configs" + File.separator;

    public static final String DEFAULT_CONTEXT_ROOT = Oauth2LoginConfigImpl.DEFAULT_CONTEXT_ROOT;

}
