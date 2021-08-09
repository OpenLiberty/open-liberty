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
package io.openliberty.cdi30.internal.fat.apps.beansxml;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AnnotatedBean {

    //doesn't do anything, only here to ensure CDI is enabled even if the beans.xml doesn't load as it should
}
