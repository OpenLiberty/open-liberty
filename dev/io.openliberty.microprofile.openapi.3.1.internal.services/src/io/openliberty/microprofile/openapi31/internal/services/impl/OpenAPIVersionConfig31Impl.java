/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi31.internal.services.impl;

import org.osgi.service.component.annotations.Component;

import io.openliberty.microprofile.openapi20.internal.OpenAPIVersionConfigImpl;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIVersionConfig;

@Component(configurationPid = "io.openliberty.microprofile.openapi", service = OpenAPIVersionConfig.class)
public class OpenAPIVersionConfig31Impl extends OpenAPIVersionConfigImpl {}
