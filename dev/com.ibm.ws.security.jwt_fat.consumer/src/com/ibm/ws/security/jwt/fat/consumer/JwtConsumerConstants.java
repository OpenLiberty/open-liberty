/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.consumer;

import com.ibm.ws.security.fat.common.jwt.JwtConstants;

public class JwtConsumerConstants extends JwtConstants {

    /*********************************** Consumer Defaults *************************************/
    public static final String JWT_CONSUMER_DEFAULT_CONFIG = "defaultJwtConsumer";

    /********************************* JWT Consumer API Servlet ********************************/
    public static final String JWT_CONSUMER_SERVLET = "jwtconsumerclient";
    public static final String JWT_CONSUMER_ENDPOINT = JWT_CONSUMER_SERVLET + "/JwtConsumerClient";
    public static final String JWT_CONSUMER_PARAM_CLIENT_ID = "clientId";
    public static final String JWT_CONSUMER_PARAM_JWT = "jwtString";
    public static final String JWT_CONSUMER_CLAIM = "JWT Consumer Claim: ";

}