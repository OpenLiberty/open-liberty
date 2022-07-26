/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
@XmlJavaTypeAdapters({
                       @XmlJavaTypeAdapter(CollapsedStringAdapter.class),
                       @XmlJavaTypeAdapter(NormalizedStringAdapter.class),
                       @XmlJavaTypeAdapter(HexBinaryAdapter.class) })
@XmlSchema(
           namespace = "http://jaxb.web.dataobjects/",
           elementFormDefault = XmlNsForm.QUALIFIED,
           xmlns = {
                     @XmlNs(prefix = "ns0", namespaceURI = "http://jaxb.web.dataobjects/")
           })
package jaxb.web.dataobjects;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
