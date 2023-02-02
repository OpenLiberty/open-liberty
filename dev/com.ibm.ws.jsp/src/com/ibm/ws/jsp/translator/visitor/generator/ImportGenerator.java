/*******************************************************************************
 * Copyright (c) 1997, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsp.translator.visitor.generator;

import java.util.StringTokenizer;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.PagesVersionHandler;

public class ImportGenerator extends CodeGeneratorBase {

	public void startGeneration(int section, JavaCodeWriter writer)
		throws JspCoreException {
		if (section == CodeGenerationPhase.IMPORT_SECTION) {
			NamedNodeMap attributes = element.getAttributes();
			if (attributes != null) {
				for (int i = 0; i < attributes.getLength(); i++) {
					Node attribute = attributes.item(i);
					String directiveName = attribute.getNodeName();
					String directiveValue = attribute.getNodeValue();
					if (directiveName.equals("import")) {
						StringTokenizer tokenizer = new StringTokenizer(directiveValue, ",");
						writeDebugStartBegin(writer);
						while (tokenizer.hasMoreTokens()) {
							writer.println("import " + (String) tokenizer.nextToken() + ";");
						}
						writeDebugStartEnd(writer);
					}
				}
			}
		} else if (section == CodeGenerationPhase.STATIC_SECTION && PagesVersionHandler.isPages31OrHigherLoaded()) {
			NamedNodeMap attributes = element.getAttributes();
			if (attributes != null) {
				for (int i = 0; i < attributes.getLength(); i++) {
					Node attribute = attributes.item(i);
					String directiveName = attribute.getNodeName();
					String directiveValue = attribute.getNodeValue();
					if (directiveName.equals("import")) {
						StringTokenizer tokenizer = new StringTokenizer(directiveValue, ",");
						String STATIC_IMPORT_PREPPEND = "static ";
						writeDebugStartBegin(writer);
						while (tokenizer.hasMoreTokens()) {
							String singleImport = ((String) tokenizer.nextToken()).trim();
							if (singleImport.startsWith(STATIC_IMPORT_PREPPEND)) {
								writer.println("importStaticList.add(\"" + singleImport.substring(STATIC_IMPORT_PREPPEND.length()).trim() + "\");");
							} else if(singleImport.endsWith(".*")){
								writer.println("importPackageList.add(\"" + singleImport.replace(".*", "") + "\");");
							} else {
								writer.println("importClassList.add(\"" + singleImport + "\");");
							}

						}
						writeDebugStartEnd(writer);
					}
				}
			}
		}
	}

	public void endGeneration(int section, JavaCodeWriter writer)
		throws JspCoreException {
	}

}
