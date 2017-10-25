package cdi12.classexclusion.test;

import javax.enterprise.context.RequestScoped;

import cdi12.classexclusion.test.interfaces.IProtectedByHalfComboBean;

@RequestScoped
public class ProtectedByHalfComboBean implements IProtectedByHalfComboBean {

    @Override
    public String getOutput() {
        return "ProtectedByHalfComboBean was correctly injected";
    }

}
