/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.xpath.mapping.datatype;

public interface XPathPropertyNode extends XPathNode {

    void setOperator(String operator);

    String getOperator();

    void setName(String name);

    String getName();

    void setValue(Object value);

    Object getValue();

    void setPropertyLocation(boolean isInRepos);

    boolean isPropertyInRepository();
}
