package com.ibm.ws.fat.jsf.viewAction.phaseListener;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

public class AnyPhaseListener implements PhaseListener {

    private static final long serialVersionUID = 1L;
    private final PhaseId phaseId = PhaseId.ANY_PHASE;

    @Override
    public void beforePhase(PhaseEvent event) {
        FacesContext.getCurrentInstance().addMessage("form1:testMetadata",
                                                     new FacesMessage("Metadata test: " + event.getPhaseId().getName()));
    }

    @Override
    public void afterPhase(PhaseEvent event) {}

    @Override
    public PhaseId getPhaseId() {
        return phaseId;
    }
}