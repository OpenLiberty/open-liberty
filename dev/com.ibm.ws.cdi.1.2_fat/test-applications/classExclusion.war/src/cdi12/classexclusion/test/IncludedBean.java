package cdi12.classexclusion.test;

import javax.enterprise.context.RequestScoped;

import cdi12.classexclusion.test.interfaces.IIncludedBean;

@RequestScoped
public class IncludedBean implements IIncludedBean {

    @Override
    public String getOutput() {
        return "IncludedBean was correctly injected";
    }

}
