package com.ibm.ws.jpa.query.model;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;

public class InvalidQuerySubclass implements Query {

    @Override
    public int executeUpdate() {
        return 0;
    }

    @Override
    public int getFirstResult() {
        return 0;
    }

    @Override
    public FlushModeType getFlushMode() {
        return null;
    }

    @Override
    public Map<String, Object> getHints() {
        return null;
    }

    @Override
    public LockModeType getLockMode() {
        return null;
    }

    @Override
    public int getMaxResults() {
        return 0;
    }

    @Override
    public Parameter<?> getParameter(String arg0) {
        return null;
    }

    @Override
    public Parameter<?> getParameter(int arg0) {
        return null;
    }

    @Override
    public <T> Parameter<T> getParameter(String arg0, Class<T> arg1) {
        return null;
    }

    @Override
    public <T> Parameter<T> getParameter(int arg0, Class<T> arg1) {
        return null;
    }

    @Override
    public <T> T getParameterValue(Parameter<T> arg0) {
        return null;
    }

    @Override
    public Object getParameterValue(String arg0) {
        return null;
    }

    @Override
    public Object getParameterValue(int arg0) {
        return null;
    }

    @Override
    public Set<Parameter<?>> getParameters() {
        return null;
    }

    @Override
    public List getResultList() {
        return null;
    }

    @Override
    public Object getSingleResult() {
        return null;
    }

    @Override
    public boolean isBound(Parameter<?> arg0) {
        return false;
    }

    @Override
    public Query setFirstResult(int arg0) {
        return null;
    }

    @Override
    public Query setFlushMode(FlushModeType arg0) {
        return null;
    }

    @Override
    public Query setHint(String arg0, Object arg1) {
        return null;
    }

    @Override
    public Query setLockMode(LockModeType arg0) {
        return null;
    }

    @Override
    public Query setMaxResults(int arg0) {
        return null;
    }

    @Override
    public <T> Query setParameter(Parameter<T> arg0, T arg1) {
        return null;
    }

    @Override
    public Query setParameter(String arg0, Object arg1) {
        return null;
    }

    @Override
    public Query setParameter(int arg0, Object arg1) {
        return null;
    }

    @Override
    public Query setParameter(Parameter<Calendar> arg0, Calendar arg1,
                              TemporalType arg2) {
        return null;
    }

    @Override
    public Query setParameter(Parameter<Date> arg0, Date arg1, TemporalType arg2) {
        return null;
    }

    @Override
    public Query setParameter(String arg0, Calendar arg1, TemporalType arg2) {
        return null;
    }

    @Override
    public Query setParameter(String arg0, Date arg1, TemporalType arg2) {
        return null;
    }

    @Override
    public Query setParameter(int arg0, Calendar arg1, TemporalType arg2) {
        return null;
    }

    @Override
    public Query setParameter(int arg0, Date arg1, TemporalType arg2) {
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> arg0) {
        return null;
    }

}
