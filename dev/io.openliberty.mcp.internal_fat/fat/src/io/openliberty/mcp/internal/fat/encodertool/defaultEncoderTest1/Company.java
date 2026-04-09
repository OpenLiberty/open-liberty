/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.encodertool.defaultEncoderTest1;

/**
 * Local Company record - same structure as War1SpecificEncoders.Company but different class.
 * This will use default JSON encoding since CompanyContentEncoder is not available in this module.
 * Fields are in alphabetical order to match JSON-B serialization.
 */
public record Company(int employees, String industry, String name) {}
