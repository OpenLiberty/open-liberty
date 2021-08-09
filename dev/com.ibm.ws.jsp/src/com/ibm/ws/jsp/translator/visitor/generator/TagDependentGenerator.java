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

import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;

import com.ibm.ws.jsp.JspCoreException;

public class TagDependentGenerator extends CodeGeneratorBase {
    private static final String SINGLE_QUOTE = "'";
    private static final String DOUBLE_QUOTE = "\\\"";
    
    public void startGeneration(int section, JavaCodeWriter writer) throws JspCoreException {
        if (section == CodeGenerationPhase.METHOD_SECTION) {
            writeDebugStartBegin(writer);
            writer.print("out.write(\"<");
            writer.print(element.getTagName());
            NamedNodeMap nodeAttrs = element.getAttributes();
            for (int i = 0; i < nodeAttrs.getLength(); i++) {
                Attr attr = (Attr)nodeAttrs.item(i);
                //PM41476 start -  added if statement
                if (jspOptions.isRemoveXmlnsFromOutput() && attr.getName().startsWith("xmlns:") == true ) {
                    continue;
                }
                //PM41476 end
                if (attr.getName().equals("jsp:id") == false && 
                    attr.getName().equals("xmlns:jsp") == false) {
                    String quote = DOUBLE_QUOTE;
                    writer.print(" ");
                    writer.print(attr.getName());
                    writer.print("=");
                    String value = attr.getValue();
                    if (value.indexOf('"') != -1) {
                        quote = SINGLE_QUOTE;
                    }
                    writer.print(quote);
                    writer.print(value);
                    writer.print(quote);
                }                
            }
            
            if (element.hasChildNodes()) {
                writer.print(">\");");
                writer.println();     
            } 
            else {
                writer.print("/>\");");
                writer.println();     
            }
            writeDebugStartEnd(writer);
        }
    }

    public void endGeneration(int section, JavaCodeWriter writer)  throws JspCoreException {
        if (section == CodeGenerationPhase.METHOD_SECTION) {
            if (element.hasChildNodes()) {
                writeDebugEndBegin(writer);
                writer.print("out.write(\"</");
                writer.print(element.getTagName());
                writer.print(">\");");
                writer.println();     
                writeDebugEndEnd(writer);
            }
        }
    }

}
