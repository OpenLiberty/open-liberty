package com.ibm.ws.cdi12.test.multipleNamedEJBs;

import javax.ejb.Local;

@Local
public interface SimpleEJBLocalInterface1 {

    public String getData1();

    public void setData1(String data);

}
