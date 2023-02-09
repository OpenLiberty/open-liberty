/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.faces40.fat.literals;

import jakarta.faces.annotation.FacesConfig;

/**
 * This class forces CDI to create a @FacesConfig beans for testing.
 */
@FacesConfig
public class ConfigBean {
    //Nothing to configure
}
