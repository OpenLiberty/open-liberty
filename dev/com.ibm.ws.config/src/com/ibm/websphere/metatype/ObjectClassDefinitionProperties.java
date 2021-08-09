/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.metatype;

import java.util.List;

/**
 *
 */
public class ObjectClassDefinitionProperties {

    private boolean supportsHiddenExtensions = false;
    private String description;
    private final String id;
    private String name;
    private String alias;
    private String childalias;
    private String extendsAlias;
    private String extend;
    private String parentPid;
    private boolean supportsExtensions = false;
    private List<String> objectClass;

    /**
     * @param id2
     */
    public ObjectClassDefinitionProperties(String id2) {
        this.id = id2;
    }

    /**
     * @return the extend
     */
    public String getExtends() {
        return extend;
    }

    /**
     * @param extend the extend to set
     */
    public void setExtends(String extend) {
        this.extend = extend;
    }

    /**
     * @param supportsHiddenExtensions the supportsHiddenExtensions to set
     */
    public void setSupportsHiddenExtensions(boolean supportsHiddenExtensions) {
        this.supportsHiddenExtensions = supportsHiddenExtensions;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param alias the alias to set
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * @param childalias the childalias to set
     */
    public void setChildalias(String childalias) {
        this.childalias = childalias;
    }

    /**
     * @param extendsAlias the childalias to set
     */
    public void setExtendsAlias(String extendsAlias) {
        this.extendsAlias = extendsAlias;
    }

    /**
     * @param parentPid the parentPid to set
     */
    public void setParentPID(String parentPid) {
        this.parentPid = parentPid;
    }

    /**
     * @param supportsExtensions the supportsExtensions to set
     */
    public void setSupportsExtensions(boolean supportsExtensions) {
        this.supportsExtensions = supportsExtensions;
    }

    public void setObjectClass(List<String> objectClass) {
        this.objectClass = objectClass;
    }

    /**
     * @return the supportsHiddenExtensions
     */
    public boolean supportsHiddenExtensions() {
        return supportsHiddenExtensions;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @return the childalias
     */
    public String getChildalias() {
        return childalias;
    }

    /**
     * @return the extendsAlias
     */
    public String getExtendsAlias() {
        return extendsAlias;
    }

    /**
     * @return the parentPid
     */
    public String getParentPID() {
        return parentPid;
    }

    /**
     * @return the supportsExtensions
     */
    public boolean supportsExtensions() {
        return supportsExtensions;
    }

    /**
     * @return
     */
    public List<String> getObjectClass() {
        return objectClass;
    }

}
