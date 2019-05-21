package com.ibm.ws.fat.jsf.viewAction.phaseListener;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

public class RestoreViewPhaseListener implements PhaseListener {

    private static final long serialVersionUID = 1L;

    private PhaseId phaseId = PhaseId.RESTORE_VIEW;

    public void beforePhase(PhaseEvent event) {
        FacesContext.getCurrentInstance().addMessage(null,
                                                     new FacesMessage("PhaseListener Message: PhaseId.getName(): " + getPhaseId().getName() + " PhaseId.phaseIdValueOf(): "
                                                                      + getPhaseId().phaseIdValueOf("RESTORE_VIEW")));
    }

    public void afterPhase(PhaseEvent event) {}

    public PhaseId getPhaseId() {
        return phaseId;
    }
}