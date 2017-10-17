package com.ibm.ws.cdi12.test.ejb.timer;

import com.ibm.ws.cdi12.test.ejb.timer.view.EjbSessionBeanLocal;

public class IncrementCountersRunnableTask implements Runnable {
    EjbSessionBeanLocal bean;

    public IncrementCountersRunnableTask(EjbSessionBeanLocal ejbSessionBean) {
        this.bean = ejbSessionBean;
    }

    @Override
    public void run() {
        try {
            System.out.println("Scheduled Task occurred");
            bean.incRequestCounter();
            System.out.println("Poll token by task!");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
