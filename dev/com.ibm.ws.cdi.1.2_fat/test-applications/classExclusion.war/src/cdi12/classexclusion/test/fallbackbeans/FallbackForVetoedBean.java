package cdi12.classexclusion.test.fallbackbeans;

import javax.enterprise.context.RequestScoped;

import cdi12.classexclusion.test.interfaces.IVetoedBean;

@RequestScoped
public class FallbackForVetoedBean implements IVetoedBean {

    @Override
    public String getOutput() {
        return "VetoedBean was correctly rejected";
    }

}
