package cdi12.classexclusion.test;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Vetoed;

import cdi12.classexclusion.test.interfaces.IVetoedBean;

@RequestScoped
@Vetoed
public class VetoedBean implements IVetoedBean {

    @Override
    public String getOutput() {
        return "VetoedBean was incorrectly injected";
    }

}
