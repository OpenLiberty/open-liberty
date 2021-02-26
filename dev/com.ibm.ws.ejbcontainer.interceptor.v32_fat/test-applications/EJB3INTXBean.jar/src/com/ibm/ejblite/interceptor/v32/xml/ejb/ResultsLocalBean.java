/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejblite.interceptor.v32.xml.ejb;

import java.util.ArrayList;
import java.util.HashMap;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Bean implementation class for Enterprise Bean: ResultsLocal
 */
public class ResultsLocalBean implements ResultsLocal {
    /**
     * Private class used to implement persistent store for Results.
     */
    private class ResultsData implements Cloneable {
        /**
         * Ordered list of String objects that are used to record the order that
         * interceptor methods are invoked.
         */
        ArrayList<String> ivAroundInvokeList = new ArrayList<String>();
        ArrayList<String> ivPostConstructList = new ArrayList<String>();
        ArrayList<String> ivPostActivateList = new ArrayList<String>();
        ArrayList<String> ivPrePassivateList = new ArrayList<String>();
        ArrayList<String> ivPreDestroyList = new ArrayList<String>();
        ArrayList<String> ivMethodList = new ArrayList<String>();
        ArrayList<String> ivExceptionList = new ArrayList<String>();
        ArrayList<String> ivInstanceId = new ArrayList<String>();
        ArrayList<String> ivTransactionContextList = new ArrayList<String>();
        String ivPostConstructContextData;
        String ivPostActivateContextData;
        String ivPrePassivateContextData;
        String ivPreDestroyContextData;
        String ivAroundInvokeContextData;

        @Override
        protected Object clone() throws CloneNotSupportedException {
            ResultsData clone = (ResultsData) super.clone();
            clone.ivAroundInvokeList = (ArrayList<String>) ivAroundInvokeList.clone();
            clone.ivPostConstructList = (ArrayList<String>) ivPostConstructList.clone();
            clone.ivPostActivateList = (ArrayList<String>) ivPostActivateList.clone();
            clone.ivPrePassivateList = (ArrayList<String>) ivPrePassivateList.clone();
            clone.ivPreDestroyList = (ArrayList<String>) ivPreDestroyList.clone();
            clone.ivMethodList = (ArrayList<String>) ivMethodList.clone();
            clone.ivExceptionList = (ArrayList<String>) ivExceptionList.clone();
            clone.ivInstanceId = (ArrayList<String>) ivInstanceId.clone();
            clone.ivTransactionContextList = (ArrayList<String>) ivTransactionContextList.clone();
            return clone;
        }
    }

    /**
     * Static HashMap to find the ResultsData object for a given Results primary key.
     */
    private final static HashMap<String, ResultsData> cvResultsMap = new HashMap<String, ResultsData>();

    /**
     * mutex for accesing cvResultsMap
     */
    private final static Object cvResultsMapLock = new Object();

    /**
     * A static stateful bean that implements ResultsLocal
     */
    public static ResultsLocal SFBean;

    /**
     * ejbLoad sets this instance variable with a clone of ResultsData found in
     * cvResultsMap and ejbStore updates cvResultsMap with updated clone object.
     */
    private ResultsData ivResultsData = null;

    /**
     * This is the key into cvResultsMap that this Results instance needs to use when
     * accessing cvResultsMap.
     */
    private String ivResultsMapKey = null;

    // --------------------------------------------------
    // ResultsLocal interface methods
    // --------------------------------------------------

    @Override
    public void addAroundInvoke(String className, String methodName) {
        ivResultsData.ivAroundInvokeList.add(className + "." + methodName);
    }

    @Override
    public ArrayList<String> getAroundInvokeList() {
        return ivResultsData.ivAroundInvokeList;
    }

    @Override
    public void addPostConstruct(String className, String methodName) {
        ivResultsData.ivPostConstructList.add(className + "." + methodName);
    }

    @Override
    public ArrayList<String> getPostConstructList() {
        return ivResultsData.ivPostConstructList;
    }

    @Override
    public void addPostActivate(String className, String methodName) {
        ivResultsData.ivPostActivateList.add(className + "." + methodName);
    }

    @Override
    public ArrayList<String> getPostActivateList() {
        return ivResultsData.ivPostActivateList;
    }

    @Override
    public void addPrePassivate(String className, String methodName) {
        ivResultsData.ivPrePassivateList.add(className + "." + methodName);
    }

    @Override
    public ArrayList<String> getPrePassivateList() {
        return ivResultsData.ivPrePassivateList;
    }

    @Override
    public void addPreDestroy(String className, String methodName) {
        ivResultsData.ivPreDestroyList.add(className + "." + methodName);
    }

    @Override
    public ArrayList<String> getPreDestroyList() {
        return ivResultsData.ivPreDestroyList;
    }

    @Override
    public void addMethod(String methodName) {
        ivResultsData.ivMethodList.add(methodName);
    }

    @Override
    public ArrayList<String> getMethodList() {
        return ivResultsData.ivMethodList;
    }

    @Override
    public void addException(String className, String methodName,
                             String exceptionType) {
        ivResultsData.ivExceptionList.add(className + "." + methodName + "."
                                          + exceptionType);
    }

    @Override
    public ArrayList<String> getExceptionList() {
        return ivResultsData.ivExceptionList;
    }

    @Override
    public void addInterceptorInstanceId(String className, String id) {
        ivResultsData.ivInstanceId.add(className + "." + id);
    }

    @Override
    public ArrayList<String> getInterceptorInstanceId() {
        return ivResultsData.ivInstanceId;
    }

    public void ejbCreate() {
        ResultsKey resultsKey = new ResultsKey("ResultsLocal");
        ivResultsMapKey = resultsKey.pKey;
        clearLists();
    }

    @Override
    public void clearLists() {
        ivResultsData = new ResultsData();
        synchronized (cvResultsMapLock) {
            cvResultsMap.put(ivResultsMapKey, ivResultsData);
        }
    }

    // -------------------------------------------------
    // StetefulBean life cycle methods
    // --------------------------------------------------

    public void ejbActivate() {
        // System.outprintln("ResultsLocalBean.ejbActivate");
        synchronized (cvResultsMapLock) {
            ivResultsData = cvResultsMap.get(ivResultsMapKey);
        }
    }

    public void ejbPassivate() {
        // System.outprintln("ResultsLocalBean.ejbPassivate");
        ivResultsMapKey = null;
        ivResultsData = null;
    }

    @Override
    public void remove() {
        // System.outprintln("ResultsLocalBean.ejbRemove called for key: " +
        // ivResultsMapKey);
        synchronized (cvResultsMapLock) {
            cvResultsMap.remove(ivResultsMapKey);
            ivResultsData = null;
        }
    }

    @Override
    public String getPostContructContextData() {
        return ivResultsData.ivPostConstructContextData;
    }

    @Override
    public void setPostContructContextData(String data) {
        ivResultsData.ivPostConstructContextData = data;
    }

    @Override
    public String getPostActivateContextData() {
        return ivResultsData.ivPostActivateContextData;
    }

    @Override
    public void setPostActivateContextData(String data) {
        ivResultsData.ivPostActivateContextData = data;
    }

    @Override
    public String getPrePassivateContextData() {
        return ivResultsData.ivPrePassivateContextData;
    }

    @Override
    public void setPrePassivateContextData(String data) {
        ivResultsData.ivPrePassivateContextData = data;
    }

    @Override
    public String getPreDestroyContextData() {
        return ivResultsData.ivPreDestroyContextData;
    }

    @Override
    public void setPreDestroyContextData(String data) {
        ivResultsData.ivPreDestroyContextData = data;
    }

    @Override
    public String getAroundInvokeContextData() {
        return ivResultsData.ivAroundInvokeContextData;
    }

    @Override
    public void setAroundInvokeContextData(String data) {
        ivResultsData.ivAroundInvokeContextData = data;
    }

    @Override
    public void addTransactionContext(String className, String methodName,
                                      boolean hasTransactionContext) {
        ivResultsData.ivTransactionContextList.add(className + "." + methodName
                                                   + ":" + hasTransactionContext);
    }

    @Override
    public ArrayList<String> getTransactionContextData() {
        return ivResultsData.ivTransactionContextList;
    }

    public static ResultsLocal setupSFBean() throws NamingException {
        InitialContext ictx = new InitialContext();
        String beanName = "java:app/EJB3INTXBean/ResultsLocalBean!com.ibm.ejblite.interceptor.v32.xml.ejb.ResultsLocal";
        SFBean = (ResultsLocal) ictx.lookup(beanName);
        return SFBean;
    }

    /**
     * Returns a static stateful bean that can be used for testing
     *
     * @return a bean implementing ResultsLocal
     * @throws NamingException
     */
    public static ResultsLocal getSFBean() {
        return SFBean;
    }
}
