package cdi12.classexclusion.test.packageprotectedbyclass;

import javax.enterprise.context.RequestScoped;

import cdi12.classexclusion.test.interfaces.IProtectedByClassBean;

@RequestScoped
public class ProtectedByClassBean implements IProtectedByClassBean {

    @Override
    public String getOutput() {
        return "ProtectedByClassBean was correctly injected";
    }

}
