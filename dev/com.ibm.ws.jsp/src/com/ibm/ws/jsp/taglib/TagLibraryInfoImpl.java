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
package com.ibm.ws.jsp.taglib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.jsp.tagext.FunctionInfo;
import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagFileInfo;
import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagLibraryInfo;
import javax.servlet.jsp.tagext.TagLibraryValidator;
import javax.servlet.jsp.tagext.TagVariableInfo;

import com.ibm.ws.jsp.inputsource.JspInputSourceContainerImpl;
import com.ibm.wsspi.jsp.resource.JspInputSource;


public class TagLibraryInfoImpl extends TagLibraryInfo {
	static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.taglib.TagLibraryInfoImpl";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}
	
	private static String TSX_URI="http://websphere.ibm.com/tags/tsx";
	private static String JSX_URI="http://websphere.ibm.com/tags/jsx";
    protected String tldOriginatorId = "";
    protected JspInputSource inputSource = null;
    protected Class tlvClass = null;
    protected Map tlvInitParams = null;
    protected HashMap<String,TagLibraryInfoImpl> tagLibMap = null; //jsp2.1work - to support getTagLibraryInfos() for JSP 2.1
    protected boolean container = false;
    
    public TagLibraryInfoImpl(String tldOriginatorId, JspInputSource inputSource) {
        super("", "");
        this.tldOriginatorId = tldOriginatorId;
        this.inputSource = inputSource;
        if (this.inputSource instanceof JspInputSourceContainerImpl) {
            container=true;
        }
    }
    
    protected TagLibraryInfoImpl(String prefix, String uri, String tldOriginatorId, JspInputSource inputSource) {
        super(prefix, uri);
        this.tldOriginatorId = tldOriginatorId;
        this.inputSource = inputSource;
        if (this.inputSource instanceof JspInputSourceContainerImpl) {
            container=true;
        }
    }
    
    /*protected TagFileInfo getTagFile(String tagFileName) {
        return new TagFileInfo(tagFileName, inputSource.getRelativeURL()+"/"+tagFileName, TagInfo);
    }*/
    
    public String getTldFilePath() {
        return inputSource.getRelativeURL();
    }
    
    public long getLoadedTimestamp() {
        return inputSource.getLastModified();
    }

    public JspInputSource getInputSource() {
        return inputSource;
    }
    
    void setPrefixString(String prefix) {
        this.prefix = prefix;
    }
    
    void setURI(String uri) {
        this.uri = uri;
    }
    
    void setInfoString(String info) {
        this.info = info;
    }
    
    void setReliableURN(String urn) {
        this.urn = urn;
    }
    
    void setRequiredVersion(String version) {
        this.jspversion = version;
    }
    
    void setShortName(String shortname) {
        this.shortname = shortname;
    }
    
    void setTlibversion(String tlibversion) {
        this.tlibversion = tlibversion;
    }
    
    void setTags(List tagList) {
        tags = new TagInfo[tagList.size()];
        if (tagList.size() > 0)
            tags = (TagInfo[])tagList.toArray(tags);
    }
    
    void setTags(TagInfo[] tags) {
        this.tags = tags;
    }
    
    void setTagFiles(List tagFileList) {
        tagFiles = new TagFileInfo[tagFileList.size()];
        if (tagFileList.size() > 0)
            tagFiles = (TagFileInfo[])tagFileList.toArray(tagFiles);
    }
    
    void setTagFiles(TagFileInfo[] tagFiles) {
        this.tagFiles = tagFiles;
    }
    
    void setFunctions(List functionList) {
        functions = new FunctionInfo[functionList.size()];
        if (functionList.size() > 0)
            functions = (FunctionInfo[])functionList.toArray(functions);
    }
    
    void setFunctions(FunctionInfo[] functions) {
        this.functions = functions;
    }
 
    void setTabLibraryValidator(Class tlvClass, Map tlvInitParams) {
        this.tlvClass = tlvClass;
        this.tlvInitParams = tlvInitParams;
    }
       
    public TagLibraryValidator getTagLibraryValidator() {
        TagLibraryValidator tlv = null;
        if (tlvClass != null) {
            try {
                tlv = (TagLibraryValidator)tlvClass.newInstance();
                if (tlvInitParams != null)
                    tlv.setInitParameters(tlvInitParams);
                else 
                    tlv.setInitParameters(new HashMap());                    
            }
            catch (IllegalAccessException e) {
				logger.logp(Level.WARNING, CLASS_NAME, "getTagLibraryValidator", "Illegal access of tag library validator [" + tlvClass.getName() +"]", e);
            }
            catch (InstantiationException e) {
				logger.logp(Level.WARNING, CLASS_NAME, "getTagLibraryValidator", "Failed to instantiate tag library validator [" + tlvClass.getName() +"]", e);
            } catch (NoClassDefFoundError e) {
                logger.logp(Level.WARNING, CLASS_NAME, "getTagLibraryValidator", "Failed to instantiate tag library validator [" + tlvClass.getName() +"]", e);
            }
        }
        
        return tlv;
    }
    
    public String getTlibversion() {
        return tlibversion;
    }
    
    public TagLibraryInfoImpl copy(String prefix) {
        TagLibraryInfoImpl tli = new TagLibraryInfoImpl(prefix, uri, tldOriginatorId, inputSource);
        tli.setInfoString(info);
        tli.setReliableURN(urn);
        tli.setRequiredVersion(jspversion);
        tli.setShortName(shortname);
        tli.setTlibversion(tlibversion);
        tli.setTags(tags);
        tli.setTagFiles(tagFiles);
        tli.setFunctions(functions);
        tli.setTabLibraryValidator(tlvClass, tlvInitParams);
        tli.setTagLibMap(this.tagLibMap);
        return (tli);
    }
    
    public String getOriginatorId() {
        return tldOriginatorId;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("TagLibraryInfo name [");
        if (shortname != null)
            sb.append(shortname);
        sb.append("] info [");
        if (info != null)
            sb.append(info);
        sb.append("] urn [");
        if (urn != null)
            sb.append(urn);
        sb.append("] jspversion [");
        if (jspversion != null)
            sb.append(jspversion);
        sb.append("] tlibversion [");
        if (tlibversion != null)
            sb.append(tlibversion);
        sb.append("]\n");
        
        if (tags != null) {
            for (int i = 0; i < tags.length; i++) {
                tagToString(sb, tags[i]);
            }
        }
        
        if (tagFiles != null) {
            for (int i = 0; i < tagFiles.length; i++) {
                sb.append("tagFile name [");
                if (tagFiles[i].getName() != null)
                    sb.append(tagFiles[i].getName());
                sb.append("] path [");
                if (tagFiles[i].getPath() != null)
                    sb.append(tagFiles[i].getPath());
                sb.append("]\n");
                if (tagFiles[i].getTagInfo() != null)
                    tagToString(sb, tagFiles[i].getTagInfo());
            }
        }
        
        if (functions != null) {
            for (int i = 0; i < functions.length; i++) {
                sb.append("function name [");
                if (functions[i].getName() != null)
                    sb.append(functions[i].getName());
                sb.append("] class [");
                if (functions[i].getFunctionClass() != null)
                    sb.append(functions[i].getFunctionClass());
                sb.append("] signature [");
                if (functions[i].getFunctionSignature() != null)
                    sb.append(functions[i].getFunctionSignature());
                sb.append("]\n");
            }
        }
        
        return (sb.toString());
    }
    
    private void tagToString(StringBuffer sb, TagInfo tag) {
        sb.append("tag tagName [");
        if (tag.getTagName() != null) 
            sb.append(tag.getTagName());
        sb.append("] tagClassName [");
        if (tag.getTagClassName() != null)
            sb.append(tag.getTagClassName());
        sb.append("] bodycontent [");
        if (tag.getBodyContent() != null)
            sb.append(tag.getBodyContent());
        sb.append("] info [");
        if (tag.getInfoString() != null)
            sb.append(tag.getInfoString());
        sb.append("] displayName [");
        if (tag.getDisplayName() != null)
            sb.append(tag.getDisplayName());
        sb.append("] smallIcon [");
        if (tag.getSmallIcon() != null)
            sb.append(tag.getSmallIcon());
        sb.append("] largeIcon [");
        if (tag.getLargeIcon() != null)
            sb.append(tag.getLargeIcon());
        sb.append("] dynamicAttributes [");
        sb.append(tag.hasDynamicAttributes());
        sb.append("]\n");
        
        if (tag.getAttributes() != null) {
            TagAttributeInfo[] attributes = tag.getAttributes();
            TagLibraryInfoImpl.printTagAttributeInfo(sb, attributes);
        }

        if (tag.getTagVariableInfos() != null) {
            TagVariableInfo[] variables = tag.getTagVariableInfos();
            TagLibraryInfoImpl.printTagVariableInfo(sb, variables);
        }        
    }

    public static void printTagVariableInfo(StringBuffer sb, TagVariableInfo[] variables ) {
        if (variables != null) {
            for (int j = 0; j < variables.length; j++) {
                sb.append("variable nameGiven [");
                if (variables[j].getNameGiven() != null)
                    sb.append(variables[j].getNameGiven());
                sb.append("] nameFromAttribute [");
                if (variables[j].getNameFromAttribute() != null)
                    sb.append(variables[j].getNameFromAttribute());
                sb.append("] className [");
                if (variables[j].getClassName() != null)
                    sb.append(variables[j].getClassName());
                sb.append("] declare [");
                sb.append(variables[j].getDeclare());                                
                sb.append("] scope [");
                sb.append(variables[j].getScope());                                
                sb.append("]\n");
            }
        }    	
    }
    
    public static void printTagAttributeInfo(StringBuffer sb, TagAttributeInfo[] attributes) {
        if (attributes != null) {
            for (int j = 0; j < attributes.length; j++) {
                sb.append("attribute name [");
                if (attributes[j].getName() != null)
                    sb.append(attributes[j].getName());
                sb.append("] type [");
                if (attributes[j].getTypeName() != null)
                    sb.append(attributes[j].getTypeName());
                sb.append("] requestTime [");
                sb.append(attributes[j].canBeRequestTime());
                sb.append("] fragment [");
                sb.append(attributes[j].isFragment());
                sb.append("] required [");
                sb.append(attributes[j].isRequired());
                sb.append("] isDeferredMethod [");
                sb.append(attributes[j].isDeferredMethod());
                sb.append("] isDeferredValue [");
                sb.append(attributes[j].isDeferredValue());
                sb.append("] ExpectedTypeName [");
                sb.append(attributes[j].getExpectedTypeName());
                sb.append("] MethodSignature [");
                sb.append(attributes[j].getMethodSignature());
                sb.append("]\n");
            }
        }    	
    }
    
    //jsp2.1work
    public void setTagLibMap(HashMap<String,TagLibraryInfoImpl> tlibMap) {
    	this.tagLibMap=tlibMap;
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "setTagLibMap","setTagLibMap:tlibMap =[" + tlibMap +"]");
		}
    }
    
    //jsp2.1work
	public TagLibraryInfo[] getTagLibraryInfos() {
		// We have to remove our implicit tag libraries, tsx and jsx.  This is not nice but
		// I'm harcoding here because there is no readily-available information that identifies our
		// implicit taglibs as such.  
		if (this.tagLibMap!=null) {
			ArrayList<TagLibraryInfoImpl> coll = new ArrayList<TagLibraryInfoImpl>();
			Set<String> keys = this.tagLibMap.keySet();
			Iterator itr=keys.iterator();
			TagLibraryInfoImpl item=null;
			String uri=null;
			while (itr.hasNext()) {
				uri=(String)itr.next();
				if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
					logger.logp(Level.FINER, CLASS_NAME, "getTagLibraryInfos","getTagLibraryInfos tagLibMap uri=[" + uri +"]");
				}
				if (!uri.equals(TSX_URI) && !uri.equals(JSX_URI)) {
					item=this.tagLibMap.get(uri); 
					if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
						logger.logp(Level.FINEST, CLASS_NAME, "getTagLibraryInfos","getTagLibraryInfos tagLibMap item to add to list=[" + item.uri +"]");
					}
					coll.add(item);
				}				
			}
			
			if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
				logger.logp(Level.FINER, CLASS_NAME, "getTagLibraryInfos","getTagLibraryInfos:tlibMap after removing jsx and tsx=[" + coll.toString() +"]");
			}
	        return (TagLibraryInfo[]) coll.toArray(new TagLibraryInfo[0]);
		}
		else {
			return null;
		}
	}
	
	public boolean isContainer() {
	    return container;
	}
}
