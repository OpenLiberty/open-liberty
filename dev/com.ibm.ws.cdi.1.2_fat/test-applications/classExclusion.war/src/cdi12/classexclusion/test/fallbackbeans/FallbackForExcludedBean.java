package cdi12.classexclusion.test.fallbackbeans;

import javax.enterprise.context.RequestScoped;

import cdi12.classexclusion.test.interfaces.IExcludedBean;

@RequestScoped
public class FallbackForExcludedBean implements IExcludedBean {

    @Override
    public String getOutput() {
        return "ExcludedBean was correctly rejected";
    }

}
