// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package io.openliberty.guides.event.ui;

import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;

import io.openliberty.guides.event.ui.facelets.PageDispatcher;
import io.openliberty.guides.event.ui.util.TimeMapUtil;
import io.openliberty.guides.event.client.EventClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import io.openliberty.guides.event.client.UnknownUrlException;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlInputHidden;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.faces.annotation.ManagedProperty;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Named
@ViewScoped
public class EventBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String location;
    private String day;
    private String month;
    private String year;
    private String hour;
    private int selectedId;
    private boolean notValid;
    private ComponentSystemEvent currentComponent;

    @Inject
    @RestClient
    private EventClient eventClient;

    @Inject
    @ManagedProperty(value = "#{pageDispatcher}")
    private PageDispatcher pageDispatcher;

    public void setName(String name) {
        this.name = name;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getName() {
        return this.name;
    }

    public String getLocation() {
        return this.location;
    }

    public String getHour() {
        return this.hour;
    }

    public String getDay() {
        return this.day;
    }

    public String getMonth() {
        return this.month;
    }

    public String getYear() {
        return this.year;
    }

    public boolean getNotValid() {
        return notValid;
    }

    public PageDispatcher getPageDispatcher() {
        return this.pageDispatcher;
    }

    public void setPageDispatcher(PageDispatcher pageDispatcher) {
        this.pageDispatcher = pageDispatcher;
    }

    public void setCurrentComponent(ComponentSystemEvent component) {
        this.currentComponent = component;
    }

    /**
     * Set a selected event id.
     */
    public void setSelectedId(int selectedId) {
        this.selectedId = selectedId;
    }

    /**
     * Remove stored event id.
     */
    public void removeSelectedId() {
        this.selectedId = -1;
    }


    /**
     * Mapped the time string retrieved from back end service to a user readable
     * format.
     */
    public String showTime(String storedTime) {
        return TimeMapUtil.getMappedDate(storedTime);
    }

    /**
     * Submit event form data to back end service.
     */
    public void submitToService() {
        String time = createStoredTime();
        try {
            eventClient.addEvent(name, time, location);
            pageDispatcher.showMainPage();
            clear();
       } catch (UnknownUrlException e) {
            System.err.println("The given URL is unreachable.");
        } catch (BadRequestException e) {
            displayInvalidEventError();
        }

    }

    /**
     * Submit updated event form data to back end service.
     */
    public void submitUpdateToService() {
        String time = createStoredTime();
        try {
            eventClient.updateEvent(this.name, time, this.location, this.selectedId);
            pageDispatcher.showMainPage();
            clear();
        } catch (UnknownUrlException e) {
            System.err.println("The given URL is unreachable");
        } catch (BadRequestException e) {
            displayInvalidEventError();
        }
    }

    public void editEvent() {
        JsonObject event = retrieveEventByCurrentId(this.selectedId);
        String[] fullDateInfo = parseTime(event.getString("time"));
        this.hour = fullDateInfo[0] + " " + fullDateInfo[1];
        this.month = fullDateInfo[2];
        this.day = fullDateInfo[3];
        this.year = fullDateInfo[4];
        this.name = event.getString("name");
        this.location = event.getString("location");
        this.selectedId = event.getInt("id");

        pageDispatcher.showEditPage();
    }

    /**
     * Delete event form data to back end service.
     */
    public void submitDeletetoService() {
        try {
            eventClient.deleteEvent(this.selectedId);
        } catch (UnknownUrlException e) {
            System.err.println("The given URL is unreachable");
        }

        pageDispatcher.showMainPage();
    }

    /**
     * Retrieve the list of events from back end service.
     */
    public JsonArray retrieveEventList() {
        try {
            return eventClient.getEvents();
        } catch (UnknownUrlException e) {
            System.err.println("The given URL is unreachable.");
            return null;
        }
    }

    /**
     * Retrieve a selected event by Id
     */
    public JsonObject retrieveEventByCurrentId(int currentId) {
        try {
            return eventClient.getEvent(currentId);
        } catch (UnknownUrlException e) {
            System.err.println("The given URL is unreachable");
            return null;
        }
    }

    /**
     * Gets TimeMapUtil map for hours
     */
    public Map<String, Object> getHoursMap() {
        return TimeMapUtil.getHours();
    }

    /**
     * Gets TimeMapUtil map for days
     */
    public Map<String, Object> getDaysMap() {
        return TimeMapUtil.getDays();
    }

    /**
     * Gets TimeMapUtil map for months
     */
    public Map<String, Object> getMonthsMap() {
        return TimeMapUtil.getMonths();
    }

    /**
     * Gets TimeMapUtil map for years
     */
    public Map<String, Object> getYearsMap() {
        return TimeMapUtil.getYears();
    }

    /**
     * Checks the user input for the event time.
     */
    public void checkTime(ComponentSystemEvent event) throws ParseException {
        String hour = getUnitOfTime(event, "eventHour");
        String day = getUnitOfTime(event, "eventDay");
        String month = getUnitOfTime(event, "eventMonth");
        String year = getUnitOfTime(event, "eventYear");

        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a, MMMM d yyyy");
        formatter.setLenient(false);
        Date userDate;

        try {
            userDate = formatter.parse(hour + ", " + month + " " + day + " " + year);
            if (userDate.before(new Date())) {
                allowSubmission(event, false);
                addErrorMessage(event, "Choose a valid time");
                displayError(true);
            } else {
                allowSubmission(event, true);
                displayError(false);
            }
        } catch (Exception e) {
            allowSubmission(event, false);
            addErrorMessage(event, "Choose a valid time");
            displayError(true);
        }
    }

    /**
     * Displays the error message if time is not valid or the event already exists
     */
    public void displayError(boolean display) {
        notValid = display;
    }

    /**
     *  Method to clean the bean after form submission and before event creation form
     */
    public void clear() {
        setName(null);
        setLocation(null);
        setDay(null);
        setMonth(null);
        setYear(null);
        setHour(null);
    }

    /**
     * Helper method to create the time string to be stored at the back end.
     */
    private String createStoredTime() {
        return hour + ", " + month + " " + day + " " + year;
    }

    /**
     * Parses time (in format: hh:mm AM, dd mm yyyy) into time, meridiem, month,
     * day, year respectively.
     */
    private String[] parseTime(String time) {
        String delims = "[ ,]+";
        return time.split(delims);
    }

    /**
     * Adds "Choose a valid time" message after selectOptions in user interface.
     */
    private void addErrorMessage(ComponentSystemEvent event, String errorMessage) {
        FacesContext context = FacesContext.getCurrentInstance();
        FacesMessage message = new FacesMessage(errorMessage);
        HtmlPanelGroup divEventTime = (HtmlPanelGroup) event.getComponent()
            .findComponent("eventform:eventTime");
        context.addMessage(divEventTime.getClientId(context), message);
    }

    /**
     * Gets 'selectOptions' for a specific unit of time from user interface
     */
    private UIInput getUnitOfTimeOptions(ComponentSystemEvent event, String unit) {
        UIComponent components = event.getComponent();
        UIInput unitOptions = (UIInput) components.findComponent("eventform:" + unit);
        return unitOptions;
    }

    /**
     * Gets a unit of time from the submitted values, like hour and year
     */
    private String getUnitOfTime(ComponentSystemEvent event, String unit) {
        UIInput unitOptions = getUnitOfTimeOptions(event, unit);
        return (String) unitOptions.getLocalValue();
    }

    /**
     * Allow if user can submit or not.
     */
    private void allowSubmission(ComponentSystemEvent event, boolean allowSubmission) {
        UIComponent components = event.getComponent();
        HtmlInputHidden formInput = (HtmlInputHidden) components
            .findComponent("eventform:eventSubmit");
        formInput.setValid(allowSubmission);
    }

    /**
     *  Display error message if Event already exists and don't allow form submission
     */
    private void displayInvalidEventError() {
        allowSubmission(currentComponent, false);
        addErrorMessage(currentComponent, "Event already exists!");
        displayError(true);
    }
}
