/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.testapp.g3store.restConsumer.model;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * @author anupag
 *
 */
@Schema(name = "AppStructureConsumer", description = "POJO that represents a single app entry.")
public class AppStructureConsumer {

    @Schema(required = true, example = "myApp", description = "App name given by the developer", defaultValue = "myApp")
    private String name;

    @Schema(required = false, example = "Famous myApp", description = "App desc given by the developer", defaultValue = "Famous App")
    private String desc;

    @Schema(required = false, example = "true", description = "App provided by the developer is free or not", defaultValue = "true")
    private Boolean free;

    @Schema(required = false, example = "NO_SECURITY, BASIC,  TOKEN_JWT, TOKEN_OAUTH2 , CUSTOM_AUTH", defaultValue = "NO_SECURITY")
    private SecurityType securityType;

    @Schema(required = false, description = "App genere defined by the developer", example = "GAME, NEWS , SOCIAL", defaultValue = "GAME")
    private GenreType genreType;

    @Schema(required = false, type = SchemaType.ARRAY, implementation = PriceModel.class, description = "App priceList defined by the developer")
    private List<PriceModel> priceList;

    @Schema(required = false, implementation = CreatorModel.class, description = "App creator ")
    private CreatorModel creator;

    public List<PriceModel> getPriceList() {
        return priceList;
    }

    public void setPriceList(List<PriceModel> priceList) {
        this.priceList = priceList;
    }

    public CreatorModel getCreator() {
        return creator;
    }

    public void setCreator(CreatorModel creator) {
        this.creator = creator;
    }

    public GenreType getGenreType() {
        return genreType;
    }

    public void setGenreType(GenreType genreType) {
        this.genreType = genreType;
    }

    public enum GenreType {
        GAME,
        NEWS,
        SOCIAL,
        TECH;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public Boolean getFree() {
        return free;
    }

    public void setFree(Boolean free) {
        this.free = free;
    }

    public enum SecurityType {
        BASIC,
        TOKEN_OAUTH2,
        TOKEN_JWT,
        CUSTOM_AUTH,
        NO_SECURITY;
    }

    public SecurityType getSecurityType() {
        return securityType;
    }

    public void setSecurityType(SecurityType securityType) {
        this.securityType = securityType;
    }

}
