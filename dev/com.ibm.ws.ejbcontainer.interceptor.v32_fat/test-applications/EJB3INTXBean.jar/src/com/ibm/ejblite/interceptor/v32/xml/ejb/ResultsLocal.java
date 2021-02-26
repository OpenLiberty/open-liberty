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

/**
 * Local interface for Enterprise Bean: ResultsLocal.
 */
public interface ResultsLocal {
    public void clearLists();

    public void addAroundInvoke(String className, String methodName);

    public ArrayList<String> getAroundInvokeList();

    public void addPostConstruct(String className, String methodName);

    public ArrayList<String> getPostConstructList();

    public void addPostActivate(String className, String methodName);

    public ArrayList<String> getPostActivateList();

    public void addPrePassivate(String className, String methodName);

    public ArrayList<String> getPrePassivateList();

    public void addPreDestroy(String className, String methodName);

    public ArrayList<String> getPreDestroyList();

    public void addMethod(String methodName);

    public ArrayList<String> getMethodList();

    public void addException(String className, String methodName,
                             String exceptionType);

    public ArrayList<String> getExceptionList();

    public void addInterceptorInstanceId(String className, String ivInstanceId);

    public ArrayList<String> getInterceptorInstanceId();

    public String getPostContructContextData();

    public void setPostContructContextData(String data);

    public String getPostActivateContextData();

    public void setPostActivateContextData(String data);

    public String getPrePassivateContextData();

    public void setPrePassivateContextData(String data);

    public String getPreDestroyContextData();

    public void setPreDestroyContextData(String data);

    public String getAroundInvokeContextData();

    public void setAroundInvokeContextData(String data);

    public void addTransactionContext(String class_name, String string,
                                      boolean hasTransactionContext);

    public ArrayList<String> getTransactionContextData();

    public void remove();
}
