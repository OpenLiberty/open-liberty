package com.ibm.ws.cdi12.test.multipleNamedEJBs;

import javax.ejb.Local;

@Local
public interface SimpleEJBLocalInterface2 {

    public String getData2();

    public void setData2(String data);

}
