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
import java.util.Iterator;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;

public class ParamGenerator extends PageTranslationTimeGenerator {

    public ParamGenerator() {
        super(new String[] {"name"});
    }

    public void startGeneration(int section, JavaCodeWriter writer) throws JspCoreException {}

    public void endGeneration(int section, JavaCodeWriter writer)  throws JspCoreException {
        if (section == CodeGenerationPhase.METHOD_SECTION) {
            boolean encode = true;

            Node parent = element.getParentNode();
            if (findPluginParent(parent)){
                encode = false;
            }
            String name = getAttributeValue("name");
            Attr value = element.getAttributeNode("value");

            String expressionValue = null;
            if (value == null) {
                HashMap jspAttributes = (HashMap)persistentData.get("jspAttributes");
                if (jspAttributes != null) {
                    ArrayList jspAttributeList = (ArrayList)jspAttributes.get(element);

                    for (Iterator itr = jspAttributeList.iterator(); itr.hasNext();) {
                        AttributeGenerator.JspAttribute jspAttribute = (AttributeGenerator.JspAttribute)itr.next();
                        if (jspAttribute.getName().equals("value")) {
                            expressionValue = jspAttribute.getVarName();
                        }
                    }
                }
            }
            else {
                boolean newEncode = encode && !jspOptions.isDisableURLEncodingForParamTag(); //PK47738
                //PK65013 - start
                String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
                if (isTagFile && jspOptions.isModifyPageContextVariable()) {
                    pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
                }
                //PK65013 - end
                expressionValue = GeneratorUtils.attributeValue(value.getValue(), newEncode, String.class, jspConfiguration, isTagFile, pageContextVar); //PK65013 and PK47738
            }

            JspParam jspParam = null;

            if (encode) {
				if (!jspOptions.isDisableURLEncodingForParamTag()) { //PK47738
                	name = "org.apache.jasper.runtime.JspRuntimeLibrary.URLEncode(" +
                       GeneratorUtils.quote(name) + ", request.getCharacterEncoding())";
				}else {
					name = GeneratorUtils.quote(name); //PK47738
            	}
            }

            jspParam = new JspParam(name, expressionValue);

            HashMap jspParams = (HashMap)persistentData.get("jspParams");
            if (jspParams == null) {
                jspParams = new HashMap();
                persistentData.put("jspParams", jspParams);
            }

            if (parent.getNodeType() == Node.ELEMENT_NODE &&
                parent.getNamespaceURI() != null &&
                parent.getNamespaceURI().equals(Constants.JSP_NAMESPACE) &&
                parent.getLocalName().equals(Constants.JSP_BODY_TYPE)) {
                parent = parent.getParentNode();
            }

            ArrayList jspParamList = (ArrayList)jspParams.get(parent);
            if (jspParamList == null) {
                jspParamList = new ArrayList();
                jspParams.put(parent, jspParamList);
            }
            jspParamList.add(jspParam);
        }
    }

    private boolean findPluginParent(Node parent) {
        boolean pluginFound = false;
        if (parent != null) {
            if (parent.getNamespaceURI() != null &&
                parent.getNamespaceURI().equals(Constants.JSP_NAMESPACE) &&
                parent.getLocalName().equals(Constants.JSP_PLUGIN_TYPE)) {
                pluginFound = true;
            }
            else {
                pluginFound = findPluginParent(parent.getParentNode());
            }
        }

        return pluginFound;
    }

    public class JspParam {
        private String name = null;
        private String value = null;

        public JspParam(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
