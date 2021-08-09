/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// PM10631     03/24/2010  sartoris    Performance improvement to reuse ExternalContext object instead of creating a new one on every call
// PI60837     04/14/2016  hwibell     StackOverflowError will occur when JSF composite components are deployed on server with com.ibm.ws.el.reuseEvaluationContext set to true

package org.apache.el.lang;

import java.util.Locale;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.VariableMapper;

public final class EvaluationContext extends ELContext {

    private ELContext elContext; //PM10631 - removed "final"

    private FunctionMapper fnMapper; //PM10631 - removed "final"

    private VariableMapper varMapper; //PM10631 - removed "final"

    public EvaluationContext(ELContext elContext, FunctionMapper fnMapper,
                             VariableMapper varMapper) {
        this.elContext = elContext;
        this.fnMapper = fnMapper;
        this.varMapper = varMapper;
    }

    public ELContext getELContext() {
        return this.elContext;
    }

    @Override
    public FunctionMapper getFunctionMapper() {
        return this.fnMapper;
    }

    @Override
    public VariableMapper getVariableMapper() {
        return this.varMapper;
    }

    @Override
    // Can't use Class<?> because API needs to match specification in superclass
    public Object getContext(@SuppressWarnings("rawtypes") Class key) {
        return this.elContext.getContext(key);
    }

    @Override
    public ELResolver getELResolver() {
        return this.elContext.getELResolver();
    }

    @Override
    public boolean isPropertyResolved() {
        return this.elContext.isPropertyResolved();
    }

    @Override
    // Can't use Class<?> because API needs to match specification in superclass
    public void putContext(@SuppressWarnings("rawtypes") Class key,
                           Object contextObject) {
        this.elContext.putContext(key, contextObject);
    }

    @Override
    public void setPropertyResolved(boolean resolved) {
        this.elContext.setPropertyResolved(resolved);
    }

    @Override
    public Locale getLocale() {
        return this.elContext.getLocale();
    }

    @Override
    public void setLocale(Locale locale) {
        this.elContext.setLocale(locale);
    }

    //PM10631 start
    public void setELContext(ELContext elc) {
        // PI60837: Cannot set elContext to the object as this will cause recursive methods to recurse infinitely!
        if (elc != this)
            this.elContext = elc;
    }

    public void setFunctionMapper(FunctionMapper fm) {
        this.fnMapper = fm;
    }

    public void setVariableMapper(VariableMapper vm) {
        this.varMapper = vm;
    }
    //PM10631 end
}
