/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * @version 1.0.0
 */
@org.osgi.annotation.versioning.Version("1.0.0")
@TraceOptions(traceGroup = "rarInstall", messageBundle = "com.ibm.ws.jca.utils.internal.resources.JcaUtilsMessages")
//FIXME need to find a way to transform http://xmlns.jcp.org/xml/ns/javaee -> https://jakarta.ee/xml/ns/jakartaee
@XmlSchema(namespace = "http://xmlns.jcp.org/xml/ns/javaee", elementFormDefault = XmlNsForm.QUALIFIED)
@XmlJavaTypeAdapter(value = MetagenXmlAdapter.class, type = String.class)
package com.ibm.ws.jca.utils.xml.ra;

import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.ibm.websphere.ras.annotation.TraceOptions;
import com.ibm.ws.jca.utils.metagen.internal.MetagenXmlAdapter;
