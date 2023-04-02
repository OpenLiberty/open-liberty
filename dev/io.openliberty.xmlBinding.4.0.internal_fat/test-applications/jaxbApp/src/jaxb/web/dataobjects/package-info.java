/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.HexBinaryAdapter;
import jakarta.xml.bind.annotation.adapters.NormalizedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
