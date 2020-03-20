/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.cdi.extension;

/**
 * This is a *marker* interface for Weld Runtime extension. All runtime extensions need to register a service
 * under this interface. This bundle will find all of the services and then get hold of the bundle classloader and
 * pass onto Weld.
 *
 * To use this class you must implement this extension on a class that also implements javax.enterprise.inject.spi.Extension.
 * The class should be annotated with @Component(service = WebSphereCDIExtension.class)
 *
 * The Component annotation takes the following properties: 
 * api.classes: The beans your extension will register with CDI. 
 * immediate: If true this bundle will start straight away, it will not be loaded lazily. 
 *
 * For example: 
 * @Component(service = WebSphereCDIExtension.class, property = { "api.classes=org.eclipse.microprofile.jwt.Claim;org.eclipse.microprofile.jwt.Claims"}, immediate = true)
 * public class JwtCDIExtension implements Extension, WebSphereCDIExtension { ... } 
 */


public interface WebSphereCDIExtension {}
