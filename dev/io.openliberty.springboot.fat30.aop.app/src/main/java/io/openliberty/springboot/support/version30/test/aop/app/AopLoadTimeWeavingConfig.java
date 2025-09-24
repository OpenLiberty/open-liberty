/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.springboot.support.version30.test.aop.app;

import static org.springframework.context.ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME;

import org.springframework.context.weaving.DefaultContextLoadTimeWeaver;
import org.springframework.stereotype.Component;

@Component(LOAD_TIME_WEAVER_BEAN_NAME)
public class AopLoadTimeWeavingConfig extends DefaultContextLoadTimeWeaver {

}
