package io.openliberty.http.monitor.fat.jsfApp;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

@RequestScoped
@Named("confirmBean")
public class ConfirmBean implements Serializable {

    public void confirm() {
        System.out.println("CONFIRMED!!!!");
    }
}