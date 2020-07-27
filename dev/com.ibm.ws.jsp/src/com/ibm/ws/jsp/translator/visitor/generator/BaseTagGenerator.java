/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
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
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagVariableInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap; //PI43036
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.taglib.TagClassInfo;
import com.ibm.ws.jsp.taglib.TagFileClassInfo;
import com.ibm.ws.jsp.taglib.TagLibraryCache;
import com.ibm.ws.jsp.translator.JspTranslationException;
import com.ibm.ws.jsp.translator.utils.JspId;
import com.ibm.ws.jsp.translator.utils.JspTranslatorUtil;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.validator.ValidateResult;
import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.jsp.resource.translation.JspResources;

public abstract class BaseTagGenerator implements TagGenerator {
    protected int nestingLevel = 0;
    protected boolean isTagFile = false;
    protected boolean hasBody = false;
    protected boolean hasJspBody = false;
    protected boolean isFragment = false;
    protected boolean isRepeatTag = false;	// PK26741
    
    protected Element element = null;
    protected TagLibraryCache tagLibraryCache = null;
    protected TagClassInfo tagClassInfo = null;
    protected String tagHandlerVar = null;
    protected TagInfo ti = null;
    protected JspConfiguration jspConfiguration = null;
    protected JspCoreContext ctxt = null;
    protected FragmentHelperClassWriter fragmentHelperClassWriter = null;
    protected Map attributeWriterMap = new HashMap();
    protected ValidateResult.CollectedTagData collectedTagData = null;
    protected Map persistentData = null;
    protected JspOptions jspOptions = null;
    protected MethodWriter bodyWriter = new MethodWriter();
    protected CustomTagGenerator.TagInstanceInfo parentTagInstanceInfo = null;
    protected boolean isConvertExpression = false; //PK53703
    protected String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG; //PK65013

	static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.translator.visitor.generator.BaseTagGenerator";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}

    protected BaseTagGenerator(int nestingLevel,
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
                               JspOptions jspOptions) {
        this.nestingLevel = nestingLevel;                                   
        this.isTagFile = isTagFile;
        this.hasBody = hasBody;
        this.hasJspBody = hasJspBody;
        this.tagHandlerVar = tagHandlerVar;
        if (tagHandlerVar.indexOf("_tsx_repeat") != -1)		// PK26741
           	isRepeatTag = true;							// PK26741
        this.element = element; 
        this.tagLibraryCache = tagLibraryCache; 
        this.jspConfiguration = jspConfiguration; 
        this.ctxt = ctxt;
        this.tagClassInfo = tagClassInfo;  
        this.ti = ti;
        this.persistentData = persistentData;
        this.collectedTagData = collectedTagData;
        this.fragmentHelperClassWriter = fragmentHelperClassWriter;
        this.jspOptions = jspOptions; 
        //PK65013 - start
        if (isTagFile && jspOptions.isModifyPageContextVariable()) {
            this.pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
        }
        //PK65013 - end
    }
    
    public abstract MethodWriter generateTagStart() throws JspCoreException;
    public abstract MethodWriter generateTagMiddle() throws JspCoreException;
    public abstract MethodWriter generateTagEnd() throws JspCoreException;
    public abstract void generateInitialization(JavaCodeWriter writer);
    public abstract void generateFinally(JavaCodeWriter writer);
    public void generateImports(JavaCodeWriter writer){}
    public void generateDeclarations(JavaCodeWriter writer) {}
    
    public void setParentTagInstanceInfo(CustomTagGenerator.TagInstanceInfo parentTagInstanceInfo) {
        this.parentTagInstanceInfo = parentTagInstanceInfo;
    }

    public void setIsInFragment(boolean isFragment) {
        this.isFragment = isFragment;
    }
    
    public boolean fragmentWriterUsed() {
        return false;
    }

    public MethodWriter getBodyWriter() {
        return bodyWriter;
    }
    
    protected String createJspId(JspVisitorInputMap inputMap) throws JspCoreException {       
        String jspIdPrefix = ((String)inputMap.get("JspIdConsumerPrefix"));
        if (jspIdPrefix == null) {
        	JspResources jspFiles = (JspResources)inputMap.get("JspFiles");
            //PM43852 start - change to use package name and class name instead of path to generated source.
            String name = jspFiles.getPackageName() + "." + jspFiles.getClassName(); 
            //PM43852 end            
            StringBuffer sb = new StringBuffer(32);
            sb.append("jsp_").append(Math.abs(name.hashCode())).append('_');
            jspIdPrefix = sb.toString();            
            inputMap.put("JspIdConsumerPrefix",jspIdPrefix);
        }
        Integer jspIdValue = ((Integer)inputMap.get("JspIdConsumerCounter"));
    	jspIdValue+=1;
        inputMap.put("JspIdConsumerCounter",jspIdValue);
        return jspIdPrefix + (jspIdValue.toString());
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
                    writerForChild = bodyWriter;
                }
            }
            else {
                writerForChild = bodyWriter;
            }
        }
        return (writerForChild);
    }
    
    public void generateSetParent(MethodWriter writer) throws JspCoreException {
        if (tagClassInfo.implementsSimpleTag()) {
            String aliasMapVar = null;
            //230956
            if (tagClassInfo instanceof TagFileClassInfo) {
                aliasMapVar = generateAliasMap(writer, tagHandlerVar);
            }
            writer.print(tagHandlerVar);
            if (aliasMapVar == null) {
                writer.println(".setJspContext("+pageContextVar+");");
            }
            else {
                writer.print(".setJspContext("+pageContextVar+", ");
                writer.print(aliasMapVar);
                writer.println(");");
            }
            // The setParent() method need not be called if the value being
            // passed is null, since SimpleTag instances are not reused
            if (parentTagInstanceInfo != null) {
                writer.print(tagHandlerVar);
                writer.print(".setParent(");
                writer.print(parentTagInstanceInfo.getTagHandlerVar());
                writer.println(");");
            }
            // - We are casting to SimpleTag because the generated java source code for a tag file extends SimpleTagSupport.
            // - The reason why tagClassInfo.implementsSimpleTag() is false (see above if statement) and isTagFile is true
            // is because we are currently processing a tag that is contained within a .tag file.
            // - The allowNullParentInTagFile is for safety...in case a tag needs to have setParent(null)..we can't think of why though.
            // - The allowNullParentInTagFile is not documented and we should leave it that way unless we run in to a problem..
            // PM24787 start
            else if (isTagFile) { //need to make sure we only set the parent if one tag invocation is nested within another or if we're within a tag file and need to support the top level
                if (!(jspOptions.isAllowNullParentInTagFile())) {
                	writer.print(tagHandlerVar);
                	writer.print(".setParent(new javax.servlet.jsp.tagext.TagAdapter(");
                	writer.print("(");
                	writer.print("javax.servlet.jsp.tagext.SimpleTag");
                	writer.print(") this ));");
                } else {
                    writer.print(tagHandlerVar);
                    writer.print(".setParent(");
                    writer.print("null");
                    writer.println(");");
                }
            }
            // PM24787 end
        }
        else {
            writer.print(tagHandlerVar);
            writer.println(".setPageContext("+pageContextVar+");");
            if (parentTagInstanceInfo != null) {
                TagClassInfo parentTagClassInfo = tagLibraryCache.getTagClassInfo(parentTagInstanceInfo.getTi());

                if (parentTagClassInfo.implementsSimpleTag()) {
                    writer.print(tagHandlerVar);
                    writer.print(".setParent(");
                    writer.print("new javax.servlet.jsp.tagext.TagAdapter(");
                    writer.print("(");
                    writer.print("javax.servlet.jsp.tagext.SimpleTag");
                    writer.print(") ");
                    writer.print(parentTagInstanceInfo.getTagHandlerVar());
                    writer.println("));");
                }
                else {
                    writer.print(tagHandlerVar);
                    writer.print(".setParent((javax.servlet.jsp.tagext.Tag) ");
                    writer.print(parentTagInstanceInfo.getTagHandlerVar());
                    writer.println(");");
                }
            }
            // PK62809 start
            // - We are casting to SimpleTag because the generated java source code for a tag file extends SimpleTagSupport.
            // - The reason why tagClassInfo.implementsSimpleTag() is false (see above if statement) and isTagFile is true
            // is because we are currently processing a tag that is contained within a .tag file.
            // - The allowNullParentInTagFile is for safety...in case a tag needs to have setParent(null)..we can't think of why though.
            // - The allowNullParentInTagFile is not documented and we should leave it that way unless we run in to a problem..
            else if (isTagFile && !(jspOptions.isAllowNullParentInTagFile())) {
                writer.print(tagHandlerVar);
                writer.print(".setParent(new javax.servlet.jsp.tagext.TagAdapter(");
                writer.print("(");
                writer.print("javax.servlet.jsp.tagext.SimpleTag");
                writer.print(") this ));");
            }
            //PK62809 end
            else {
                writer.print(tagHandlerVar);
                writer.print(".setParent(");
                writer.print("null");
                writer.println(");");
            }
        }
    }

    public List generateSetters() throws JspCoreException {
        List setterWriterList = new ArrayList();
        
        JspId jspId = new JspId(element.getAttributeNS(Constants.JSP_NAMESPACE, "id"));
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
			logger.logp(Level.FINEST, CLASS_NAME, "generateSetters","jspId = ["+jspId+"]");
		}
		NamedNodeMap attributesInElement = element.getAttributes(); //PI43036
        for (Iterator itr = jspId.getAttrNameList().iterator(); itr.hasNext();) {
            Attr attr = null;
            String attrName = (String)itr.next();
            boolean attrIsQualifiedName = false; //PI43036
            if (attrName.indexOf(':') != -1) {
                //PI43036 start
                /* This is because in jsp:id we passed the uri:prefix:attrname (if it was a JSP in Document form)
                * Modify the attrName to ignore the uri, we want to have prefix:attrname.
                * If attributesInElement.getNamedItem(attrName) is null is because we have  uri:prefix:attrname
                */
                if (attributesInElement.getNamedItem(attrName) == null)
                    attrName = attrName.substring(attrName.indexOf(':') + 1);
                
                String namespaceUri = element.lookupNamespaceURI(attrName.substring(0, attrName.indexOf(':')));
                if (namespaceUri == null) {
                    //If a dynamic attribute has a prefix that does not map to a namespace, a translation error must occur.
                    throw new JspTranslationException("jsp.error.dynamicAttributes.translationException", new Object[] { attrName });
                }
                //Needed to modify this to work with JSP standard pages (it never worked), it works in the same way with JSP Documents
                attr = element.getAttributeNodeNS(namespaceUri, attrName.substring(attrName.indexOf(':') + 1));
                attrIsQualifiedName = true;
                //PI43036 end
            }
            else {
                attr = element.getAttributeNode(attrName);    
            }
            if (attr==null) {
                //attribute did not exist in namespace ... must break according to the spec
                break;
            }
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
    			logger.logp(Level.FINEST, CLASS_NAME, "generateSetters","attrName = ["+attrName+"]");
                logger.logp(Level.FINEST, CLASS_NAME, "generateSetters","attr.getName() = ["+attr.getName()+"]");
    		}
            if (attr.getName().equals("jsp:id") == false && attr.getName().startsWith("xmlns") == false) {
                MethodWriter setterWriter = new MethodWriter();
                TagAttributeInfo tai = findTagAttributeInfo(attr.getName());
                boolean isDynamic = false;
                if (tai == null && ti.hasDynamicAttributes() == true)
                    isDynamic = true;
        		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
        			logger.logp(Level.FINEST, CLASS_NAME, "generateSetters","tai = ["+tai+"] isDynamic = ["+isDynamic+"]");
        		}
                //PI30519 start
                String evalAttrValue = null;
                if (jspOptions.isAllowMultipleAttributeValues()) {
                    String tmpAttrVal = null;
                    List valueList = (ArrayList) element.getUserData(attr.getName());
                    
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST))
                        logger.logp(Level.FINEST, CLASS_NAME, "generateSetters", "allowMultipleAttributeValues is enabled valueList = " + valueList.toString());
                    
                    if (valueList != null && valueList.size() > 0) {
                        tmpAttrVal = (String) valueList.get(0);
                        valueList.remove(0);
                        element.setUserData(attr.getName(), valueList, null);
                        valueList = null;
                    } else {
                        tmpAttrVal = attr.getValue();
                    }
                    evalAttrValue = evaluateAttribute(attr.getName(), tmpAttrVal, isDynamic, false, false, tai);
                } else {
                    evalAttrValue = evaluateAttribute(attr.getName(), attr.getValue(), isDynamic, false, false, tai);
                }
                //PI30519 end
        		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
        			logger.logp(Level.FINEST, CLASS_NAME, "generateSetters","evalAttrValue = ["+evalAttrValue+"]");
        		}
                //PI43036 start
                if (attrIsQualifiedName && isDynamic) {
                    String attrQualifiedName = attr.getName();
                    String attrLocalName = attrQualifiedName.substring(attrQualifiedName.indexOf(":") + 1);
                    //setDynamicAttribute should use the localName, not the qualified name of the attribute
                    generateSetterCall(attrLocalName, evalAttrValue, attr.getNamespaceURI(), setterWriter, isDynamic);
                } else
                    generateSetterCall(attr.getName(), evalAttrValue, attr.getNamespaceURI(), setterWriter, isDynamic);
                //PI43036 end
                setterWriterList.add(setterWriter);
            }
        }
        
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node childNode = nl.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element)childNode;
                if (childElement.getNamespaceURI() != null && 
                    childElement.getNamespaceURI().equals(Constants.JSP_NAMESPACE) && 
                    childElement.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                    String name = childElement.getAttribute("name");
                    if (name.indexOf(':') != -1) {
                        name = name.substring(name.indexOf(':') + 1);
                    }
                                            
                    TagAttributeInfo tai = findTagAttributeInfo(name);
                    boolean isAttrFragment = false;
                    if (tai != null)
                        isAttrFragment = tai.isFragment();    
            		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
            			logger.logp(Level.FINEST, CLASS_NAME, "generateSetters","childElement.getLocalName() = ["+childElement.getLocalName()+"]");
            			logger.logp(Level.FINEST, CLASS_NAME, "generateSetters","name = ["+name+"]");
            			logger.logp(Level.FINEST, CLASS_NAME, "generateSetters","tai = ["+tai+"]");
            			logger.logp(Level.FINEST, CLASS_NAME, "generateSetters","isAttrFragment = ["+isAttrFragment+"]");
            		}
                    NamedAttributeWriter attributeWriter = createJspAttributeWriter(childElement, isAttrFragment);
                    setterWriterList.add(attributeWriter);
                }
            }
        }
        return setterWriterList;
    }


    public void generateJspAttributeSetters() throws JspCoreException {
        HashMap jspAttributes = (HashMap)persistentData.get("jspAttributes");
        if (jspAttributes != null) {
            ArrayList jspAttributeList = (ArrayList)jspAttributes.get(element);
            if (jspAttributeList != null) {
                for (Iterator itr = jspAttributeList.iterator(); itr.hasNext();) {
                    AttributeGenerator.JspAttribute jspAttribute = (AttributeGenerator.JspAttribute)itr.next();
                    MethodWriter attributeWriter = findAttributeWriter(jspAttribute.getName());
                    TagAttributeInfo tai = findTagAttributeInfo(jspAttribute.getName());
                    boolean isAttrFragment = false;
                    boolean isDynamic = false;
                    if (tai != null)
                        isAttrFragment = tai.isFragment();    
                    if (tai == null && ti.hasDynamicAttributes() == true)
                        isDynamic = true;
                    if (isAttrFragment == false) {                        
                        String evalAttrValue = evaluateAttribute(jspAttribute.getName(), jspAttribute.getVarName(), isDynamic, isAttrFragment, true, tai);
                        generateSetterCall(jspAttribute.getName(), evalAttrValue, jspAttribute.getJspAttrElement().getNamespaceURI(), attributeWriter, isDynamic);
                    }
                }
            }
            
        }
    }
    
    private String generateAliasMap(JavaCodeWriter writer, String tagHandlerVar)
        throws JspCoreException {
        TagVariableInfo[] tagVars = ti.getTagVariableInfos();
        String aliasMapVar = null;
        
        boolean aliasSeen = false;
        for (int i = 0; i < tagVars.length; i++) {
        
            String nameFrom = tagVars[i].getNameFromAttribute();
            if (nameFrom != null) {
                String aliasName = element.getAttribute(nameFrom);
                if (aliasName.equals(""))
                    continue;
        
                if (!aliasSeen) {
                    writer.print("java.util.HashMap ");
                    aliasMapVar = tagHandlerVar + "_aliasMap";
                    writer.print(aliasMapVar);
                    writer.print(" = new java.util.HashMap();");
                    writer.println();
                    aliasSeen = true;
                }
                writer.print(aliasMapVar);
                writer.print(".put(");
                writer.print(GeneratorUtils.quote(tagVars[i].getNameGiven()));
                writer.print(", ");
                writer.print(GeneratorUtils.quote(aliasName));
                writer.print(");");
                writer.println();
            }
        }
        return aliasMapVar;
    }
    
    protected TagAttributeInfo findTagAttributeInfo(String attrName) {
        TagAttributeInfo foundAttr = null;
        TagAttributeInfo[] attributes = ti.getAttributes();
        
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i].getName().equals(attrName)) {
                foundAttr = attributes[i];
                break;        
            }
        } 
        
        return (foundAttr);
    }
    
    private String evaluateAttribute(String attrName, String attrValue, boolean isDynamic, boolean isAttrFragment, boolean isNamedAttribute,TagAttributeInfo tai) 
        throws JspCoreException {
        String evalAttrValue = attrValue;
        String parameterClassName = "Object";
        String fullParameterClassName = "java.lang.Object";
        if (isDynamic == false) {
            fullParameterClassName = tagClassInfo.getParameterClassName(attrName, ctxt); //PK36246 need to pass the context for the correct classloader
            parameterClassName = fullParameterClassName;
            if (parameterClassName.startsWith("java.lang.")) {
                parameterClassName = parameterClassName.substring(parameterClassName.indexOf("java.lang.")+10);    
            }
        }

        attrValue = attrValue.replaceAll("&gt;", ">");
        attrValue = attrValue.replaceAll("&lt;", "<");
        attrValue = attrValue.replaceAll("&amp;", "&");
        attrValue = attrValue.replaceAll("<\\%", "<%");
        attrValue = attrValue.replaceAll("%\\>", "%>");
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "evaluateAttribute","attrName = ["+attrName+"]");
			logger.logp(Level.FINER, CLASS_NAME, "evaluateAttribute","attrValue = ["+attrValue+"]");
			logger.logp(Level.FINER, CLASS_NAME, "evaluateAttribute","isDynamic = ["+isDynamic+"]");
			logger.logp(Level.FINER, CLASS_NAME, "evaluateAttribute","isAttrFragment = ["+isAttrFragment+"]");
			logger.logp(Level.FINER, CLASS_NAME, "evaluateAttribute","isNamedAttribute = ["+isNamedAttribute+"]");
            logger.logp(Level.FINER, CLASS_NAME, "evaluateAttribute","parameterClassName = ["+parameterClassName+"]");
		}
        boolean isELInput=JspTranslatorUtil.isELInterpreterInput(attrValue, jspConfiguration);
        isConvertExpression = false;  //PK53703
        if (JspTranslatorUtil.isExpression(attrValue)) {
            //PK53703 - start
            // this next line is the orignal code
            //      evalAttrValue = attrValue.substring(2, attrValue.length()-1);
            //This change is not spec compliant...we need to make this change to support
            //existing customer apps that expect the expression to be converted to a particular type
            //but the JSP spec says that we do not do this for expressions.  see section JSP.1.14.2.2
            
            if(jspOptions != null && jspOptions.isConvertExpression()) {
                isConvertExpression = true;
                //evalAttrValue = convertString(parameterClassName, attrValue, attrName, tagClassInfo.getPropertyEditorClassName(attrName), isNamedAttribute);              
                evalAttrValue = convertString(parameterClassName, attrValue, attrName, tagClassInfo.getPropertyEditorClassName(attrName), true);
            } else {
                evalAttrValue = attrValue.substring(2, attrValue.length()-1);
            }
            //PK53703 - end
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
    			logger.logp(Level.FINER, CLASS_NAME, "evaluateAttribute","isExpression, evalAttrValue = ["+evalAttrValue+"]");
    		}
        }
        else if (isNamedAttribute == true) {
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
    			logger.logp(Level.FINER, CLASS_NAME, "evaluateAttribute","isNamedAttribute = ["+isNamedAttribute+"]");
    		}
            if (isAttrFragment == false && isDynamic == false) {
                evalAttrValue = convertString(parameterClassName, attrValue, attrName, tagClassInfo.getPropertyEditorClassName(attrName), true);
        		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
        			logger.logp(Level.FINER, CLASS_NAME, "evaluateAttribute","converted evalAttrValue = ["+evalAttrValue+"]");
        		}
            }
        }
        else if (isELInput || GeneratorUtils.isDeferredInput(tai) || GeneratorUtils.isDeferredMethodInput(tai)) {
            try {            	
                String type;
                if (tai!=null) {
                    type = tai.getTypeName();
                } else {
                    //default type for dynamic attribute 
                    //(need to figure out how to tell if this is a deferredValue or deferredMethod
                    // since we compare this to ValueExpression and MethodExpression below)
                    type = "java.lang.Object";
                }
                Class c = JspTranslatorUtil.toClass(fullParameterClassName, ctxt.getJspClassloaderContext().getClassLoader());
                
                // results buffer
                StringBuffer sb = new StringBuffer(64);

                // generate elContext reference
                String jspCtxt = null;
                if (isTagFile)
                    jspCtxt = "getJspContext()";
                else
                    jspCtxt = "pageContext";
                sb.append(jspCtxt);
                sb.append(".getELContext()");
                String elContext = sb.toString();
                if (true/*isELInput*/) {
                    sb.setLength(0);
                    sb.append("new org.apache.jasper.el.ELContextWrapper(");
                    sb.append(elContext);
                    sb.append(',');
                    sb.append("_jspx_fnmap");
                    sb.append(')');
                    elContext = sb.toString();
                }

                // reset buffer
                sb.setLength(0);
                
                
                // create mark
                String jspIdString = element.getAttributeNS(Constants.JSP_NAMESPACE, "id");
                String msg="";
                if (jspIdString.equals("") == false) { 
                	JspId jspId = new JspId(jspIdString);
                	msg = jspId.getFilePath() + "(" + jspId.getStartSourceLineNum() + "," + jspId.getStartSourceColNum() + ")";                	
                }                
                sb.append(msg);
                sb.append(" '");
                sb.append(attrValue);
                sb.append('\'');                
                String mark = sb.toString();
                // reset buffer
                sb.setLength(0);
                
        		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
        			logger.logp(Level.FINER, CLASS_NAME, "evaluateAttribute","isELInterperterInput Class= ["+c+"]  type= ["+type+"]");
        		}
                if (GeneratorUtils.isDeferredInput(tai)
                        || ValueExpression.class.getName().equals(type)) {
                	evalAttrValue = GeneratorUtils.createValueExpression(sb, mark, elContext, attrValue, isELInput,  c, tai, jspCtxt);
                	
	            } else if (GeneratorUtils.isDeferredMethodInput(tai)
	                    || MethodExpression.class.getName().equals(type)) {
	                //Can't use isELInput because when JSP version is <2.1, this doesn't check deferred values
                    if (!JspTranslatorUtil.isELInterpreterInput(attrValue, jspConfiguration, true) && "void".equals(GeneratorUtils.getExpectedTypeName(tai))) {
                	    throw new JspTranslationException("jsp.error.translation.invalid.void.deferredMethodClass", new Object[] {attrName});
                    }
                    evalAttrValue = GeneratorUtils.createMethodExpression(sb, mark, elContext, attrValue, isELInput, c, tai, jspCtxt);
	            	
	            } else {
                    evalAttrValue = GeneratorUtils.interpreterCall(this.isTagFile, attrValue, c, "_jspx_fnmap", false, pageContextVar);//PK65013
	            }
        		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
        			logger.logp(Level.FINER, CLASS_NAME, "evaluateAttribute","isELInterperterInput evalAttrValue= ["+evalAttrValue+"]");
        		}
            }
            catch(ClassNotFoundException e) {
                throw new JspTranslationException(element,"jsp.error.loadclass.taghandler.attr", new Object[] {attrName}, e);
            }
        }
        else {
            //PK31208
            if(isRepeatTag == true && (attrName.equals("end") || attrName.equals("start")) && !jspOptions.isConvertAttrValueToString()) {
                return evalAttrValue;       //PK31208
            }
            else {
            	evalAttrValue = convertString(parameterClassName, attrValue, attrName, tagClassInfo.getPropertyEditorClassName(attrName), false);
        		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
        			logger.logp(Level.FINER, CLASS_NAME, "evaluateAttribute","converted string evalAttrValue= ["+evalAttrValue+"]");
        		}
            }
        }
        return (evalAttrValue);
    }
    
    protected void generateSetterCall(String attrName,
                                      String evalAttrValue,
                                      String uri,
                                      MethodWriter setterWriter,
                                      boolean isDymanic) throws JspCoreException {
        if (isDymanic) {
            setterWriter.print(tagHandlerVar);
            setterWriter.print(".");
            setterWriter.print("setDynamicAttribute(");
            if ("".equals(uri) || (uri == null)) {
                setterWriter.print("null");
            }
            else {
                setterWriter.print("\"" + uri + "\"");
            }
            setterWriter.print(", \"");
            setterWriter.print(attrName);
            setterWriter.print("\", ");
            setterWriter.print(evalAttrValue);
            setterWriter.println(");");
        }
        else {
            setterWriter.print(tagHandlerVar);
            setterWriter.print(".");
            setterWriter.print(tagClassInfo.getSetterMethodName(attrName));
            setterWriter.print("(");
            setterWriter.print(evalAttrValue);
            setterWriter.println(");");
        }
    }
    
    private String convertString(String parameterClassName,
                                 String s,
                                 String attrName,
                                 String propEditorClassName,
                                 boolean isNamedAttribute) {

        String quoted = s;

        //PK53703 - Jay Sartoris
        //checking to make sure this is an expression (e.g. <%=val%>
        //if so, then we do not want to wrap quotes around it because
        //if the parameterClassName is Object then it will end up looking like
        //this for the value we return:
        //      new String("val")       
        //We want it to look like this:
        //      new String(val)
        //We have to wrap it around a flag though so we don't break existing functionality

        // PK53703 - start        
        //boolean isExpression = JspTranslatorUtil.isExpression(s);
        
        if(logger.isLoggable(Level.FINER)){
            logger.logp(Level.FINER, CLASS_NAME, "convertString","attrValue: " + s);
        }

        if (isConvertExpression) {
            //if(logger.isLoggable(Level.FINER)){
            //  logger.logp(Level.FINER, CLASS_NAME, "convertString","JspTranslatorUtil.isExpression(s): " + JspTranslatorUtil.isExpression(s));
            //}
            
            
            //if (!isExpression && !isNamedAttribute) {
            //  quoted = GeneratorUtils.quote(s);
            //} else {              
                quoted = quoted.substring(2, quoted.length()-1);            
            //}
            if(logger.isLoggable(Level.FINER)){
                logger.logp(Level.FINER, CLASS_NAME, "convertString","quoted: " + quoted);
            }
            
        }
        // PK53703 - end

        if (!isNamedAttribute) {
            quoted = GeneratorUtils.quote(s, jspOptions.isEnableDoubleQuotesDecoding());                   //PM21395
        }

        // PK53703 - start
        if (isConvertExpression && !parameterClassName.equals("String")) {
            s = quoted;
            if(logger.isLoggable(Level.FINER)){
                logger.logp(Level.FINER, CLASS_NAME, "convertString","s: " + s);
            }
        }
        // PK53703 - end        

        if (propEditorClassName != null) {
            return "("
                + parameterClassName
                + ")org.apache.jasper.runtime.JspRuntimeLibrary.getValueFromBeanInfoPropertyEditor("
                + parameterClassName
                + ".class, \""
                + attrName
                + "\", "
                + quoted
                + ", "
                + propEditorClassName
                + ".class)";
        }
        else if (parameterClassName.equals("String")) {
            // PK53703 - start
            if (isConvertExpression) {
                //return "new String(" + quoted.toString() + ")";   
                return quoted + ".toString()";
            } else {
            // PK53703 - end    
                return quoted;
            }
        }
        else if (parameterClassName.equals("boolean")) {
            return GeneratorUtils.coerceToPrimitiveBoolean(s, isNamedAttribute);
        }
        else if (parameterClassName.equals("Boolean")) {
            return GeneratorUtils.coerceToBoolean(s, isNamedAttribute);
        }
        else if (parameterClassName.equals("byte")) {
            return GeneratorUtils.coerceToPrimitiveByte(s, isNamedAttribute);
        }
        else if (parameterClassName.equals("Byte")) {
            return GeneratorUtils.coerceToByte(s, isNamedAttribute);
        }
        else if (parameterClassName.equals("char")) {
            return GeneratorUtils.coerceToChar(s, isNamedAttribute);
        }
        else if (parameterClassName.equals("Character")) {
            return GeneratorUtils.coerceToCharacter(s, isNamedAttribute);
        }
        else if (parameterClassName.equals("double")) {
            return GeneratorUtils.coerceToPrimitiveDouble(s, isNamedAttribute);
        }
        else if (parameterClassName.equals("Double")) {
            return GeneratorUtils.coerceToDouble(s, isNamedAttribute);
        }
        else if (parameterClassName.equals("float")) {
            return GeneratorUtils.coerceToPrimitiveFloat(s, isNamedAttribute);
        }
        else if (parameterClassName.equals("Float")) {
            return GeneratorUtils.coerceToFloat(s, isNamedAttribute);
        }
        else if (parameterClassName.equals("int")) {
            return GeneratorUtils.coerceToInt(s, isNamedAttribute);
        }
        else if (parameterClassName.equals("Integer")) {
            return GeneratorUtils.coerceToInteger(s, isNamedAttribute);
        }
        else if (parameterClassName.equals("short")) {
            return GeneratorUtils.coerceToPrimitiveShort(s, isNamedAttribute);
        }
        else if (parameterClassName.equals("Short")) {
            return GeneratorUtils.coerceToShort(s, isNamedAttribute);
        }
        else if (parameterClassName.equals("long")) {
            return GeneratorUtils.coerceToPrimitiveLong(s, isNamedAttribute);
        }
        else if (parameterClassName.equals("Long")) {
            return GeneratorUtils.coerceToLong(s, isNamedAttribute);
        }
        else if (parameterClassName.equals("Object")) {
            //PI05359 performance improvement
            return quoted;
        }
        else {
            //PK53703 - start
            if (isConvertExpression) {
                return quoted;
            }               
            else {
            // PK53703 - end    
                return "("
                    + parameterClassName
                    + ")org.apache.jasper.runtime.JspRuntimeLibrary.getValueFromPropertyEditorManager("
                    + parameterClassName
                    + ".class, \""
                    + attrName
                    + "\", "
                    + quoted
                    + ")";
            }
        }
    }
    
    private NamedAttributeWriter createJspAttributeWriter(Element attributeElement, 
                                                          boolean isAttrFragment) 
        throws JspCoreException {
        int methodNesting =  ((Integer)persistentData.get("methodNesting")).intValue();
        String name = attributeElement.getAttribute("name");
            
        if (name.indexOf(':') != -1) {
            name = name.substring(name.indexOf(':') + 1);
        }
            
        NamedAttributeWriter attributeWriter = new NamedAttributeWriter(name, GeneratorUtils.nextTemporaryVariableName(persistentData));

        if (isAttrFragment) {
            attributeWriter.print("javax.servlet.jsp.tagext.JspFragment " + attributeWriter.getVarName() + " = ");
            FragmentHelperClassWriter.FragmentWriter fragment = fragmentHelperClassWriter.openFragment(element, tagHandlerVar, methodNesting, pageContextVar); //PK65013
            attributeWriter.print("new " + fragmentHelperClassWriter.getClassName() + "( ");
            if (jspOptions.isUsePageTagPool() ||
                jspOptions.isUseThreadTagPool()) {
                attributeWriter.print("_jspx_TagLookup, "); 
            }
            String pushBodyCountVar = (String)persistentData.get("pushBodyCountVar");
            //PK65013
            attributeWriter.print(fragment.getId()
                                + ", "+pageContextVar+", "
                                + tagHandlerVar 
                                + ", " 
                                + pushBodyCountVar
                                + ")");
            attributeWriter.println(";");
            attributeWriterMap.put(attributeElement, fragment);
            generateSetterCall(name, attributeWriter.getVarName(), attributeElement.getNamespaceURI(), attributeWriter, false);
        }
        else {
            attributeWriterMap.put(attributeElement, attributeWriter);
        }
        return (attributeWriter);
    }
    
    protected void declareScriptingVars(JavaCodeWriter tagWriter, int scope) {

        Vector vec = getScriptingVars(scope);
        
        if (vec != null) {
            for (int i = 0; i < vec.size(); i++) {
                Object elem = vec.elementAt(i);
                if (elem instanceof VariableInfo) {
                    VariableInfo varInfo = (VariableInfo) elem;
                    if (varInfo.getDeclare()) {
						// PK26741
						if(jspOptions.isUseRepeatInt() && isRepeatTag) {
							tagWriter.print("int");
						    tagWriter.print(" ");
						    tagWriter.print(varInfo.getVarName());
						    tagWriter.println(" = 0;");
						} // PK26741
						else {
                            String className=varInfo.getClassName();
                            className = replaceCharacters(className); //PK40417 &gt;,&lt;,&amp;,<\\%, and %\\> 
                            
                            tagWriter.print(className);
	                        tagWriter.print(" ");
	                        tagWriter.print(varInfo.getVarName());
	                        tagWriter.println(" = null;");
						}
                    }
                }
                else {
                    TagVariableInfo tagVarInfo = (TagVariableInfo) elem;
                    if (tagVarInfo.getDeclare()) {
                        String varName = tagVarInfo.getNameGiven();
                        if (varName == null) {
                            varName = collectedTagData.getTagData().getAttributeString(tagVarInfo.getNameFromAttribute());
                        }
                        else if (tagVarInfo.getNameFromAttribute() != null) {
                            // alias
                            continue;
                        }
                        // PK26741
						if(jspOptions.isUseRepeatInt() && isRepeatTag) {
							tagWriter.print("int");  //change tagVarInfo.getClassname to 0
						    tagWriter.print(" ");
						    tagWriter.print(varName);
						    tagWriter.println(" = 0;");  //change null to 0
						}	// PK26741
						else {
                            String className = tagVarInfo.getClassName();
                            className = replaceCharacters(className); //PK40417 &gt;, &lt;, &amp; <\\%, %\\>
                            tagWriter.print(className);
	                        tagWriter.print(" ");
	                        tagWriter.print(varName);
	                        tagWriter.println(" = null;");
						}
                    }
                }
            }
        }
    }

    /*
     * This method is called as part of the custom tag's start element.
     *
     * If the given custom tag has a custom nesting level greater than 0,
     * save the current values of its scripting variables to 
     * temporary variables, so those values may be restored in the tag's
     * end element. This way, the scripting variables may be synchronized
     * by the given tag without affecting their original values.
     */
    protected void saveScriptingVars(JavaCodeWriter tagWriter, int scope) {
        if (nestingLevel == 0) {
            return;
        }

        TagVariableInfo[] tagVarInfos = ti.getTagVariableInfos();
        VariableInfo[] varInfos = ti.getVariableInfo(collectedTagData.getTagData());
        
        if (varInfos == null)
            varInfos = new VariableInfo[0];
            
        if ((varInfos.length == 0) && (tagVarInfos.length == 0)) {
            return;
        }

        if (varInfos.length > 0) {
            for (int i = 0; i < varInfos.length; i++) {
                if (varInfos[i].getScope() != scope)
                    continue;
                if (containsVariableInfo(getScriptingVars(scope), varInfos[i])) {
                    continue;
                }
                String varName = varInfos[i].getVarName();
                String tmpVarName = "_jspx_" + varName + "_" + nestingLevel;
                tagWriter.print(tmpVarName);
                tagWriter.print(" = ");
                tagWriter.print(varName);
                tagWriter.println(";");
            }
        }
        else {
            for (int i = 0; i < tagVarInfos.length; i++) {
                if (tagVarInfos[i].getScope() != scope)
                    continue;
                if (containsTagVariableInfo(getScriptingVars(scope),tagVarInfos[i])) {
                    continue;
                }
                String varName = tagVarInfos[i].getNameGiven();
                if (varName == null) {
                    varName = collectedTagData.getTagData().getAttributeString(tagVarInfos[i].getNameFromAttribute());
                }
                else if (tagVarInfos[i].getNameFromAttribute() != null) {
                    // alias
                    continue;
                }
                String tmpVarName = "_jspx_" + varName + "_" + nestingLevel;
                tagWriter.print(tmpVarName);
                tagWriter.print(" = ");
                tagWriter.print(varName);
                tagWriter.println(";");
            }
        }
    }

    /*
     * This method is called as part of the custom tag's end element.
     *
     * If the given custom tag has a custom nesting level greater than 0,
     * restore its scripting variables to their original values that were
     * saved in the tag's start element.
     */
    protected void restoreScriptingVars(JavaCodeWriter tagWriter, int scope) {
        if (nestingLevel == 0) {
            return;
        }

        TagVariableInfo[] tagVarInfos = ti.getTagVariableInfos();
        VariableInfo[] varInfos = ti.getVariableInfo(collectedTagData.getTagData());
        
        if (varInfos == null)
            varInfos = new VariableInfo[0];
            
        if ((varInfos.length == 0) && (tagVarInfos.length == 0)) {
            return;
        }

        if (varInfos.length > 0) {
            for (int i = 0; i < varInfos.length; i++) {
                if (varInfos[i].getScope() != scope)
                    continue;
                if (containsVariableInfo(getScriptingVars(scope), varInfos[i])) {
                    continue;
                }
                String varName = varInfos[i].getVarName();
                String tmpVarName = "_jspx_" + varName + "_" + nestingLevel;
                tagWriter.print(varName);
                tagWriter.print(" = ");
                tagWriter.print(tmpVarName);
                tagWriter.println(";");
            }
        }
        else {
            for (int i = 0; i < tagVarInfos.length; i++) {
                if (tagVarInfos[i].getScope() != scope)
                    continue;
                if (containsTagVariableInfo(getScriptingVars(scope),tagVarInfos[i])) {
                    continue;
                }
                String varName = tagVarInfos[i].getNameGiven();
                if (varName == null) {
                    varName = collectedTagData.getTagData().getAttributeString(tagVarInfos[i].getNameFromAttribute());
                }
                else if (tagVarInfos[i].getNameFromAttribute() != null) {
                    // alias
                    continue;
                }
                String tmpVarName = "_jspx_" + varName + "_" + nestingLevel;
                tagWriter.print(varName);
                tagWriter.print(" = ");
                tagWriter.print(tmpVarName);
                tagWriter.println(";");
            }
        }
    }

    /*
     * Synchronizes the scripting variables of the given custom tag for
     * the given scope.
     */
    protected void syncScriptingVars(JavaCodeWriter tagWriter, int scope) {
        TagVariableInfo[] tagVarInfos = ti.getTagVariableInfos();
        VariableInfo[] varInfos = ti.getVariableInfo(collectedTagData.getTagData());

        if (varInfos == null)
            varInfos = new VariableInfo[0];
            
        if ((varInfos.length == 0) && (tagVarInfos.length == 0)) {
            return;
        }

        if (varInfos.length > 0) {
            for (int i = 0; i < varInfos.length; i++) {
                if (varInfos[i].getScope() == scope) {
                    tagWriter.print(varInfos[i].getVarName());
                    tagWriter.print(" = ((");
                    String className=varInfos[i].getClassName();
                    className = replaceCharacters(className); //PK40417 &gt;,&lt;,&amp;,<\\%, and %\\>
                    tagWriter.print(className);
                    tagWriter.print(") "+pageContextVar+".findAttribute(");    //PK65013
                    tagWriter.print(GeneratorUtils.quote(varInfos[i].getVarName()));
					tagWriter.print("))");
					// PK26741
					if(jspOptions.isUseRepeatInt() && isRepeatTag)
						tagWriter.println(".intValue();");
					else
                    	tagWriter.println(";");		// PK26741
                }
            }
        }
        else {
            for (int i = 0; i < tagVarInfos.length; i++) {
                if (tagVarInfos[i].getScope() == scope) {
                    String name = tagVarInfos[i].getNameGiven();
                    if (name == null) {
                        name = collectedTagData.getTagData().getAttributeString(tagVarInfos[i].getNameFromAttribute());
                    }
                    else if (tagVarInfos[i].getNameFromAttribute() != null) {
                        // alias
                        continue;
                    }
                    tagWriter.print(name);
					tagWriter.print(" = ((");
                    String className=tagVarInfos[i].getClassName();
                    className = replaceCharacters(className); //PK40417 &gt;,&lt;,&amp;,<\\%, and %\\>
                    tagWriter.print(className);
                    tagWriter.print(") "+pageContextVar+".findAttribute("); //PK65013
                    tagWriter.print(GeneratorUtils.quote(name));
					tagWriter.print("))");
					// PK26741
					if(jspOptions.isUseRepeatInt() && isRepeatTag)
						tagWriter.println(".intValue();");
					else
                    	tagWriter.println(";");		// PK26741
                }
            }
        }
    }
    
    private MethodWriter findAttributeWriter(String attributeName) {
        MethodWriter attributeWriter = null;
        
        for (Iterator itr = attributeWriterMap.values().iterator(); itr.hasNext();) {
            MethodWriter writer = (MethodWriter)itr.next();
            if (writer instanceof NamedAttributeWriter) {
                if (((NamedAttributeWriter)writer).getAttributeName().equals(attributeName)) {
                    attributeWriter = writer;
                    break;
                }
            }
        }
        return (attributeWriter);
    }
    
    private Vector getScriptingVars(int scope) {
        Vector vec = null;

        switch (scope) {
            case VariableInfo.AT_BEGIN :
                vec = collectedTagData.getAtBeginScriptingVars();
                break;
            case VariableInfo.AT_END :
            	//PK29373
			    if(jspOptions.isUseScriptVarDupInit() && collectedTagData.getAtEndDuplicateVars()!=null)
					vec = collectedTagData.getAtEndDuplicateVars();
				else   //PK29373
                vec = collectedTagData.getAtEndScriptingVars();
                break;
            case VariableInfo.NESTED :
                vec = collectedTagData.getNestedScriptingVars();
                break;
        }

        return vec;
    }
    
    private boolean containsVariableInfo(Vector vec, VariableInfo varInfo) {
        boolean found = false;
        
        for (Iterator itr = vec.iterator(); itr.hasNext();) {
            VariableInfo vi = (VariableInfo)itr.next();
            if (vi.getVarName().equals(varInfo.getVarName())) {
                found = true;
                break;    
            }
        }
        return found;
    }
    
    private boolean containsTagVariableInfo(Vector vec, TagVariableInfo tagVarInfo) {
        boolean found = false;
        
        for (Iterator itr = vec.iterator(); itr.hasNext();) {
            TagVariableInfo tvi = (TagVariableInfo)itr.next();
            String varName = tvi.getNameGiven();
            if (varName == null) {
                varName = collectedTagData.getTagData().getAttributeString(tvi.getNameFromAttribute());
            }
            String compareVarName = tagVarInfo.getNameGiven();
            if (compareVarName == null) {
                compareVarName = collectedTagData.getTagData().getAttributeString(tagVarInfo.getNameFromAttribute());
            }
            if (varName != null && compareVarName != null) {
                if (varName.equals(compareVarName)) {
                    found = true;
                    break;
                }    
            }
        }
        return found;
    }

    //PK40417 Method to replace translated characters to their original form.  This prevents compilation errors when escaped characters are used in tags.
    private String replaceCharacters(String name) {
        name = name.replaceAll("&gt;", ">");
        name = name.replaceAll("&lt;", "<");
        name = name.replaceAll("&amp;", "&");
        name = name.replaceAll("<\\%", "<%");
        name = name.replaceAll("%\\>", "%>"); 
        return name;
    }
    
}
