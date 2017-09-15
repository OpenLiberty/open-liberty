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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;

public class UseBeanGenerator extends PageTranslationTimeGenerator {
    private MethodWriter attributesWriter = new MethodWriter();
    private MethodWriter bodyWriter = new MethodWriter();
    
    public UseBeanGenerator() {
        super(new String[] {"id", "scope", "class", "type"});
    }
    
    public void startGeneration(int section, JavaCodeWriter writer) throws JspCoreException {}

    public void endGeneration(int section, JavaCodeWriter writer)  throws JspCoreException {
        //PK65013 - start
        String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
        if (isTagFile && jspOptions.isModifyPageContextVariable()) {
            pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
        }
        //PK65013 - end
        if (section == CodeGenerationPhase.METHOD_SECTION) {
            String name = getAttributeValue("id");
            String scope = getAttributeValue("scope");
            String klass = getAttributeValue("class");
            String type = getAttributeValue("type");
            String beanName = null; 
            Attr beanNameAttr = element.getAttributeNode("beanName");
            if (beanNameAttr != null)
                beanName = beanNameAttr.getValue();
            
            //Start PK60570
            if(klass != null){
            	klass = klass.replaceAll("&gt;", ">");
            	klass = klass.replaceAll("&lt;", "<");
            }
            if(type != null){
            	type = type.replaceAll("&gt;", ">");
            	type = type.replaceAll("&lt;", "<");
            }
            else // if unspecified, use class as type of bean 
                type = klass;
            //End PK60570

            String scopename = "PageContext.PAGE_SCOPE"; // Default to page
            String lock = pageContextVar;  //PK65013

            if ("request".equals(scope)) {
                scopename = "PageContext.REQUEST_SCOPE";
                lock = "request";
            }
            else if ("session".equals(scope)) {
                scopename = "PageContext.SESSION_SCOPE";
                lock = "session";
            }
            else if ("application".equals(scope)) {
                scopename = "PageContext.APPLICATION_SCOPE";
                lock = "application";
            }

            writeDebugStartBegin(writer);
            // Declare bean
            writer.print(type);
            writer.print(" ");
            writer.print(name);
            writer.print(" = null;");
            writer.println();

            // Lock while getting or creating bean
            writer.print("synchronized (");
            writer.print(lock);
            writer.print(") {");
            writer.println();

            // Locate bean from context
            writer.print(name);
            writer.print(" = (");
            writer.print(type);
            //PK65013 change pageContext variable to customizable one.
            writer.print(") "+pageContextVar+".getAttribute(");
            writer.print(GeneratorUtils.quote(name));
            writer.print(", ");
            writer.print(scopename);
            writer.print(");");
            writer.println();

            // Create bean
            /*
             * Check if bean is alredy there
             */
            writer.print("if (");
            writer.print(name);
            writer.print(" == null) {");
            writer.println();
            if (klass == null && beanName == null) {
                /*
                 * If both class name and beanName is not specified, the bean
                 * must be found locally, otherwise it's an error
                 */
                writer.print("throw new java.lang.InstantiationException(\"bean ");
                writer.print(name);
                writer.print(" not found within scope\");");
                writer.println();
                writer.println("}");                
                // End of lock block
                writer.println("}");
            }
            else {
                if (beanName == null) {
                    HashMap jspAttributes = (HashMap)persistentData.get("jspAttributes");
                    if (jspAttributes != null) {
                        ArrayList jspAttributeList = (ArrayList)jspAttributes.get(element);

                        if (jspAttributeList != null) {                
                            for (Iterator itr = jspAttributeList.iterator(); itr.hasNext();) {
                                AttributeGenerator.JspAttribute jspAttribute = (AttributeGenerator.JspAttribute)itr.next();
                                if (jspAttribute.getName().equals("beanName")) {
                                    beanName = jspAttribute.getVarName();
                                    writer.printMultiLn(attributesWriter.toString());
                                }
                            }
                        }
                    }
                }
                else {
                    beanName = GeneratorUtils.attributeValue(beanName, false, String.class, jspConfiguration, isTagFile, pageContextVar); //PK65013
                }
                /*
                 * Instantiate bean if not there
                 */

                // Start 174648   SDJ
                // a.  if beanName is not null, we must use Beans.instantiate()
                // 1.  if beanName IS null, see if .ser resource exists;  if so, use Beans.instantiate() ...
                // 2.  ... else, see if class can be instantiated, and if it can be instantiated then
                //     we can use the new operator to create the bean, otherwise we must use Beans.instantiate().
                boolean useInstantiate=true;
                String className = beanName;
                if (beanName == null) {                    
                    className = klass;  // klass can't be null if beanName is null
                    String serName = className.replace('.','/').concat(".ser");
                    ClassLoader cl = ctxt.getJspClassloaderContext().getClassLoader();
                    InputStream ins=cl.getResourceAsStream(serName);
                    if (ins == null) {
                        // .ser resource does not exist.
                        // see if we can load this class; if we can then we may use new.
                        Class cls = null;
                        try {
                            cls = cl.loadClass (className);
                            useInstantiate=false;
                        }
                        catch (ClassNotFoundException e) {
                        }
                    }
                }

                if (useInstantiate==false) {
                    writer.println(name+" =  new "+ className +"();");
                }
                else {  // must use Beans.instantiate()
                    if (beanName == null) {
                        // klass can't be null if beanName is null
                        className = GeneratorUtils.quote(klass);
                    }
                    // End 174648   SDJ
                    writer.println("try {");
                    writer.print(name);
                    writer.print(" = (");
                    writer.print(type);
                    writer.print(") java.beans.Beans.instantiate(");
                    writer.print("this.getClass().getClassLoader(), ");
                    writer.print(className);
                    writer.print(");");
                    writer.println();     
                    /*
                     * Note: Beans.instantiate throws ClassNotFoundException
                     * if the bean class is abstract.
                     */
                    writer.println("} catch (ClassNotFoundException exc) {");
                    writer.println("throw new InstantiationException(exc.getMessage());");
                    writer.println("} catch (Exception exc) {");
                    writer.print("throw new ServletException(");
                    writer.print("\"Cannot create bean of class \" + ");
                    writer.print(className);
                    writer.print(", exc);");
                    writer.println();     
                    writer.println("}"); // close of try
                }
                /*
                 * Set attribute for bean in the specified scope
                 */
                //PK65013 change pageContext variable to customizable one.
                writer.print(pageContextVar+".setAttribute(");
                writer.print(GeneratorUtils.quote(name));
                writer.print(", ");
                writer.print(name);
                writer.print(", ");
                writer.print(scopename);
                writer.print(");");
                writer.println();     
                
                // Write body of tag
                // 227804 Start
                String body = bodyWriter.toString();
                if (body.length()>0)
                	writeDebugStartEnd(writer);
                // 227804 End
                
                writer.printMultiLn(body);
                
                // 227804 Start
                if (body.length()>0)
                	writeDebugEndBegin(writer);
                // 227804 End
                
                writer.println("}");

                // End of lock block
                writer.println("}");
                // 227804 Start
                if (body.length()>0)
                	writeDebugEndEnd(writer);
                else
                    writeDebugStartEnd(writer);
                // 227804 End
            }
        }
    }
    
    public JavaCodeWriter getWriterForChild(int section, Node childElement) throws JspCoreException {
        JavaCodeWriter writerForChild = super.getWriterForChild(section, childElement);
        if (writerForChild == null) {
            if (section == CodeGenerationPhase.METHOD_SECTION) {
                if (childElement.getNodeType() == Node.ELEMENT_NODE) {
                    if (childElement.getNamespaceURI().equals(Constants.JSP_NAMESPACE) && 
                        childElement.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                        writerForChild = attributesWriter;
                    }
                    else {
                        writerForChild = bodyWriter;
                    }
                }
                else {
                    writerForChild = bodyWriter;
                }
            }
        }
        
        return writerForChild;
    }
}
