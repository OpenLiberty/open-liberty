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
package com.ibm.ws.jsp.translator.visitor.generator;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Node;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;

public class ParamsGenerator extends CodeGeneratorBase {
    public void startGeneration(int section, JavaCodeWriter writer) throws JspCoreException {
    }

    public void endGeneration(int section, JavaCodeWriter writer)  throws JspCoreException {
        if (section == CodeGenerationPhase.METHOD_SECTION) {
            HashMap jspParams = (HashMap)persistentData.get("jspParams");
            if (jspParams != null) {
                ArrayList jspParamList = (ArrayList)jspParams.get(element);
                if (jspParamList != null) {
                    Node parent = element.getParentNode();
                    if (parent.getNodeType() == Node.ELEMENT_NODE &&
                        parent.getNamespaceURI() != null &&
                        parent.getNamespaceURI().equals(Constants.JSP_NAMESPACE) &&
                        parent.getLocalName().equals(Constants.JSP_BODY_TYPE)) {
                        parent = parent.getParentNode();    
                    }
                    jspParams.remove(element);
                    jspParams.put(parent, jspParamList);
                }
            }
        }
    }
}
