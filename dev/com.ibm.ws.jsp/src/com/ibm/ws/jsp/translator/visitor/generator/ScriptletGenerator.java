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

import org.w3c.dom.CDATASection;
import org.w3c.dom.Node;

import com.ibm.ws.jsp.JspCoreException;

public class ScriptletGenerator extends CodeGeneratorBase {
    public void startGeneration(int section, JavaCodeWriter writer) throws JspCoreException {
        if (section == CodeGenerationPhase.METHOD_SECTION) {
            for (int i = 0; i < element.getChildNodes().getLength(); i++) {
                Node n = element.getChildNodes().item(i);
                if (n.getNodeType() == Node.CDATA_SECTION_NODE) {
                    CDATASection cdata = (CDATASection)n;
                    String data = cdata.getData();
                    data = data.replaceAll("&gt;", ">");
                    data = data.replaceAll("&lt;", "<");
                    data = data.replaceAll("&amp;", "&");
                    char[] chars = data.toCharArray();
                    writeDebugStartBegin(writer);
                    writer.printMultiLn(new String(GeneratorUtils.removeQuotes(chars)));
                    writeDebugStartEnd(writer);
                }
            }
        }
    }

    public void endGeneration(int section, JavaCodeWriter writer)  throws JspCoreException {}
}
