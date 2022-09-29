package com.ibm.ws.remoteEJB.shared;

import javax.transaction.SystemException;

public interface TestBeanRemote {
    public String getProperty(String var);

    public String mandatory() throws SystemException;

    public String never() throws SystemException;

    public String notSupported() throws SystemException;

    public String required() throws SystemException;

    public String requiresNew() throws SystemException;

    public String supports() throws SystemException;
}