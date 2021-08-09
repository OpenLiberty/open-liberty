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
package com.ibm.ws.jsp.translator.visitor.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.servlet.jsp.tagext.TagData;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.jsp.bean.BeanRepository;
import com.ibm.ws.jsp.taglib.TagLibraryInfoImpl;
import com.ibm.ws.jsp.translator.visitor.JspVisitorResult;

public class ValidateResult extends JspVisitorResult { 
    protected String language = "";
    protected String pageEncoding = "";
    protected boolean isELIgnored = false;
    protected String trimDirectiveWhitespacesValue = null; // jsp2.1work
    protected String deferredSyntaxAllowedAsLiteralValue = null; // jsp2.1ELwork
    protected boolean trimDirectiveWhitespaces = false; // jsp2.1work
    protected boolean deferredSyntaxAllowedAsLiteral = false; // jsp2.1ELwork
    protected BeanRepository beanRepository = null;
    protected HashMap<String,TagLibraryInfoImpl> tagLibMap = new HashMap<String,TagLibraryInfoImpl>();
    protected ArrayList dependencyList = new ArrayList();
    protected HashMap collectedTagDataMap = new HashMap();
    protected ValidateFunctionMapper validateFunctionMapper = new ValidateFunctionMapper();
    // defect 393421 begin    
    private String omitXmlDecl = null;
    private String doctypeRoot = null;
    private String doctypePublic = null;
    private String doctypeSystem = null;
    // defect 393421 end    

    
    protected ValidateResult(String jspVisitorId) {
        super(jspVisitorId);
    }
    /**
     * Returns the beanRepository.
     * @return BeanRepository
     */
    public BeanRepository getBeanRepository() {
        return beanRepository;
    }

    /**
     * Returns the isELIgnored.
     * @return boolean
     */
    public boolean isELIgnored() {
        return isELIgnored;
    }
    
    // jsp2.1work
    public String getTrimDirectiveWhitespaces() {
        return (trimDirectiveWhitespacesValue);
    }
    
    // jsp2.1ELwork
    public String getDeferredSyntaxAllowedAsLiteral() {
        return (deferredSyntaxAllowedAsLiteralValue);
    }
    
    // jsp2.1work
    public boolean isTrimDirectiveWhitespaces() {
        return (trimDirectiveWhitespaces);
    }
    
    // jsp2.1ELwork
    public boolean isDeferredSyntaxAllowedAsLiteral() {
        return (deferredSyntaxAllowedAsLiteral);
    }

    /**
     * Returns the language.
     * @return String
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Returns the pageEncoding.
     * @return String
     */
    public String getPageEncoding() {
        return pageEncoding;
    }

    /**
     * Returns the tagPrefixUriMap.
     * @return HashMap
     */
    public HashMap<String,TagLibraryInfoImpl> getTagLibMap() {
        return tagLibMap;
    }

    /**
     * Sets the beanRepository.
     * @param beanRepository The beanRepository to set
     */
    public void setBeanRepository(BeanRepository beanRepository) {
        this.beanRepository = beanRepository;
    }

    /**
     * Sets the isELIgnored.
     * @param isELIgnored The isELIgnored to set
     */
    public void setIsELIgnored(boolean isELIgnored) {
        this.isELIgnored = isELIgnored;
    }

    // jsp2.1work
    public void setTrimDirectiveWhitespaces(String trimDirectiveWhitespaces) {
        this.trimDirectiveWhitespacesValue = trimDirectiveWhitespaces;
    }
    
    // jsp2.1ELwork
    public void setDeferredSyntaxAllowedAsLiteral(boolean deferredSyntaxAllowedAsLiteral) {
        this.deferredSyntaxAllowedAsLiteral = deferredSyntaxAllowedAsLiteral;
    }
    
    // jsp2.1work
    public void setTrimDirectiveWhitespaces(boolean trimDirectiveWhitespaces) {
        this.trimDirectiveWhitespaces = trimDirectiveWhitespaces;
    }
    
    // jsp2.1ELwork
    public void setDeferredSyntaxAllowedAsLiteral(String deferredSyntaxAllowedAsLiteral) {
        this.deferredSyntaxAllowedAsLiteralValue = deferredSyntaxAllowedAsLiteral;
    }

    /**
     * Sets the language.
     * @param language The language to set
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Sets the pageEncoding.
     * @param pageEncoding The pageEncoding to set
     */
    public void setPageEncoding(String pageEncoding) {
        this.pageEncoding = pageEncoding;
    }

    /**
     * Sets the tagPrefixUriMap.
     * @param tagPrefixUriMap The tagPrefixUriMap to set
     */
    public void setTagLibMap(HashMap tagLibMap) {
        this.tagLibMap = tagLibMap;
    }

    /**
     * @return
     */
    public ValidateFunctionMapper getValidateFunctionMapper() {
        return validateFunctionMapper;
    }

    /**
     * @param mapper
     */
    public void setValidateFunctionMapper(ValidateFunctionMapper mapper) {
        validateFunctionMapper = mapper;
    }
    
    /**
     * Returns the dependencyList.
     * @return ArrayList
     */
    public ArrayList getDependencyList() {
        return dependencyList;
    }
    
    public void addCollectTagData(Element tagElement,
                                  boolean isScriptless,
                                  boolean hasScriptingVars,
                                  Vector atBeginScriptingVars,
                                  Vector atEndScriptingVars,
                                  Vector nestedScriptingVars,
                                  TagData tagData,
                                  String varNameSuffix) {
        CollectedTagData collectedTagData = new CollectedTagData();
        collectedTagData.setIsScriptless(isScriptless);
        if (isScriptless == false) {
            rollupIsScriptlessFlag(tagElement);
        }
        collectedTagData.setHasScriptingVars(hasScriptingVars);
        if (hasScriptingVars) {
            rollupHasScriptingVars(tagElement);
        }
        collectedTagData.setAtBeginScriptingVars(atBeginScriptingVars);
        collectedTagData.setAtEndScriptingVars(atEndScriptingVars);
        collectedTagData.setNestedScriptingVars(nestedScriptingVars);
        collectedTagData.setTagData(tagData);
        collectedTagData.setVarNameSuffix(varNameSuffix);
        collectedTagDataMap.put(tagElement, collectedTagData);
    }
    
    public CollectedTagData getCollectedTagData(Element tagElement) {
        return ((CollectedTagData)collectedTagDataMap.get(tagElement));
    }
    
    private void rollupIsScriptlessFlag(Node n) {
        Node parent = n.getParentNode();
        if (parent != null) {
            if (parent.getNodeType() == Node.ELEMENT_NODE) {
                Element parentElement = (Element)parent;
                CollectedTagData collectedTagData = (CollectedTagData)collectedTagDataMap.get(parentElement);
                if (collectedTagData != null)
                    collectedTagData.setIsScriptless(false);                     
            }
            rollupIsScriptlessFlag(parent);
        }
    }

    private void rollupHasScriptingVars(Node n) {
        Node parent = n.getParentNode();
        if (parent != null) {
            if (parent.getNodeType() == Node.ELEMENT_NODE) {
                Element parentElement = (Element)parent;
                CollectedTagData collectedTagData = (CollectedTagData)collectedTagDataMap.get(parentElement);
                if (collectedTagData != null)
                    collectedTagData.setHasScriptingVars(true);                     
            }
            rollupHasScriptingVars(parent);
        }
    }

    /**
     * Sets the dependencyList.
     * @param dependencyList The dependencyList to set
     */
    public void setDependencyList(ArrayList dependencyList) {
        this.dependencyList = dependencyList;
    }
    
    // defect 393421 begin 
    public String getOmitXmlDecl() {
        return omitXmlDecl;
    }

    public void setOmitXmlDecl(String omit) {
        omitXmlDecl = omit;
    }

    public String getDoctypeRoot() {
        return doctypeRoot;
    }

    public void setDoctypeRoot(String doctypeRoot) {
        this.doctypeRoot = doctypeRoot;
    }

    public String getDoctypeSystem() {
        return doctypeSystem;
    }

    public void setDoctypeSystem(String doctypeSystem) {
        this.doctypeSystem = doctypeSystem;
    }

    public String getDoctypePublic() {
        return doctypePublic;
    }

    public void setDoctypePublic(String doctypePublic) {
        this.doctypePublic = doctypePublic;
    }
    // defect 393421 end 
    
    
    public class CollectedTagData {
        private boolean isScriptless = false;
        private boolean hasScriptingVars = false;
        private Vector atBeginScriptingVars = null;
        private Vector atEndScriptingVars = null;
        private Vector nestedScriptingVars = null;
        private TagData tagData = null;
        private String varNameSuffix = null;
        private Vector duplicateVars = null;   //PK29373

        //PK29373
		/**
		 * Returns the atEndDuplicateVars.
		 * @return Vector
		 */
		 public Vector getAtEndDuplicateVars() {
			return duplicateVars;
		 }
		 //PK29373
        
        /**
         * Returns the atBeginScriptingVars.
         * @return Vector
         */
        public Vector getAtBeginScriptingVars() {
            return atBeginScriptingVars;
        }

        /**
         * Returns the atEndScriptingVars.
         * @return Vector
         */
        public Vector getAtEndScriptingVars() {
            return atEndScriptingVars;
        }

        /**
         * Returns the hasScriptingVars.
         * @return boolean
         */
        public boolean hasScriptingVars() {
            return hasScriptingVars;
        }

        /**
         * Returns the isScriptless.
         * @return boolean
         */
        public boolean isScriptless() {
            return isScriptless;
        }

        /**
         * Returns the nestedScriptingVars.
         * @return Vector
         */
        public Vector getNestedScriptingVars() {
            return nestedScriptingVars;
        }

        //PK29373
		/**
		 * Would be called only when 'useScriptVarDupInit' attribute is set
		 * Sets the atEndDuplicateVars
		 */
	    public void setAtEndDuplicateVars(Vector duplicateVars) {
			this.duplicateVars = duplicateVars;
		}
		//PK29373

        /**
         * Sets the atBeginScriptingVars.
         * @param atBeginScriptingVars The atBeginScriptingVars to set
         */
        public void setAtBeginScriptingVars(Vector atBeginScriptingVars) {
            this.atBeginScriptingVars = atBeginScriptingVars;
        }

        /**
         * Sets the atEndScriptingVars.
         * @param atEndScriptingVars The atEndScriptingVars to set
         */
        public void setAtEndScriptingVars(Vector atEndScriptingVars) {
            this.atEndScriptingVars = atEndScriptingVars;
        }

        /**
         * Sets the hasScriptingVars.
         * @param hasScriptingVars The hasScriptingVars to set
         */
        public void setHasScriptingVars(boolean hasScriptingVars) {
            this.hasScriptingVars = hasScriptingVars;
        }

        /**
         * Sets the isScriptless.
         * @param isScriptless The isScriptless to set
         */
        public void setIsScriptless(boolean isScriptless) {
            this.isScriptless = isScriptless;
        }

        /**
         * Sets the nestedScriptingVars.
         * @param nestedScriptingVars The nestedScriptingVars to set
         */
        public void setNestedScriptingVars(Vector nestedScriptingVars) {
            this.nestedScriptingVars = nestedScriptingVars;
        }

        /**
         * Returns the tagData.
         * @return TagData
         */
        public TagData getTagData() {
            return tagData;
        }

        /**
         * Sets the tagData.
         * @param tagData The tagData to set
         */
        public void setTagData(TagData tagData) {
            this.tagData = tagData;
        }
        
        /**
         * @return
         */
        public String getVarNameSuffix() {
            return varNameSuffix;
        }

        /**
         * @param string
         */
        public void setVarNameSuffix(String string) {
            varNameSuffix = string;
        }
    }
}
