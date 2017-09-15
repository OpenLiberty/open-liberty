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
package com.ibm.websphere.security.wim.ras_test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/*
 * Customer Java Bean
 * 
 */

public class Customer {
	
	private String firtName = null;
	private String lastName = null;
	private String address = null;
	private int pinCode = 0;
	
	private PhoneInfo pi = null;
	
	private Geography geo = null;
	
	protected List<com.ibm.websphere.security.wim.ras_test.BankInfo> bankInfo = null;
	
	private HashMap dataTypeMap = new HashMap();
	
	public HashMap getDataTypeMap() {
		return dataTypeMap;
	}
	public void setDataTypeMap(HashMap dataTypeMap) {
		this.dataTypeMap = dataTypeMap;
	}
	public ArrayList getSuperTypeList() {
		return superTypeList;
	}
	public void setSuperTypeList(ArrayList superTypeList) {
		this.superTypeList = superTypeList;
	}
	public HashSet getSubTypeList() {
		return subTypeList;
	}
	public void setSubTypeList(HashSet subTypeList) {
		this.subTypeList = subTypeList;
	}
	private ArrayList superTypeList = new ArrayList();
    private HashSet subTypeList = new HashSet();

   
	
	
	
	public List<com.ibm.websphere.security.wim.ras_test.BankInfo> getBankInfo() {
		return bankInfo;
	}
	public void setBankInfo(List<com.ibm.websphere.security.wim.ras_test.BankInfo> bankInfo) {
		this.bankInfo = bankInfo;
	}
	public Geography getGeo() {
		return geo;
	}
	public void setGeo(Geography geo) {
		this.geo = geo;
	}
		
	public PhoneInfo getPi() {
		return pi;
	}
	public void setPi(PhoneInfo pi) {
		this.pi = pi;
	}
	public String getFirtName() {
		return firtName;
	}
	public void setFirtName(String firtName) {
		this.firtName = firtName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public int getPinCode() {
		return pinCode;
	}
	public void setPinCode(int pinCode) {
		this.pinCode = pinCode;
	}
	
	

}
