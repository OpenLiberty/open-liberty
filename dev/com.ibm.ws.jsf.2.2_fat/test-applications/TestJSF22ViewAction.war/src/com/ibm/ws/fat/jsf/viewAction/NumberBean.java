package com.ibm.ws.fat.jsf.viewAction;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.application.FacesMessage;
import javax.faces.event.PhaseId;

@ManagedBean
@SessionScoped
public class NumberBean implements Serializable {

    private static final long serialVersionUID = 1L;

    protected Integer number;
    protected boolean invalidNumber = false;
    protected boolean postback;
    private int count = 0;

    public NumberBean() {}

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String checkNumber() {
        if (number.intValue() >= 1 &&
            number.intValue() <= 100) {
            return null;
        }
        invalidNumber = true;
        getFacesContext().addMessage(null,
                                     new FacesMessage("The number you entered is invalid."));
        return "testViewActionNavigation";
    }

    public boolean isInvalidNumber() {
        return invalidNumber;
    }

    public boolean getPostback() {
        postback = getFacesContext().getCurrentInstance().isPostback();
        return postback;
    }

    public int getCount() {
        return count;
    }

    public void incrementCounter() {
        count++;
    }

    public void resetCounter() {
        count = 0;
    }

    public void checkPhase() {

        PhaseId phase = getFacesContext().getCurrentPhaseId();
        //test new getName() method
        String phaseGetName = phase.getName();
        //test new phaseIdValueOf() method
        PhaseId phaseIdValueOf = phase.phaseIdValueOf(phaseGetName);
        
        getFacesContext().addMessage(null,
                                     new FacesMessage("PhaseId.getName(): " + phaseGetName + " PhaseId.phaseIdValueOf(): " + phaseIdValueOf));
    }

    public FacesContext getFacesContext() {
        return FacesContext.getCurrentInstance();
    }
}
