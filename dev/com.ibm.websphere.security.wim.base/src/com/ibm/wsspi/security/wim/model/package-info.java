/*******************************************************************************
 * Copyright (c) 2016,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * @version 1.1
 */
@org.osgi.annotation.versioning.Version("1.1")
@com.ibm.websphere.ras.annotation.TraceOptions(traceGroup = com.ibm.websphere.security.wim.util.TraceConstants.TRACE_GROUP,
                                               messageBundle = com.ibm.websphere.security.wim.util.TraceConstants.MESSAGE_BUNDLE)
@javax.xml.bind.annotation.XmlSchema(namespace = SchemaConstants.WIM_NS_URI,
                                     attributeFormDefault = XmlNsForm.UNQUALIFIED,
                                     elementFormDefault = XmlNsForm.QUALIFIED,
                                     xmlns = { @XmlNs(prefix = SchemaConstants.WIM_NS_PREFIX, namespaceURI = SchemaConstants.WIM_NS_URI),
                                               @XmlNs(prefix = "xsd", namespaceURI = "http://www.w3.org/2001/XMLSchema"),
                                               @XmlNs(prefix = "xsi", namespaceURI = "http://www.w3.org/2001/XMLSchema-instance") })
package com.ibm.wsspi.security.wim.model;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;

import com.ibm.wsspi.security.wim.SchemaConstants;
