/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.visitor.configuration;

import java.util.HashMap;

public class JspVisitorConfiguration {
    protected HashMap jspVisitorDefinitionMap = new HashMap();
    protected HashMap jspVisitorCollectionMap = new HashMap();
    
    public JspVisitorConfiguration() {
    }
    
    void addJspVisitorDefinition(JspVisitorDefinition jspVisitorDefinition) {
        jspVisitorDefinitionMap.put(jspVisitorDefinition.getId(), jspVisitorDefinition);   
    }
    
    JspVisitorDefinition getJspVisitorDefinition(String id) {
        return ((JspVisitorDefinition)jspVisitorDefinitionMap.get(id));
    }
    
    void addJspVisitorCollection(JspVisitorCollection jspVisitorCollection) {
        jspVisitorCollectionMap.put(jspVisitorCollection.getId(), jspVisitorCollection);   
    }
    
    public JspVisitorCollection getJspVisitorCollection(String id) {
        return ((JspVisitorCollection)jspVisitorCollectionMap.get(id));
    }
}
