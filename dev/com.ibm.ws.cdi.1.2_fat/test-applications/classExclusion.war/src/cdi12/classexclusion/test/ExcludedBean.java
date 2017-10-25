package cdi12.classexclusion.test;

import javax.enterprise.context.RequestScoped;

import cdi12.classexclusion.test.interfaces.IExcludedBean;

@RequestScoped
public class ExcludedBean implements IExcludedBean {

    @Override
    public String getOutput() {
        return "ExcludedBean was incorrectly injected";
    }

}
