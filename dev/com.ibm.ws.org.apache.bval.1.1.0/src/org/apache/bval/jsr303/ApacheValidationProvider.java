/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.apache.bval.jsr303;

/**
 * This is a no-op extender so that we maintain compatibility with bean validation
 * 1.0 apps that explicitly listed the apache provider class in the <default-provider>
 * element in validation.xml.
 * 
 * <p>
 * Apache Bean Validation changed the package name in their 1.1 implementation,
 * so we provide this provider with the original package name to allow 1.0 applications
 * to move up without needing to change their application.
 */
public class ApacheValidationProvider extends org.apache.bval.jsr.ApacheValidationProvider {

}
