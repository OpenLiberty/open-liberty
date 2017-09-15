/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.jwt;

public interface Consumer {

    public final static String DEFAULT_ID = "defaultJwtConsumer";

    public abstract Consumer create() throws InvalidConsumerException;

    public abstract Consumer create(String consumerConfigId) throws InvalidConsumerException;

    public abstract JwtToken createJwt(String encodedTokenString) throws InvalidTokenException, InvalidConsumerException;

}