package com.ibm.ws.cdi12.test.ejb.timer.view;

public interface EjbSessionBeanLocal {

    String getStack();

    int getSesCount();

    int getReqCount();

    void incCounters();

    void incCountersViaTimer();

    void incRequestCounter();
}
