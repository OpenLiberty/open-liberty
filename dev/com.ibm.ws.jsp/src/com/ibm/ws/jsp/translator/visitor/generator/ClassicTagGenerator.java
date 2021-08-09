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

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.taglib.TagClassInfo;
import com.ibm.ws.jsp.taglib.TagLibraryCache;
import com.ibm.ws.jsp.translator.visitor.validator.ValidateResult;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.wsspi.jsp.context.JspCoreContext;

public class ClassicTagGenerator extends BaseTagGenerator {
    private boolean reuseTag = false;
    private boolean genTagInMethod = false;
    private boolean existingTag = false;

    private String baseVar = null;
    private String tagEvalVar = null;
    private String tagPushBodyCountVar = null;
    private InitTaglibLookupWriter initTaglibLookupWriter = null; //247815
    private CleanupTaglibLookupWriter cleanupTaglibLookupWriter = null; //247815
    //PK60565 pushBodyCountVarDeclOrig has a value if we are within a TryCatchFinally tag.  Need to restore original value in case of nested tags.
    private String pushBodyCountVarDeclOrig = null; //PK60565
    private JspVisitorInputMap inputMap=null; //jsp2.1work

    public ClassicTagGenerator(
        boolean reuseTag,
        boolean genTagInMethod,
        boolean existingTag,
        String baseVar,
        String tagEvalVar,
        String tagPushBodyCountVar,
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

        super(
            nestingLevel,
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
            
        this.reuseTag = reuseTag;
        this.genTagInMethod = genTagInMethod;
        this.existingTag = existingTag;

        this.baseVar = baseVar;
        this.tagEvalVar = tagEvalVar;
        this.tagPushBodyCountVar = tagPushBodyCountVar;
        this.inputMap=inputMap;
        
        //247815 Start
        if (reuseTag) {
        	cleanupTaglibLookupWriter = (CleanupTaglibLookupWriter)persistentData.get("CleanupTaglibLookupWriter");
            initTaglibLookupWriter = (InitTaglibLookupWriter)persistentData.get("InitTaglibLookupWriter");
        }
        //247815 End
    }

    public MethodWriter generateTagStart() throws JspCoreException {
        MethodWriter tagStartWriter = new MethodWriter();
        declareScriptingVars(tagStartWriter, VariableInfo.AT_BEGIN);
        saveScriptingVars(tagStartWriter, VariableInfo.AT_BEGIN);

        if (reuseTag == false || tagClassInfo.implementsJspIdConsumer()) {//jsp2.1work
            
            // LIDB4147-24
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
            
            tagStartWriter.println();
        }
        else {
        	//PM26777 start
            if (genTagInMethod || (isFragment && !existingTag)) {
                tagStartWriter.print(tagClassInfo.getTagClassName());
                tagStartWriter.print(" ");
            }
            //PM26777 end
            //247815 Start
            tagStartWriter.print(tagHandlerVar);
            tagStartWriter.println(" = (" + tagClassInfo.getTagClassName() + ")_jspx_TagLookup.get(\"" + tagHandlerVar + "\");");
            //247815 End
        }
        generateSetParent(tagStartWriter);
        //PK60565 saves original pushBodyCountVarDeclaration value
        if (tagClassInfo.implementsTryCatchFinally()) {
            pushBodyCountVarDeclOrig = (String)persistentData.get("pushBodyCountVarDeclaration");
        } //PK60565 end
        return tagStartWriter;
    }

    public MethodWriter generateTagMiddle() throws JspCoreException {
        MethodWriter tagMiddleWriter = new MethodWriter();
        String pushBodyCountVar = (String)persistentData.get("pushBodyCountVar");
        //PK65013 - start
        String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
        if (isTagFile && jspOptions.isModifyPageContextVariable()) {
            pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
        }
        //PK65013 - end
        // JspIdConsumer (after context has been set)
        if (tagClassInfo.implementsJspIdConsumer()) {
        	tagMiddleWriter.print(tagHandlerVar);
        	tagMiddleWriter.print(".setJspId(\"");
        	tagMiddleWriter.print(createJspId(inputMap));
        	tagMiddleWriter.println("\");");
        }
        if (tagClassInfo.implementsTryCatchFinally()) {
            //begin 230150: reuse TryCatchFinally counter object when pooling is enabled.
            //PM26777 start
            if ((jspOptions.isUsePageTagPool() == false && jspOptions.isUseThreadTagPool() == false) || genTagInMethod == true || (isFragment && !existingTag)){ 
        		tagMiddleWriter.print("int[] ");
            }	
            //PM26777 end
            //end 230150: reuse TryCatchFinally counter object when pooling is enabled.
        	tagMiddleWriter.print(tagPushBodyCountVar);
            tagMiddleWriter.println(" = new int[] { 0 };");
            tagMiddleWriter.println("try {");
            persistentData.put("pushBodyCountVarDeclaration", tagPushBodyCountVar);  // defect 363508
        }

        if (genTagInMethod == false && reuseTag && isFragment == false) {
            tagMiddleWriter.print(tagEvalVar);
        }
        else {
            tagMiddleWriter.print("int ");
            tagMiddleWriter.print(tagEvalVar);
        }

        tagMiddleWriter.print(" = ");
        tagMiddleWriter.print(tagHandlerVar);
        tagMiddleWriter.println(".doStartTag();");
        
        if (!tagClassInfo.implementsBodyTag()) {
            syncScriptingVars(tagMiddleWriter, VariableInfo.AT_BEGIN);
        }

        if (hasBody || hasJspBody) {
            tagMiddleWriter.print("if (");
            tagMiddleWriter.print(tagEvalVar);
            tagMiddleWriter.println(" != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {");
            
            // PM48052 start
            // If doStartTag does a pushBody(Writer writer) then we need to make sure we get the
            // correct writer.
            tagMiddleWriter.println("out = "+pageContextVar+".getOut();");
            // PM48052 end

            declareScriptingVars(tagMiddleWriter, VariableInfo.NESTED);
            saveScriptingVars(tagMiddleWriter, VariableInfo.NESTED);

            if (tagClassInfo.implementsBodyTag()) {
                tagMiddleWriter.print("if (");
                tagMiddleWriter.print(tagEvalVar);
                tagMiddleWriter.println(" != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {");
                //PK65013 change pageContext variable to customizable one.
                tagMiddleWriter.println("out = "+pageContextVar+".pushBody();");

                if (tagClassInfo.implementsTryCatchFinally()) {
                    tagMiddleWriter.print(tagPushBodyCountVar);
                    tagMiddleWriter.print("[0]++;");
                    tagMiddleWriter.println();
                }
                else if (pushBodyCountVar != null) {
                    tagMiddleWriter.print(pushBodyCountVar);
                    tagMiddleWriter.print("[0]++;");
                    tagMiddleWriter.println();
                }

                tagMiddleWriter.print(tagHandlerVar);
                tagMiddleWriter.print(".setBodyContent((javax.servlet.jsp.tagext.BodyContent) out);");
                tagMiddleWriter.println();
                tagMiddleWriter.print(tagHandlerVar);
                tagMiddleWriter.print(".doInitBody();");
                tagMiddleWriter.println();

                tagMiddleWriter.println("}");

                syncScriptingVars(tagMiddleWriter, VariableInfo.AT_BEGIN);
                syncScriptingVars(tagMiddleWriter, VariableInfo.NESTED);

            }
            else {
                syncScriptingVars(tagMiddleWriter, VariableInfo.NESTED);
            }

            if (tagClassInfo.implementsIterationTag()) {
                tagMiddleWriter.println("do {");
            }
        }
        return tagMiddleWriter;
    }

    public MethodWriter generateTagEnd() throws JspCoreException {
        //PK65013 - start
        String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
        if (isTagFile && jspOptions.isModifyPageContextVariable()) {
            pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
        }
        //PK65013 - end
        generateJspAttributeSetters();
        MethodWriter tagEndWriter = new MethodWriter();
        String pushBodyCountVar = (String)persistentData.get("pushBodyCountVar");
        int methodNesting =  ((Integer)persistentData.get("methodNesting")).intValue();
        if (hasBody || hasJspBody) {
            if (tagClassInfo.implementsIterationTag()) {
				//PK31135
				if (!jspOptions.isUseIterationEval()) { //PK31135
	                tagEndWriter.print("int evalDoAfterBody = ");
	                tagEndWriter.print(tagHandlerVar);
	                tagEndWriter.println(".doAfterBody();");
	
	                syncScriptingVars(tagEndWriter, VariableInfo.AT_BEGIN);
	                syncScriptingVars(tagEndWriter, VariableInfo.NESTED);
	
	                tagEndWriter.println("if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN) break;");
	
	                tagEndWriter.println("} while (true);");
				//PK31135
				} else {
					tagEndWriter.println("} while (");
					tagEndWriter.println(tagHandlerVar);
					tagEndWriter.println(".doAfterBody() == javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN);");
				} //PK31135
            }

            restoreScriptingVars(tagEndWriter, VariableInfo.NESTED);

            if (tagClassInfo.implementsBodyTag()) {
                tagEndWriter.print("if (");
                tagEndWriter.print(tagEvalVar);
                //PK65013 change pageContext variable to customizable one.
                tagEndWriter.println(" != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) out = "+pageContextVar+".popBody();");
                if (tagClassInfo.implementsTryCatchFinally()) {
                    tagEndWriter.print(tagPushBodyCountVar);
                    tagEndWriter.println("[0]--;");
                }
                else if (pushBodyCountVar != null) {
                    tagEndWriter.print(pushBodyCountVar);
                    tagEndWriter.println("[0]--;");
                }
            }

            tagEndWriter.println("}");
        }

        tagEndWriter.print("if (");
        tagEndWriter.print(tagHandlerVar);
        tagEndWriter.println(".doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {");
        
        if (reuseTag == false || tagClassInfo.implementsJspIdConsumer()) {//jsp2.1work
            // LIDB4147-24
             
        	if (!jspOptions.isDisableResourceInjection()){		//PM06063
            	    tagEndWriter.print ("_jspx_iaHelper.doPreDestroy(");
            	    tagEndWriter.print (tagHandlerVar);
            	    tagEndWriter.println (");");
            	
                    tagEndWriter.print ("_jspx_iaHelper.cleanUpTagHandlerFromCdiMap(");
                    tagEndWriter.print (tagHandlerVar);
                    tagEndWriter.println (");");
        	} 
             
            tagEndWriter.println();
             
            tagEndWriter.println(tagHandlerVar + ".release();");
        }
        
        if (isTagFile || isFragment) {
        	// begin 242714: enhance error reporting for SkipPageException.
        	//tagEndWriter.println("throw new javax.servlet.jsp.SkipPageException();");
            tagEndWriter.println("throw new com.ibm.ws.jsp.runtime.WsSkipPageException(\"Tag file or fragment [" +tagHandlerVar +"] doEndTag returned SKIP_PAGE\");");
        	// end 242714: enhance error reporting for SkipPageException.
        }
        else {
            tagEndWriter.println((methodNesting > 0) ? "return true;" : "return;");
        }
        tagEndWriter.println("}");

        // PM48052 start
        // If doEndTag does a popBody() then we need to make sure we get the
        // correct writer, if we are generated in a method then no need to reset
        // the writer as it a method variable.
        if(!genTagInMethod){
            tagEndWriter.println("out = "+pageContextVar+".getOut();");
        }
        // PM48052 end
        
        syncScriptingVars(tagEndWriter, VariableInfo.AT_BEGIN);

        if (tagClassInfo.implementsTryCatchFinally()) {
            tagEndWriter.println("} catch (Throwable _jspx_exception) {");

            tagEndWriter.print("while (");
            tagEndWriter.print(tagPushBodyCountVar);
            tagEndWriter.println("[0]-- > 0)");
            //PK65013 change pageContext variable to customizable one.
            tagEndWriter.print("out = "+pageContextVar+".popBody();");
            tagEndWriter.println();

            tagEndWriter.print(tagHandlerVar);
            tagEndWriter.println(".doCatch(_jspx_exception);");
            tagEndWriter.println("} finally {");
            tagEndWriter.print(tagHandlerVar);
            tagEndWriter.println(".doFinally();");
        }

        if (tagClassInfo.implementsTryCatchFinally()) {
            tagEndWriter.println("}");
        }

        if (reuseTag == false || tagClassInfo.implementsJspIdConsumer()) {//jsp2.1work
            //           LIDB4147-24
             
        	if (!jspOptions.isDisableResourceInjection()){		//PM06063
            	    tagEndWriter.print ("_jspx_iaHelper.doPreDestroy(");
            	    tagEndWriter.print (tagHandlerVar);
            	    tagEndWriter.println (");");
            	
            	    tagEndWriter.print ("_jspx_iaHelper.cleanUpTagHandlerFromCdiMap(");
            	    tagEndWriter.print (tagHandlerVar);
            	    tagEndWriter.println (");");
        	}
             
            tagEndWriter.println();
             
            tagEndWriter.println(tagHandlerVar + ".release();");
        }
        
        //PK60565 putting back the orig value for pushBodyCountVarDeclaration
        if (tagClassInfo.implementsTryCatchFinally()) {
            persistentData.put("pushBodyCountVarDeclaration", pushBodyCountVarDeclOrig); //PK60565
        } //PK60565 end

        declareScriptingVars(tagEndWriter, VariableInfo.AT_END);
        syncScriptingVars(tagEndWriter, VariableInfo.AT_END);
        restoreScriptingVars(tagEndWriter, VariableInfo.AT_BEGIN);
        return tagEndWriter;
    }

    public void generateInitialization(JavaCodeWriter writer) {
        if (reuseTag && existingTag == false) {
            if (tagClassInfo.implementsJspIdConsumer() == false) {//jsp2.1work
	       if (jspOptions.isUsePageTagPool()) {
	             //247815 Start
                     // LIDB4147-24
                     
                     if (!jspOptions.isDisableResourceInjection()){		//PM06063
                         // have CDI create and inject the managed object
                         initTaglibLookupWriter.print ("com.ibm.ws.managedobject.ManagedObject " + tagHandlerVar + "_mo = ");
                         initTaglibLookupWriter.print ("_jspx_iaHelper.inject(");
                         initTaglibLookupWriter.print (tagClassInfo.getTagClassName() + ".class");
                         initTaglibLookupWriter.println (");");
                     
                         // get the underlying object from the managed object
                         initTaglibLookupWriter.print(tagClassInfo.getTagClassName());
                         initTaglibLookupWriter.print(" ");
                         initTaglibLookupWriter.print(tagHandlerVar);   
                         initTaglibLookupWriter.print(" = ");           
                         initTaglibLookupWriter.println("("+tagClassInfo.getTagClassName()+")"+tagHandlerVar+"_mo.getObject();"); 

                     	initTaglibLookupWriter.print ("_jspx_iaHelper.doPostConstruct(");
                     	initTaglibLookupWriter.print (tagHandlerVar);
                     	initTaglibLookupWriter.println (");");
                     	
                     	initTaglibLookupWriter.print ("_jspx_iaHelper.addTagHandlerToCdiMap(");
                     	initTaglibLookupWriter.print (tagHandlerVar + ", " + tagHandlerVar + "_mo");
                     	initTaglibLookupWriter.println (");");
                     } else {
                         // CDI is not enabled
                         initTaglibLookupWriter.print(tagClassInfo.getTagClassName());
                         initTaglibLookupWriter.print(" ");
                         initTaglibLookupWriter.print(tagHandlerVar);
                         initTaglibLookupWriter.print(" = ");
                         initTaglibLookupWriter.print("new ");
                         initTaglibLookupWriter.print(tagClassInfo.getTagClassName());
                         initTaglibLookupWriter.print("();");

                         initTaglibLookupWriter.println();
                      
                     }
                     
	                initTaglibLookupWriter.println();
	                initTaglibLookupWriter.println("_jspx_TagLookup.put(\"" + tagHandlerVar + "\", " + tagHandlerVar + ");");
	                //247815 End
	            }
	            else if (jspOptions.isUseThreadTagPool()) {
	                String tagKey = ti.getTagClassName() + collectedTagData.getVarNameSuffix();
	                //247815 Start
	                initTaglibLookupWriter.print(tagClassInfo.getTagClassName());
	                initTaglibLookupWriter.print(" ");
	                initTaglibLookupWriter.print(tagHandlerVar);
	                initTaglibLookupWriter.print(" = (" + tagClassInfo.getTagClassName() + ")getTagHandler(request, \"" + tagKey + "\", \"" + ti.getTagClassName() + "\");");
	                initTaglibLookupWriter.println();
	                initTaglibLookupWriter.println("_jspx_TagLookup.put(\"" + tagHandlerVar + "\", " + tagHandlerVar + ");");
	                //247815 End
	            }
            }
            //247815 Start
            writer.print(tagClassInfo.getTagClassName());
            writer.print(" ");
            writer.print(tagHandlerVar);
            writer.println(" = null;");
            //247815 End
            //begin 230150: reuse TryCatchFinally counter object when pooling is enabled.
            if (tagClassInfo.implementsTryCatchFinally()) {  // defect 315723
	            writer.print("int[] ");
	            writer.print(tagPushBodyCountVar);
	            writer.println(" = new int[] { 0 };");
	            persistentData.put("pushBodyCountVarDeclaration", tagPushBodyCountVar); // defect 363508
            }
            //end 230150: reuse TryCatchFinally counter object when pooling is enabled.
            writer.println("int " + tagEvalVar + " = 0;");
        }
    }

    public void generateFinally(JavaCodeWriter writer) {
        if (reuseTag && existingTag == false) {
            if (tagClassInfo.implementsJspIdConsumer() == false) {//jsp2.1work
	            if (jspOptions.isUsePageTagPool()) {
	                //247815 Start
	                cleanupTaglibLookupWriter.print(tagClassInfo.getTagClassName());
	                cleanupTaglibLookupWriter.print(" ");
	                cleanupTaglibLookupWriter.print(tagHandlerVar);
	                cleanupTaglibLookupWriter.println(" = (" + tagClassInfo.getTagClassName() + ")_jspx_TagLookup.get(\"" + tagHandlerVar + "\");");
                     
                     // LIDB4147-24
                     
	                if (!jspOptions.isDisableResourceInjection()){		//PM06063
                     	    cleanupTaglibLookupWriter.println ("_jspx_iaHelper.doPreDestroy(");
                     	    cleanupTaglibLookupWriter.println (tagHandlerVar);
                     	    cleanupTaglibLookupWriter.println (");");
                     	
                     	    cleanupTaglibLookupWriter.print ("_jspx_iaHelper.cleanUpTagHandlerFromCdiMap(");
                     	    cleanupTaglibLookupWriter.print (tagHandlerVar);
                     	    cleanupTaglibLookupWriter.println (");");
	                }
                     
                     cleanupTaglibLookupWriter.println();
                     
                     cleanupTaglibLookupWriter.print(tagHandlerVar);
	                cleanupTaglibLookupWriter.println(".release();");
                     
	                //247815 End
	            }
	            else if (jspOptions.isUseThreadTagPool()) {
	                String tagKey = ti.getTagClassName() + collectedTagData.getVarNameSuffix();
	                //247815 Start
	                cleanupTaglibLookupWriter.print(tagClassInfo.getTagClassName());
	                cleanupTaglibLookupWriter.print(" ");
	                cleanupTaglibLookupWriter.print(tagHandlerVar);
	                cleanupTaglibLookupWriter.println(" = (" + tagClassInfo.getTagClassName() + ")_jspx_TagLookup.get(\"" + tagHandlerVar + "\");");
	                cleanupTaglibLookupWriter.println("putTagHandler(request, \"" + tagKey + "\", " + tagHandlerVar + ");");
	                //247815 End
	            }
            }
        }
    }
}
