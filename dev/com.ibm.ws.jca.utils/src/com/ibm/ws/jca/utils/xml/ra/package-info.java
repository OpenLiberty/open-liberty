/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * @version 1.0.0
 */
@org.osgi.annotation.versioning.Version("1.0.0")
@TraceOptions(traceGroup = "rarInstall", messageBundle = "com.ibm.ws.jca.utils.internal.resources.JcaUtilsMessages")
@XmlSchema(namespace = "https://jakarta.ee/xml/ns/jakartaee", elementFormDefault = XmlNsForm.QUALIFIED)
@XmlJavaTypeAdapter(value = MetagenXmlAdapter.class, type = String.class)
package com.ibm.ws.jca.utils.xml.ra;

import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.ibm.websphere.ras.annotation.TraceOptions;
import com.ibm.ws.jca.utils.metagen.internal.MetagenXmlAdapter;