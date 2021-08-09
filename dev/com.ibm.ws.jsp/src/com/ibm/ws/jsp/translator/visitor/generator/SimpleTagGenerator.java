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

import java.util.Map;

import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.taglib.TagClassInfo;
import com.ibm.ws.jsp.taglib.TagLibraryCache;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.validator.ValidateResult;
import com.ibm.wsspi.jsp.context.JspCoreContext;

public class SimpleTagGenerator extends BaseTagGenerator {
    private FragmentHelperClassWriter.FragmentWriter fragmentBodyWriter = null;
    private JspVisitorInputMap inputMap=null; //jsp2.1work

    public SimpleTagGenerator(
        int nestingLevel,
        boolean isTagFile,
        boolean hasBody,
        boolean hasJspBody,
        String tagHandlerVar,
        Element element,
        TagLibraryCache tagLibraryCache,
        JspConfiguration jspConfiguration,
        JspCoreContext ctxt,
        TagClassInfo tagClassInfo,
        TagInfo ti,
        Map persistentData,
        ValidateResult.CollectedTagData collectedTagData,
        FragmentHelperClassWriter fragmentHelperClassWriter,
        JspOptions jspOptions,
        JspVisitorInputMap inputMap) {
            
        super(nestingLevel,
              isTagFile,
              hasBody,
              hasJspBody,
              tagHandlerVar,
              element,
              tagLibraryCache,
              jspConfiguration,
              ctxt,
              tagClassInfo,
              ti,
              persistentData,
              collectedTagData,
              fragmentHelperClassWriter,
              jspOptions);
        this.inputMap=inputMap;
    }

    public MethodWriter generateTagStart() throws JspCoreException {
        MethodWriter tagStartWriter = new MethodWriter();
        declareScriptingVars(tagStartWriter, VariableInfo.AT_BEGIN);
        saveScriptingVars(tagStartWriter, VariableInfo.AT_BEGIN);

        if (!jspOptions.isDisableResourceInjection()){		//PM06063

            // have CDI create and inject the managed object
            tagStartWriter.print ("com.ibm.ws.managedobject.ManagedObject " + tagHandlerVar + "_mo = ");
            tagStartWriter.print ("_jspx_iaHelper.inject(");
            tagStartWriter.print (tagClassInfo.getTagClassName() + ".class");
            tagStartWriter.println (");");
        
            // get the underlying object from the managed object
            tagStartWriter.print(tagClassInfo.getTagClassName());
            tagStartWriter.print(" ");
            tagStartWriter.print(tagHandlerVar);   
            tagStartWriter.print(" = ");           
            tagStartWriter.println("("+tagClassInfo.getTagClassName()+")"+tagHandlerVar+"_mo.getObject();"); 

            tagStartWriter.print ("_jspx_iaHelper.doPostConstruct(");
            tagStartWriter.print (tagHandlerVar);
            tagStartWriter.println (");");
    	
            tagStartWriter.print ("_jspx_iaHelper.addTagHandlerToCdiMap(");
            tagStartWriter.print (tagHandlerVar + ", " + tagHandlerVar + "_mo");
            tagStartWriter.println (");");

        } else {
            // not using CDI
            tagStartWriter.print(tagClassInfo.getTagClassName());
            tagStartWriter.print(" ");
            tagStartWriter.print(tagHandlerVar);
            tagStartWriter.print(" = ");
            tagStartWriter.print("new ");
            tagStartWriter.print(tagClassInfo.getTagClassName());
            tagStartWriter.println("();");

        }
        
        generateSetParent(tagStartWriter);
        return tagStartWriter;
    }

    public MethodWriter generateTagMiddle() throws JspCoreException {
        MethodWriter tagMiddleWriter = new MethodWriter();
        int methodNesting =  ((Integer)persistentData.get("methodNesting")).intValue();

	  // JspIdConsumer (after context has been set)
        if (tagClassInfo.implementsJspIdConsumer()) {
        	tagMiddleWriter.print(tagHandlerVar);
        	tagMiddleWriter.print(".setJspId(\"");
        	tagMiddleWriter.print(createJspId(inputMap));
        	tagMiddleWriter.println("\");");
        }
        
	  if (hasJspBody == false) {
            if (hasBody) {
                createBodyWriter(methodNesting, tagMiddleWriter);
            }
        }
        else {
            createBodyWriter(methodNesting, tagMiddleWriter);
        }
        return tagMiddleWriter;
    }

    public MethodWriter generateTagEnd() throws JspCoreException {
        generateJspAttributeSetters();
        MethodWriter tagEndWriter = new MethodWriter();
        int methodNesting =  ((Integer)persistentData.get("methodNesting")).intValue();
        tagEndWriter.print(tagHandlerVar);
        tagEndWriter.println(".doTag();");
        
        if (!jspOptions.isDisableResourceInjection()){		//PM06063
        	tagEndWriter.print ("_jspx_iaHelper.doPreDestroy(");
        	tagEndWriter.print (tagHandlerVar);
        	tagEndWriter.println (");");
        	
        	tagEndWriter.print ("_jspx_iaHelper.cleanUpTagHandlerFromCdiMap(");
        	tagEndWriter.print (tagHandlerVar);
        	tagEndWriter.println (");");
        }

        restoreScriptingVars(tagEndWriter, VariableInfo.AT_BEGIN);
        syncScriptingVars(tagEndWriter, VariableInfo.AT_BEGIN);
        declareScriptingVars(tagEndWriter, VariableInfo.AT_END);
        syncScriptingVars(tagEndWriter, VariableInfo.AT_END);
        if (hasJspBody == false) {
            if (hasBody) {
                fragmentHelperClassWriter.closeFragment(fragmentBodyWriter, methodNesting);
            }
        }
        return tagEndWriter;
    }

    private void createBodyWriter(int methodNesting, MethodWriter tagMiddleWriter) throws JspCoreException {
        //PK65013 start
        String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
        if (isTagFile && jspOptions.isModifyPageContextVariable()) {
            pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
        }        
        //PK65013 end
        tagMiddleWriter.print(tagHandlerVar);
        tagMiddleWriter.print(".setJspBody(");
        fragmentBodyWriter = fragmentHelperClassWriter.openFragment(element, tagHandlerVar, methodNesting, pageContextVar);
        tagMiddleWriter.print("new " + fragmentHelperClassWriter.getClassName());
        
        if (jspOptions.isUsePageTagPool() ||
            jspOptions.isUseThreadTagPool()) {
            tagMiddleWriter.print("(_jspx_TagLookup, " + fragmentBodyWriter.getId() + ", "+pageContextVar+", ");//PK65013
        }
        else {
            tagMiddleWriter.print("( " + fragmentBodyWriter.getId() + ", "+pageContextVar+", "); //PK65013
        } 
        String pushBodyCountVar = (String)persistentData.get("pushBodyCountVar");
        // defect 363508 begin
        if (pushBodyCountVar!=null) {
        	String pushBodyCountVarToUse=(String)persistentData.get("pushBodyCountVarArgument"+this.hashCode());
        	if (pushBodyCountVarToUse==null) {
        		pushBodyCountVarToUse=(String)persistentData.get("pushBodyCountVarDeclaration");
        	}
        	if (pushBodyCountVarToUse==null) {
        		pushBodyCountVarToUse=(String)persistentData.get("pushBodyCountVarDeclarationBase");
        	}
        	if (pushBodyCountVarToUse!=null) {
        		pushBodyCountVar=pushBodyCountVarToUse;
        	}            	
        }
        // defect 363508 end
        tagMiddleWriter.print(tagHandlerVar + ", " + pushBodyCountVar + ")" );
        tagMiddleWriter.println(");");
        persistentData.put("pushBodyCountVarArgument"+this.hashCode(),null); // defect 363508
        
        //PK60565 commented out the next line - problem with nested TryCatchFinally tags
        //persistentData.put("pushBodyCountVarDeclaration",null); // defect 363508
        //PK60565 end
    }

    public JavaCodeWriter getWriterForChild(int section, Node childElement) throws JspCoreException {
        JavaCodeWriter writerForChild = null;
        if (section == CodeGenerationPhase.METHOD_SECTION) {
            if (childElement.getNodeType() == Node.ELEMENT_NODE) {
                if (childElement.getNamespaceURI() != null &&
                    childElement.getNamespaceURI().equals(Constants.JSP_NAMESPACE) && 
                    childElement.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                    writerForChild = (JavaCodeWriter)attributeWriterMap.get(childElement);    
                }
                else {
                    if (fragmentBodyWriter != null)
                        writerForChild = fragmentBodyWriter;
                    else                
                        writerForChild = bodyWriter;
                }
            }
            else {
                if (fragmentBodyWriter != null)
                    writerForChild = fragmentBodyWriter;
                else                
                    writerForChild = bodyWriter;
            }
        }
        return (writerForChild);
    }
    
    public boolean fragmentWriterUsed() {
        return (fragmentBodyWriter != null) ? true : false;
    }
    
    public void generateInitialization(JavaCodeWriter writer) {}
    public void generateFinally(JavaCodeWriter writer) {}
}
