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
import java.util.List;

import org.w3c.dom.Node;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;

public class PluginGenerator extends PageTranslationTimeGenerator {
    private static final String NS_PLUGIN_URL = "http://java.sun.com/products/plugin/";
    private static final String IE_PLUGIN_URL = "http://java.sun.com/products/plugin/1.2.2/jinstall-1_2_2-win.cab#Version=1,2,2,0";
    private static final String IE_CLASS_ID = "clsid:8AD9C840-044E-11D1-B3E9-00805F499D93";
    
    private MethodWriter attributesWriter = null;
    private MethodWriter bodyWriter = null;
    
    public PluginGenerator() {
        super(new String[] {"type", "code", "name", "hspace", "vspace", "align", "iepluginurl", "nspluginurl", "codebase", "archive", "jreversion"});
    }
    
    public void startGeneration(int section, JavaCodeWriter writer) throws JspCoreException {}

    public void endGeneration(int section, JavaCodeWriter writer)  throws JspCoreException {
        if (section == CodeGenerationPhase.METHOD_SECTION) {
            String type = getAttributeValue("type");
            String code = getAttributeValue("code");
            String name = getAttributeValue("name");
            String height = getAttributeValue("height");
            String width = getAttributeValue("width");
            String hspace = getAttributeValue("hspace");
            String vspace = getAttributeValue("vspace");
            String align = getAttributeValue("align");
            String iepluginurl = getAttributeValue("iepluginurl");
            String nspluginurl = getAttributeValue("nspluginurl");
            String codebase = getAttributeValue("codebase");
            String archive = getAttributeValue("archive");
            String jreversion = getAttributeValue("jreversion");
            //PK65013 - start
            String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
            if (isTagFile && jspOptions.isModifyPageContextVariable()) {
                pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
            }
            //PK65013 - end

            HashMap jspAttributes = (HashMap)persistentData.get("jspAttributes");
            ArrayList jspAttributeList = null;
            if (jspAttributes != null) {
                jspAttributeList = (ArrayList)jspAttributes.get(element);
            }
            else {
                jspAttributeList = new ArrayList();
            }
            
            String widthStr = null;
            if (width == null) {
                widthStr = findAttributeValue("width", jspAttributeList);
            }
            else {
                widthStr = GeneratorUtils.attributeValue(width, false, String.class, jspConfiguration, isTagFile, pageContextVar); //PK65013
            }

            String heightStr = null;
            if (height == null) {
                heightStr = findAttributeValue("height", jspAttributeList);
            }
            else {
                heightStr = GeneratorUtils.attributeValue(height, false, String.class, jspConfiguration, isTagFile, pageContextVar); //PK65013
            }
            
            if (attributesWriter != null)
                writer.printMultiLn(attributesWriter.toString());

            if (iepluginurl == null)
                iepluginurl = IE_PLUGIN_URL;
            if (nspluginurl == null)
                nspluginurl = NS_PLUGIN_URL;

            HashMap jspParams = (HashMap)persistentData.get("jspParams");
            ArrayList jspParamList = null;
            if (jspParams != null) {
                jspParamList = (ArrayList)jspParams.get(element);
            }

            writeDebugStartBegin(writer);            
            // XXX - Fixed a bug here - width and height can be set 
            // dynamically.  Double-check if this generation is correct.

            // IE style plugin
            // <OBJECT ...>
            // First compose the runtime output string 
    		//Start changes Defect PK28029
    		String ieClassId = IE_CLASS_ID;
            if (jspOptions != null && jspOptions.getIeClassId() != null) {
            	ieClassId = jspOptions.getIeClassId();
            }
            String s0 = "<OBJECT classid=\"" + ieClassId + "\"" + makeAttr("name", name);
			//End changes Defect PK28029

            String s1 = "";
            if (width != null) {
                s1 = " + \" width=\\\"\" + " + widthStr + " + \"\\\"\"";
            }

            String s2 = "";
            if (height != null) {
                s2 = " + \" height=\\\"\" + " + heightStr + " + \"\\\"\"";
            }

            String s3 =
                makeAttr("hspace", hspace)
                    + makeAttr("vspace", vspace)
                    + makeAttr("align", align)
                    + makeAttr("codebase", iepluginurl)
                    + '>';

            // Then print the output string to the java file
            writer.println("out.write(" + GeneratorUtils.quote(s0) + s1 + s2 + " + " + GeneratorUtils.quote(s3) + ");");
            writer.println("out.write(\"\\n\");");

            // <PARAM > for java_code
            s0 = "<PARAM name=\"java_code\"" + makeAttr("value", code) + '>';
            writer.println("out.write(" + GeneratorUtils.quote(s0) + ");");
            writer.println("out.write(\"\\n\");");

            // <PARAM > for java_codebase
            if (codebase != null) {
                s0 = "<PARAM name=\"java_codebase\"" + makeAttr("value", codebase) + '>';
                writer.println("out.write(" + GeneratorUtils.quote(s0) + ");");
                writer.println("out.write(\"\\n\");");
            }

            // <PARAM > for java_archive
            if (archive != null) {
                s0 = "<PARAM name=\"java_archive\"" + makeAttr("value", archive) + '>';
                writer.println("out.write(" + GeneratorUtils.quote(s0) + ");");
                writer.println("out.write(\"\\n\");");
            }

            // <PARAM > for type
            s0 =
                "<PARAM name=\"type\""
                    + makeAttr(
                        "value",
                        "application/x-java-" + type + ";" + ((jreversion == null) ? "" : "version=" + jreversion))
                    + '>';
            writer.println("out.write(" + GeneratorUtils.quote(s0) + ");");
            writer.println("out.write(\"\\n\");");

            /*
             * generate a <PARAM> for each <jsp:param> in the plugin body
             */
            if (jspParamList != null) {
                for (Iterator itr = jspParamList.iterator(); itr.hasNext();) {
                    ParamGenerator.JspParam jspParam = (ParamGenerator.JspParam)itr.next();
                    String paramName = jspParam.getName();
                    
                    if (paramName.equalsIgnoreCase("object"))
                        paramName = "java_object";
                    else if (paramName.equalsIgnoreCase("type"))
                        paramName = "java_type";
                        
                    writer.println(
                            "out.write( \"<PARAM name=\\\""
                                + GeneratorUtils.escape(paramName)
                                + "\\\" value=\\\"\" + "
                                + jspParam.getValue()
                                + " + \"\\\">\" );");
                    writer.println("out.write(\"\\n\");");
                }
            }

            /*
             * Netscape style plugin part
             */
            writer.println("out.write(" + GeneratorUtils.quote("<COMMENT>") + ");");
            writer.println("out.write(\"\\n\");");
            s0 =
                "<EMBED"
                    + makeAttr(
                        "type",
                        "application/x-java-" + type + ";" + ((jreversion == null) ? "" : "version=" + jreversion))
                    + makeAttr("name", name);

            // s1 and s2 are the same as before.

            s3 =
                makeAttr("hspace", hspace)
                    + makeAttr("vspace", vspace)
                    + makeAttr("align", align)
                    + makeAttr("pluginspage", nspluginurl)
                    + makeAttr("java_code", code)
                    + makeAttr("java_codebase", codebase)
                    + makeAttr("java_archive", archive);
            writer.println("out.write(" + GeneratorUtils.quote(s0) + s1 + s2 + " + " + GeneratorUtils.quote(s3) + ");");

            /*
             * Generate a 'attr = "value"' for each <jsp:param> in plugin body
             */
            if (jspParamList != null) {
                for (Iterator itr = jspParamList.iterator(); itr.hasNext();) {
                    ParamGenerator.JspParam jspParam = (ParamGenerator.JspParam)itr.next();
                    String paramName = jspParam.getName();
                    
                    if (paramName.equalsIgnoreCase("object"))
                        paramName = "java_object";
                    else if (paramName.equalsIgnoreCase("type"))
                        paramName = "java_type";
                    writer.println("out.write( \" "
                                   + GeneratorUtils.escape(paramName)
                                   + "=\\\"\" + "
                                   + jspParam.getValue()
                                   + " + \"\\\"\" );");
                }
            }

            writer.println("out.write(" + GeneratorUtils.quote("/>") + ");");
            writer.println("out.write(\"\\n\");");

            writer.println("out.write(" + GeneratorUtils.quote("<NOEMBED>") + ");");
            writer.println("out.write(\"\\n\");");

            /*
             * Fallback
             */
            if (bodyWriter != null) {
                writeDebugStartEnd(writer);            
                writer.printMultiLn(bodyWriter.toString());
                writeDebugEndBegin(writer);            
                writer.println("out.write(\"\\n\");");
            }

            writer.println("out.write(" + GeneratorUtils.quote("</NOEMBED>") + ");");
            writer.println("out.write(\"\\n\");");
            writer.println("out.write(" + GeneratorUtils.quote("</COMMENT>") + ");");
            writer.println("out.write(\"\\n\");");
            writer.println("out.write(" + GeneratorUtils.quote("</OBJECT>") + ");");
            writer.println("out.write(\"\\n\");");
            
            if (bodyWriter != null) 
                writeDebugEndEnd(writer);            
            else
                writeDebugStartEnd(writer);            
        }
    }
    
    public JavaCodeWriter getWriterForChild(int section, Node childElement) throws JspCoreException {
        JavaCodeWriter writerForChild = super.getWriterForChild(section, childElement);
        if (writerForChild == null) {
            if (section == CodeGenerationPhase.METHOD_SECTION) {
                if (childElement.getNodeType() == Node.ELEMENT_NODE) {
                    if (childElement.getNamespaceURI().equals(Constants.JSP_NAMESPACE) && 
                        childElement.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                        if (attributesWriter == null)
                            attributesWriter = new MethodWriter();
                        writerForChild = attributesWriter;
                    }
                    else {
                        if (bodyWriter == null) 
                            bodyWriter = new MethodWriter();
                        writerForChild = bodyWriter;
                    }
                }
                else {
                    if (bodyWriter == null) 
                        bodyWriter = new MethodWriter();
                    writerForChild = bodyWriter;
                }
            }
        }
        
        return writerForChild;
    }
    
    private String makeAttr(String attr, String value) {
        if (value == null)
            return "";

        return " " + attr + "=\"" + value + '\"';
    }
    
    private String findAttributeValue(String name, List jspAttributeList) {
        String value = "";
        for (Iterator itr = jspAttributeList.iterator(); itr.hasNext();) {
            AttributeGenerator.JspAttribute jspAttribute = (AttributeGenerator.JspAttribute)itr.next();
            if (jspAttribute.getName().equals(name)) {
                value = jspAttribute.getVarName();
            }
        }
        return (value);
    }
}
