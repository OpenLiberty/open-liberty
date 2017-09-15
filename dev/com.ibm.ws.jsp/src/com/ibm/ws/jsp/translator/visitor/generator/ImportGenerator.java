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

import java.util.StringTokenizer;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.ibm.ws.jsp.JspCoreException;

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
		}
	}

	public void endGeneration(int section, JavaCodeWriter writer)
		throws JspCoreException {
	}

}
