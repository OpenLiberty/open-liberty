/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jakarta.xmlbinding;

import java.util.ResourceBundle;

public class JaxbConstants {
    public static final String TR_GROUP = "Jaxb";

    public static final String TR_RESOURCE_BUNDLE = "io.openliberty.jakarta.xmlbinding.resources.JAXBMessages";

    public static final String WARNING_PROPERTY_MAP_JAXBCONTEXTFACTORY = "warning.property.map.jaxbcontextfactory.not.found";

    public static final String WARNING_SYSTEM_PROPERTY_JAXBCONTEXTFACTORY = "warning.system.property.jaxbcontextfactory.not.found";
    
    public static final ResourceBundle messages = ResourceBundle.getBundle(TR_RESOURCE_BUNDLE);
}
