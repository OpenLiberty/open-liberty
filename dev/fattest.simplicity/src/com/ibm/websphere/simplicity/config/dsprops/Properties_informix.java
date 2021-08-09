/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config.dsprops;

import javax.xml.bind.annotation.XmlAttribute;

import com.ibm.websphere.simplicity.config.DataSourceProperties;

/**
 * Lists data source properties specific to this driver.
 */
public class Properties_informix extends DataSourceProperties {
    private String ifxCLIENT_LOCALE;
    private String ifxCPMAgeLimit;
    private String ifxCPMInitPoolSize;
    private String ifxCPMMaxConnections;
    private String ifxCPMMaxPoolSize;
    private String ifxCPMMinAgeLimit;
    private String ifxCPMMinPoolSize;
    private String ifxCPMServiceInterval;
    private String ifxDBANSIWARN;
    private String ifxDBCENTURY;
    private String ifxDBDATE;
    private String ifxDBSPACETEMP;
    private String ifxDBTEMP;
    private String ifxDBTIME;
    private String ifxDBUPSPACE;
    private String ifxDB_LOCALE;
    private String ifxDELIMIDENT;
    private String ifxENABLE_TYPE_CACHE;
    private String ifxFET_BUF_SIZE;
    private String ifxGL_DATE;
    private String ifxGL_DATETIME;
    private String ifxIFXHOST;
    private String ifxIFX_AUTOFREE;
    private String ifxIFX_DIRECTIVES;
    private String ifxIFX_LOCK_MODE_WAIT;
    private String ifxIFX_SOC_TIMEOUT;
    private String ifxIFX_USEPUT;
    private String ifxIFX_USE_STRENC;
    private String ifxIFX_XASPEC;
    private String ifxINFORMIXCONRETRY;
    private String ifxINFORMIXCONTIME;
    private String ifxINFORMIXOPCACHE;
    private String ifxINFORMIXSTACKSIZE;
    private String ifxJDBCTEMP;
    private String ifxLDAP_IFXBASE;
    private String ifxLDAP_PASSWD;
    private String ifxLDAP_URL;
    private String ifxLDAP_USER;
    private String ifxLOBCACHE;
    private String ifxNEWCODESET;
    private String ifxNEWLOCALE;
    private String ifxNODEFDAC;
    private String ifxOPTCOMPIND;
    private String ifxOPTOFC;
    private String ifxOPT_GOAL;
    private String ifxPATH;
    private String ifxPDQPRIORITY;
    private String ifxPLCONFIG;
    private String ifxPLOAD_LO_PATH;
    private String ifxPROTOCOLTRACE;
    private String ifxPROTOCOLTRACEFILE;
    private String ifxPROXY;
    private String ifxPSORT_DBTEMP;
    private String ifxPSORT_NPROCS;
    private String ifxSECURITY;
    private String ifxSQLH_FILE;
    private String ifxSQLH_LOC;
    private String ifxSQLH_TYPE;
    private String ifxSSLCONNECTION;
    private String ifxSTMT_CACHE;
    private String ifxTRACE;
    private String ifxTRACEFILE;
    private String ifxTRUSTED_CONTEXT;
    private String ifxUSEV5SERVER;
    private String ifxUSE_DTENV;
    private String roleName;

    @Override
    public String getElementName() {
        return INFORMIX_JDBC;
    }

    @XmlAttribute(name = "ifxCLIENT_LOCALE")
    public void setIfxCLIENT_LOCALE(String ifxCLIENT_LOCALE) {
        this.ifxCLIENT_LOCALE = ifxCLIENT_LOCALE;
    }

    public String getIfxCLIENT_LOCALE() {
        return this.ifxCLIENT_LOCALE;
    }

    @XmlAttribute(name = "ifxCPMAgeLimit")
    public void setIfxCPMAgeLimit(String ifxCPMAgeLimit) {
        this.ifxCPMAgeLimit = ifxCPMAgeLimit;
    }

    public String getIfxCPMAgeLimit() {
        return this.ifxCPMAgeLimit;
    }

    @XmlAttribute(name = "ifxCPMInitPoolSize")
    public void setIfxCPMInitPoolSize(String ifxCPMInitPoolSize) {
        this.ifxCPMInitPoolSize = ifxCPMInitPoolSize;
    }

    public String getIfxCPMInitPoolSize() {
        return this.ifxCPMInitPoolSize;
    }

    @XmlAttribute(name = "ifxCPMMaxConnections")
    public void setIfxCPMMaxConnections(String ifxCPMMaxConnections) {
        this.ifxCPMMaxConnections = ifxCPMMaxConnections;
    }

    public String getIfxCPMMaxConnections() {
        return this.ifxCPMMaxConnections;
    }

    @XmlAttribute(name = "ifxCPMMaxPoolSize")
    public void setIfxCPMMaxPoolSize(String ifxCPMMaxPoolSize) {
        this.ifxCPMMaxPoolSize = ifxCPMMaxPoolSize;
    }

    public String getIfxCPMMaxPoolSize() {
        return this.ifxCPMMaxPoolSize;
    }

    @XmlAttribute(name = "ifxCPMMinAgeLimit")
    public void setIfxCPMMinAgeLimit(String ifxCPMMinAgeLimit) {
        this.ifxCPMMinAgeLimit = ifxCPMMinAgeLimit;
    }

    public String getIfxCPMMinAgeLimit() {
        return this.ifxCPMMinAgeLimit;
    }

    @XmlAttribute(name = "ifxCPMMinPoolSize")
    public void setIfxCPMMinPoolSize(String ifxCPMMinPoolSize) {
        this.ifxCPMMinPoolSize = ifxCPMMinPoolSize;
    }

    public String getIfxCPMMinPoolSize() {
        return this.ifxCPMMinPoolSize;
    }

    @XmlAttribute(name = "ifxCPMServiceInterval")
    public void setIfxCPMServiceInterval(String ifxCPMServiceInterval) {
        this.ifxCPMServiceInterval = ifxCPMServiceInterval;
    }

    public String getIfxCPMServiceInterval() {
        return this.ifxCPMServiceInterval;
    }

    @XmlAttribute(name = "ifxDBANSIWARN")
    public void setIfxDBANSIWARN(String ifxDBANSIWARN) {
        this.ifxDBANSIWARN = ifxDBANSIWARN;
    }

    public String getIfxDBANSIWARN() {
        return this.ifxDBANSIWARN;
    }

    @XmlAttribute(name = "ifxDBCENTURY")
    public void setIfxDBCENTURY(String ifxDBCENTURY) {
        this.ifxDBCENTURY = ifxDBCENTURY;
    }

    public String getIfxDBCENTURY() {
        return this.ifxDBCENTURY;
    }

    @XmlAttribute(name = "ifxDBDATE")
    public void setIfxDBDATE(String ifxDBDATE) {
        this.ifxDBDATE = ifxDBDATE;
    }

    public String getIfxDBDATE() {
        return this.ifxDBDATE;
    }

    @XmlAttribute(name = "ifxDBSPACETEMP")
    public void setIfxDBSPACETEMP(String ifxDBSPACETEMP) {
        this.ifxDBSPACETEMP = ifxDBSPACETEMP;
    }

    public String getIfxDBSPACETEMP() {
        return this.ifxDBSPACETEMP;
    }

    @XmlAttribute(name = "ifxDBTEMP")
    public void setIfxDBTEMP(String ifxDBTEMP) {
        this.ifxDBTEMP = ifxDBTEMP;
    }

    public String getIfxDBTEMP() {
        return this.ifxDBTEMP;
    }

    @XmlAttribute(name = "ifxDBTIME")
    public void setIfxDBTIME(String ifxDBTIME) {
        this.ifxDBTIME = ifxDBTIME;
    }

    public String getIfxDBTIME() {
        return this.ifxDBTIME;
    }

    @XmlAttribute(name = "ifxDBUPSPACE")
    public void setIfxDBUPSPACE(String ifxDBUPSPACE) {
        this.ifxDBUPSPACE = ifxDBUPSPACE;
    }

    public String getIfxDBUPSPACE() {
        return this.ifxDBUPSPACE;
    }

    @XmlAttribute(name = "ifxDB_LOCALE")
    public void setIfxDB_LOCALE(String ifxDB_LOCALE) {
        this.ifxDB_LOCALE = ifxDB_LOCALE;
    }

    public String getIfxDB_LOCALE() {
        return this.ifxDB_LOCALE;
    }

    @XmlAttribute(name = "ifxDELIMIDENT")
    public void setIfxDELIMIDENT(String ifxDELIMIDENT) {
        this.ifxDELIMIDENT = ifxDELIMIDENT;
    }

    public String getIfxDELIMIDENT() {
        return this.ifxDELIMIDENT;
    }

    @XmlAttribute(name = "ifxENABLE_TYPE_CACHE")
    public void setIfxENABLE_TYPE_CACHE(String ifxENABLE_TYPE_CACHE) {
        this.ifxENABLE_TYPE_CACHE = ifxENABLE_TYPE_CACHE;
    }

    public String getIfxENABLE_TYPE_CACHE() {
        return this.ifxENABLE_TYPE_CACHE;
    }

    @XmlAttribute(name = "ifxFET_BUF_SIZE")
    public void setIfxFET_BUF_SIZE(String ifxFET_BUF_SIZE) {
        this.ifxFET_BUF_SIZE = ifxFET_BUF_SIZE;
    }

    public String getIfxFET_BUF_SIZE() {
        return this.ifxFET_BUF_SIZE;
    }

    @XmlAttribute(name = "ifxGL_DATE")
    public void setIfxGL_DATE(String ifxGL_DATE) {
        this.ifxGL_DATE = ifxGL_DATE;
    }

    public String getIfxGL_DATE() {
        return this.ifxGL_DATE;
    }

    @XmlAttribute(name = "ifxGL_DATETIME")
    public void setIfxGL_DATETIME(String ifxGL_DATETIME) {
        this.ifxGL_DATETIME = ifxGL_DATETIME;
    }

    public String getIfxGL_DATETIME() {
        return this.ifxGL_DATETIME;
    }

    @XmlAttribute(name = "ifxIFXHOST")
    public void setIfxIFXHOST(String ifxIFXHOST) {
        this.ifxIFXHOST = ifxIFXHOST;
    }

    public String getIfxIFXHOST() {
        return this.ifxIFXHOST;
    }

    @XmlAttribute(name = "ifxIFX_AUTOFREE")
    public void setIfxIFX_AUTOFREE(String ifxIFX_AUTOFREE) {
        this.ifxIFX_AUTOFREE = ifxIFX_AUTOFREE;
    }

    public String getIfxIFX_AUTOFREE() {
        return this.ifxIFX_AUTOFREE;
    }

    @XmlAttribute(name = "ifxIFX_DIRECTIVES")
    public void setIfxIFX_DIRECTIVES(String ifxIFX_DIRECTIVES) {
        this.ifxIFX_DIRECTIVES = ifxIFX_DIRECTIVES;
    }

    public String getIfxIFX_DIRECTIVES() {
        return this.ifxIFX_DIRECTIVES;
    }

    @XmlAttribute(name = "ifxIFX_LOCK_MODE_WAIT")
    public void setIfxIFX_LOCK_MODE_WAIT(String ifxIFX_LOCK_MODE_WAIT) {
        this.ifxIFX_LOCK_MODE_WAIT = ifxIFX_LOCK_MODE_WAIT;
    }

    public String getIfxIFX_LOCK_MODE_WAIT() {
        return this.ifxIFX_LOCK_MODE_WAIT;
    }

    @XmlAttribute(name = "ifxIFX_SOC_TIMEOUT")
    public void setIfxIFX_SOC_TIMEOUT(String ifxIFX_SOC_TIMEOUT) {
        this.ifxIFX_SOC_TIMEOUT = ifxIFX_SOC_TIMEOUT;
    }

    public String getIfxIFX_SOC_TIMEOUT() {
        return this.ifxIFX_SOC_TIMEOUT;
    }

    @XmlAttribute(name = "ifxIFX_USEPUT")
    public void setIfxIFX_USEPUT(String ifxIFX_USEPUT) {
        this.ifxIFX_USEPUT = ifxIFX_USEPUT;
    }

    public String getIfxIFX_USEPUT() {
        return this.ifxIFX_USEPUT;
    }

    @XmlAttribute(name = "ifxIFX_USE_STRENC")
    public void setIfxIFX_USE_STRENC(String ifxIFX_USE_STRENC) {
        this.ifxIFX_USE_STRENC = ifxIFX_USE_STRENC;
    }

    public String getIfxIFX_USE_STRENC() {
        return this.ifxIFX_USE_STRENC;
    }

    @XmlAttribute(name = "ifxIFX_XASPEC")
    public void setIfxIFX_XASPEC(String ifxIFX_XASPEC) {
        this.ifxIFX_XASPEC = ifxIFX_XASPEC;
    }

    public String getIfxIFX_XASPEC() {
        return this.ifxIFX_XASPEC;
    }

    @XmlAttribute(name = "ifxINFORMIXCONRETRY")
    public void setIfxINFORMIXCONRETRY(String ifxINFORMIXCONRETRY) {
        this.ifxINFORMIXCONRETRY = ifxINFORMIXCONRETRY;
    }

    public String getIfxINFORMIXCONRETRY() {
        return this.ifxINFORMIXCONRETRY;
    }

    @XmlAttribute(name = "ifxINFORMIXCONTIME")
    public void setIfxINFORMIXCONTIME(String ifxINFORMIXCONTIME) {
        this.ifxINFORMIXCONTIME = ifxINFORMIXCONTIME;
    }

    public String getIfxINFORMIXCONTIME() {
        return this.ifxINFORMIXCONTIME;
    }

    @XmlAttribute(name = "ifxINFORMIXOPCACHE")
    public void setIfxINFORMIXOPCACHE(String ifxINFORMIXOPCACHE) {
        this.ifxINFORMIXOPCACHE = ifxINFORMIXOPCACHE;
    }

    public String getIfxINFORMIXOPCACHE() {
        return this.ifxINFORMIXOPCACHE;
    }

    @XmlAttribute(name = "ifxINFORMIXSTACKSIZE")
    public void setIfxINFORMIXSTACKSIZE(String ifxINFORMIXSTACKSIZE) {
        this.ifxINFORMIXSTACKSIZE = ifxINFORMIXSTACKSIZE;
    }

    public String getIfxINFORMIXSTACKSIZE() {
        return this.ifxINFORMIXSTACKSIZE;
    }

    @XmlAttribute(name = "ifxJDBCTEMP")
    public void setIfxJDBCTEMP(String ifxJDBCTEMP) {
        this.ifxJDBCTEMP = ifxJDBCTEMP;
    }

    public String getIfxJDBCTEMP() {
        return this.ifxJDBCTEMP;
    }

    @XmlAttribute(name = "ifxLDAP_IFXBASE")
    public void setIfxLDAP_IFXBASE(String ifxLDAP_IFXBASE) {
        this.ifxLDAP_IFXBASE = ifxLDAP_IFXBASE;
    }

    public String getIfxLDAP_IFXBASE() {
        return this.ifxLDAP_IFXBASE;
    }

    @XmlAttribute(name = "ifxLDAP_PASSWD")
    public void setIfxLDAP_PASSWD(String ifxLDAP_PASSWD) {
        this.ifxLDAP_PASSWD = ifxLDAP_PASSWD;
    }

    public String getIfxLDAP_PASSWD() {
        return this.ifxLDAP_PASSWD;
    }

    @XmlAttribute(name = "ifxLDAP_URL")
    public void setIfxLDAP_URL(String ifxLDAP_URL) {
        this.ifxLDAP_URL = ifxLDAP_URL;
    }

    public String getIfxLDAP_URL() {
        return this.ifxLDAP_URL;
    }

    @XmlAttribute(name = "ifxLDAP_USER")
    public void setIfxLDAP_USER(String ifxLDAP_USER) {
        this.ifxLDAP_USER = ifxLDAP_USER;
    }

    public String getIfxLDAP_USER() {
        return this.ifxLDAP_USER;
    }

    @XmlAttribute(name = "ifxLOBCACHE")
    public void setIfxLOBCACHE(String ifxLOBCACHE) {
        this.ifxLOBCACHE = ifxLOBCACHE;
    }

    public String getIfxLOBCACHE() {
        return this.ifxLOBCACHE;
    }

    @XmlAttribute(name = "ifxNEWCODESET")
    public void setIfxNEWCODESET(String ifxNEWCODESET) {
        this.ifxNEWCODESET = ifxNEWCODESET;
    }

    public String getIfxNEWCODESET() {
        return this.ifxNEWCODESET;
    }

    @XmlAttribute(name = "ifxNEWLOCALE")
    public void setIfxNEWLOCALE(String ifxNEWLOCALE) {
        this.ifxNEWLOCALE = ifxNEWLOCALE;
    }

    public String getIfxNEWLOCALE() {
        return this.ifxNEWLOCALE;
    }

    @XmlAttribute(name = "ifxNODEFDAC")
    public void setIfxNODEFDAC(String ifxNODEFDAC) {
        this.ifxNODEFDAC = ifxNODEFDAC;
    }

    public String getIfxNODEFDAC() {
        return this.ifxNODEFDAC;
    }

    @XmlAttribute(name = "ifxOPTCOMPIND")
    public void setIfxOPTCOMPIND(String ifxOPTCOMPIND) {
        this.ifxOPTCOMPIND = ifxOPTCOMPIND;
    }

    public String getIfxOPTCOMPIND() {
        return this.ifxOPTCOMPIND;
    }

    @XmlAttribute(name = "ifxOPTOFC")
    public void setIfxOPTOFC(String ifxOPTOFC) {
        this.ifxOPTOFC = ifxOPTOFC;
    }

    public String getIfxOPTOFC() {
        return this.ifxOPTOFC;
    }

    @XmlAttribute(name = "ifxOPT_GOAL")
    public void setIfxOPT_GOAL(String ifxOPT_GOAL) {
        this.ifxOPT_GOAL = ifxOPT_GOAL;
    }

    public String getIfxOPT_GOAL() {
        return this.ifxOPT_GOAL;
    }

    @XmlAttribute(name = "ifxPATH")
    public void setIfxPATH(String ifxPATH) {
        this.ifxPATH = ifxPATH;
    }

    public String getIfxPATH() {
        return this.ifxPATH;
    }

    @XmlAttribute(name = "ifxPDQPRIORITY")
    public void setIfxPDQPRIORITY(String ifxPDQPRIORITY) {
        this.ifxPDQPRIORITY = ifxPDQPRIORITY;
    }

    public String getIfxPDQPRIORITY() {
        return this.ifxPDQPRIORITY;
    }

    @XmlAttribute(name = "ifxPLCONFIG")
    public void setIfxPLCONFIG(String ifxPLCONFIG) {
        this.ifxPLCONFIG = ifxPLCONFIG;
    }

    public String getIfxPLCONFIG() {
        return this.ifxPLCONFIG;
    }

    @XmlAttribute(name = "ifxPLOAD_LO_PATH")
    public void setIfxPLOAD_LO_PATH(String ifxPLOAD_LO_PATH) {
        this.ifxPLOAD_LO_PATH = ifxPLOAD_LO_PATH;
    }

    public String getIfxPLOAD_LO_PATH() {
        return this.ifxPLOAD_LO_PATH;
    }

    @XmlAttribute(name = "ifxPROTOCOLTRACE")
    public void setIfxPROTOCOLTRACE(String ifxPROTOCOLTRACE) {
        this.ifxPROTOCOLTRACE = ifxPROTOCOLTRACE;
    }

    public String getIfxPROTOCOLTRACE() {
        return this.ifxPROTOCOLTRACE;
    }

    @XmlAttribute(name = "ifxPROTOCOLTRACEFILE")
    public void setIfxPROTOCOLTRACEFILE(String ifxPROTOCOLTRACEFILE) {
        this.ifxPROTOCOLTRACEFILE = ifxPROTOCOLTRACEFILE;
    }

    public String getIfxPROTOCOLTRACEFILE() {
        return this.ifxPROTOCOLTRACEFILE;
    }

    @XmlAttribute(name = "ifxPROXY")
    public void setIfxPROXY(String ifxPROXY) {
        this.ifxPROXY = ifxPROXY;
    }

    public String getIfxPROXY() {
        return this.ifxPROXY;
    }

    @XmlAttribute(name = "ifxPSORT_DBTEMP")
    public void setIfxPSORT_DBTEMP(String ifxPSORT_DBTEMP) {
        this.ifxPSORT_DBTEMP = ifxPSORT_DBTEMP;
    }

    public String getIfxPSORT_DBTEMP() {
        return this.ifxPSORT_DBTEMP;
    }

    @XmlAttribute(name = "ifxPSORT_NPROCS")
    public void setIfxPSORT_NPROCS(String ifxPSORT_NPROCS) {
        this.ifxPSORT_NPROCS = ifxPSORT_NPROCS;
    }

    public String getIfxPSORT_NPROCS() {
        return this.ifxPSORT_NPROCS;
    }

    @XmlAttribute(name = "ifxSECURITY")
    public void setIfxSECURITY(String ifxSECURITY) {
        this.ifxSECURITY = ifxSECURITY;
    }

    public String getIfxSECURITY() {
        return this.ifxSECURITY;
    }

    @XmlAttribute(name = "ifxSQLH_FILE")
    public void setIfxSQLH_FILE(String ifxSQLH_FILE) {
        this.ifxSQLH_FILE = ifxSQLH_FILE;
    }

    public String getIfxSQLH_FILE() {
        return this.ifxSQLH_FILE;
    }

    @XmlAttribute(name = "ifxSQLH_LOC")
    public void setIfxSQLH_LOC(String ifxSQLH_LOC) {
        this.ifxSQLH_LOC = ifxSQLH_LOC;
    }

    public String getIfxSQLH_LOC() {
        return this.ifxSQLH_LOC;
    }

    @XmlAttribute(name = "ifxSQLH_TYPE")
    public void setIfxSQLH_TYPE(String ifxSQLH_TYPE) {
        this.ifxSQLH_TYPE = ifxSQLH_TYPE;
    }

    public String getIfxSQLH_TYPE() {
        return this.ifxSQLH_TYPE;
    }

    @XmlAttribute(name = "ifxSSLCONNECTION")
    public void setIfxSSLCONNECTION(String ifxSSLCONNECTION) {
        this.ifxSSLCONNECTION = ifxSSLCONNECTION;
    }

    public String getIfxSSLCONNECTION() {
        return this.ifxSSLCONNECTION;
    }

    @XmlAttribute(name = "ifxSTMT_CACHE")
    public void setIfxSTMT_CACHE(String ifxSTMT_CACHE) {
        this.ifxSTMT_CACHE = ifxSTMT_CACHE;
    }

    public String getIfxSTMT_CACHE() {
        return this.ifxSTMT_CACHE;
    }

    @XmlAttribute(name = "ifxTRACE")
    public void setIfxTRACE(String ifxTRACE) {
        this.ifxTRACE = ifxTRACE;
    }

    public String getIfxTRACE() {
        return this.ifxTRACE;
    }

    @XmlAttribute(name = "ifxTRACEFILE")
    public void setIfxTRACEFILE(String ifxTRACEFILE) {
        this.ifxTRACEFILE = ifxTRACEFILE;
    }

    public String getIfxTRACEFILE() {
        return this.ifxTRACEFILE;
    }

    @XmlAttribute(name = "ifxTRUSTED_CONTEXT")
    public void setIfxTRUSTED_CONTEXT(String ifxTRUSTED_CONTEXT) {
        this.ifxTRUSTED_CONTEXT = ifxTRUSTED_CONTEXT;
    }

    public String getIfxTRUSTED_CONTEXT() {
        return this.ifxTRUSTED_CONTEXT;
    }

    @XmlAttribute(name = "ifxUSEV5SERVER")
    public void setIfxUSEV5SERVER(String ifxUSEV5SERVER) {
        this.ifxUSEV5SERVER = ifxUSEV5SERVER;
    }

    public String getIfxUSEV5SERVER() {
        return this.ifxUSEV5SERVER;
    }

    @XmlAttribute(name = "ifxUSE_DTENV")
    public void setIfxUSE_DTENV(String ifxUSE_DTENV) {
        this.ifxUSE_DTENV = ifxUSE_DTENV;
    }

    public String getIfxUSE_DTENV() {
        return this.ifxUSE_DTENV;
    }

    @XmlAttribute(name = "roleName")
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return this.roleName;
    }

    /**
     * Returns a String listing the properties and their values used on this
     * data source.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("{");
        if (super.getDatabaseName() != null)
            buf.append("databaseName=\"" + super.getDatabaseName() + "\" ");
        if (ifxCLIENT_LOCALE != null)
            buf.append("ifxCLIENT_LOCALE=\"" + ifxCLIENT_LOCALE + "\" ");
        if (ifxCPMAgeLimit != null)
            buf.append("ifxCPMAgeLimit=\"" + ifxCPMAgeLimit + "\" ");
        if (ifxCPMInitPoolSize != null)
            buf.append("ifxCPMInitPoolSize=\"" + ifxCPMInitPoolSize + "\" ");
        if (ifxCPMMaxConnections != null)
            buf.append("ifxCPMMaxConnections=\"" + ifxCPMMaxConnections + "\" ");
        if (ifxCPMMaxPoolSize != null)
            buf.append("ifxCPMMaxPoolSize=\"" + ifxCPMMaxPoolSize + "\" ");
        if (ifxCPMMinAgeLimit != null)
            buf.append("ifxCPMMinAgeLimit=\"" + ifxCPMMinAgeLimit + "\" ");
        if (ifxCPMMinPoolSize != null)
            buf.append("ifxCPMMinPoolSize=\"" + ifxCPMMinPoolSize + "\" ");
        if (ifxCPMServiceInterval != null)
            buf.append("ifxCPMServiceInterval=\"" + ifxCPMServiceInterval + "\" ");
        if (ifxDBANSIWARN != null)
            buf.append("ifxDBANSIWARN=\"" + ifxDBANSIWARN + "\" ");
        if (ifxDBCENTURY != null)
            buf.append("ifxDBCENTURY=\"" + ifxDBCENTURY + "\" ");
        if (ifxDBDATE != null)
            buf.append("ifxDBDATE=\"" + ifxDBDATE + "\" ");
        if (ifxDBSPACETEMP != null)
            buf.append("ifxDBSPACETEMP=\"" + ifxDBSPACETEMP + "\" ");
        if (ifxDBTEMP != null)
            buf.append("ifxDBTEMP=\"" + ifxDBTEMP + "\" ");
        if (ifxDBTIME != null)
            buf.append("ifxDBTIME=\"" + ifxDBTIME + "\" ");
        if (ifxDBUPSPACE != null)
            buf.append("ifxDBUPSPACE=\"" + ifxDBUPSPACE + "\" ");
        if (ifxDB_LOCALE != null)
            buf.append("ifxDB_LOCALE=\"" + ifxDB_LOCALE + "\" ");
        if (ifxDELIMIDENT != null)
            buf.append("ifxDELIMIDENT=\"" + ifxDELIMIDENT + "\" ");
        if (ifxENABLE_TYPE_CACHE != null)
            buf.append("ifxENABLE_TYPE_CACHE=\"" + ifxENABLE_TYPE_CACHE + "\" ");
        if (ifxFET_BUF_SIZE != null)
            buf.append("ifxFET_BUF_SIZE=\"" + ifxFET_BUF_SIZE + "\" ");
        if (ifxGL_DATE != null)
            buf.append("ifxGL_DATE=\"" + ifxGL_DATE + "\" ");
        if (ifxGL_DATETIME != null)
            buf.append("ifxGL_DATETIME=\"" + ifxGL_DATETIME + "\" ");
        if (ifxIFXHOST != null)
            buf.append("ifxIFXHOST=\"" + ifxIFXHOST + "\" ");
        if (ifxIFX_AUTOFREE != null)
            buf.append("ifxIFX_AUTOFREE=\"" + ifxIFX_AUTOFREE + "\" ");
        if (ifxIFX_DIRECTIVES != null)
            buf.append("ifxIFX_DIRECTIVES=\"" + ifxIFX_DIRECTIVES + "\" ");
        if (ifxIFX_LOCK_MODE_WAIT != null)
            buf.append("ifxIFX_LOCK_MODE_WAIT=\"" + ifxIFX_LOCK_MODE_WAIT + "\" ");
        if (ifxIFX_SOC_TIMEOUT != null)
            buf.append("ifxIFX_SOC_TIMEOUT=\"" + ifxIFX_SOC_TIMEOUT + "\" ");
        if (ifxIFX_USEPUT != null)
            buf.append("ifxIFX_USEPUT=\"" + ifxIFX_USEPUT + "\" ");
        if (ifxIFX_USE_STRENC != null)
            buf.append("ifxIFX_USE_STRENC=\"" + ifxIFX_USE_STRENC + "\" ");
        if (ifxIFX_XASPEC != null)
            buf.append("ifxIFX_XASPEC=\"" + ifxIFX_XASPEC + "\" ");
        if (ifxINFORMIXCONRETRY != null)
            buf.append("ifxINFORMIXCONRETRY=\"" + ifxINFORMIXCONRETRY + "\" ");
        if (ifxINFORMIXCONTIME != null)
            buf.append("ifxINFORMIXCONTIME=\"" + ifxINFORMIXCONTIME + "\" ");
        if (ifxINFORMIXOPCACHE != null)
            buf.append("ifxINFORMIXOPCACHE=\"" + ifxINFORMIXOPCACHE + "\" ");
        if (ifxINFORMIXSTACKSIZE != null)
            buf.append("ifxINFORMIXSTACKSIZE=\"" + ifxINFORMIXSTACKSIZE + "\" ");
        if (ifxJDBCTEMP != null)
            buf.append("ifxJDBCTEMP=\"" + ifxJDBCTEMP + "\" ");
        if (ifxLDAP_IFXBASE != null)
            buf.append("ifxLDAP_IFXBASE=\"" + ifxLDAP_IFXBASE + "\" ");
        if (ifxLDAP_PASSWD != null)
            buf.append("ifxLDAP_PASSWD=\"" + ifxLDAP_PASSWD + "\" ");
        if (ifxLDAP_URL != null)
            buf.append("ifxLDAP_URL=\"" + ifxLDAP_URL + "\" ");
        if (ifxLDAP_USER != null)
            buf.append("ifxLDAP_USER=\"" + ifxLDAP_USER + "\" ");
        if (ifxLOBCACHE != null)
            buf.append("ifxLOBCACHE=\"" + ifxLOBCACHE + "\" ");
        if (ifxNEWCODESET != null)
            buf.append("ifxNEWCODESET=\"" + ifxNEWCODESET + "\" ");
        if (ifxNEWLOCALE != null)
            buf.append("ifxNEWLOCALE=\"" + ifxNEWLOCALE + "\" ");
        if (ifxNODEFDAC != null)
            buf.append("ifxNODEFDAC=\"" + ifxNODEFDAC + "\" ");
        if (ifxOPTCOMPIND != null)
            buf.append("ifxOPTCOMPIND=\"" + ifxOPTCOMPIND + "\" ");
        if (ifxOPTOFC != null)
            buf.append("ifxOPTOFC=\"" + ifxOPTOFC + "\" ");
        if (ifxOPT_GOAL != null)
            buf.append("ifxOPT_GOAL=\"" + ifxOPT_GOAL + "\" ");
        if (ifxPATH != null)
            buf.append("ifxPATH=\"" + ifxPATH + "\" ");
        if (ifxPDQPRIORITY != null)
            buf.append("ifxPDQPRIORITY=\"" + ifxPDQPRIORITY + "\" ");
        if (ifxPLCONFIG != null)
            buf.append("ifxPLCONFIG=\"" + ifxPLCONFIG + "\" ");
        if (ifxPLOAD_LO_PATH != null)
            buf.append("ifxPLOAD_LO_PATH=\"" + ifxPLOAD_LO_PATH + "\" ");
        if (ifxPROTOCOLTRACE != null)
            buf.append("ifxPROTOCOLTRACE=\"" + ifxPROTOCOLTRACE + "\" ");
        if (ifxPROTOCOLTRACEFILE != null)
            buf.append("ifxPROTOCOLTRACEFILE=\"" + ifxPROTOCOLTRACEFILE + "\" ");
        if (ifxPROXY != null)
            buf.append("ifxPROXY=\"" + ifxPROXY + "\" ");
        if (ifxPSORT_DBTEMP != null)
            buf.append("ifxPSORT_DBTEMP=\"" + ifxPSORT_DBTEMP + "\" ");
        if (ifxPSORT_NPROCS != null)
            buf.append("ifxPSORT_NPROCS=\"" + ifxPSORT_NPROCS + "\" ");
        if (ifxSECURITY != null)
            buf.append("ifxSECURITY=\"" + ifxSECURITY + "\" ");
        if (ifxSQLH_FILE != null)
            buf.append("ifxSQLH_FILE=\"" + ifxSQLH_FILE + "\" ");
        if (ifxSQLH_LOC != null)
            buf.append("ifxSQLH_LOC=\"" + ifxSQLH_LOC + "\" ");
        if (ifxSQLH_TYPE != null)
            buf.append("ifxSQLH_TYPE=\"" + ifxSQLH_TYPE + "\" ");
        if (ifxSSLCONNECTION != null)
            buf.append("ifxSSLCONNECTION=\"" + ifxSSLCONNECTION + "\" ");
        if (ifxSTMT_CACHE != null)
            buf.append("ifxSTMT_CACHE=\"" + ifxSTMT_CACHE + "\" ");
        if (ifxTRACE != null)
            buf.append("ifxTRACE=\"" + ifxTRACE + "\" ");
        if (ifxTRACEFILE != null)
            buf.append("ifxTRACEFILE=\"" + ifxTRACEFILE + "\" ");
        if (ifxTRUSTED_CONTEXT != null)
            buf.append("ifxTRUSTED_CONTEXT=\"" + ifxTRUSTED_CONTEXT + "\" ");
        if (ifxUSEV5SERVER != null)
            buf.append("ifxUSEV5SERVER=\"" + ifxUSEV5SERVER + "\" ");
        if (ifxUSE_DTENV != null)
            buf.append("ifxUSE_DTENV=\"" + ifxUSE_DTENV + "\" ");
        if (super.getLoginTimeout() != null)
            buf.append("loginTimeout=\"" + super.getLoginTimeout() + "\" ");
        if (super.getPassword() != null)
            buf.append("password=\"" + super.getPassword() + "\" ");
        if (super.getPortNumber() != null)
            buf.append("portNumber=\"" + super.getPortNumber() + "\" ");
        if (roleName != null)
            buf.append("roleName=\"" + roleName + "\" ");
        if (super.getServerName() != null)
            buf.append("serverName=\"" + super.getServerName() + "\" ");
        if (super.getUser() != null)
            buf.append("user=\"" + super.getUser() + "\" ");
        buf.append("}");
        return buf.toString();
    }

}