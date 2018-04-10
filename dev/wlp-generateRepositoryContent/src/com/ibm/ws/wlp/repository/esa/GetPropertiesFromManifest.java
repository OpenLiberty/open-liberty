package com.ibm.ws.wlp.repository.esa;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;

/**
 * This {@link Task} gets the applies to, description, display name and the display name with all white space removed from a feature manifest and stores them in the properties
 * provided. These properties will be left
 * <code>null</code> if they are not set in the manifest.
 */
public class GetPropertiesFromManifest extends Task {

    private String appliesToProperty = "applies.to";
    private String descriptionProperty = "description";
    private String displayNameProperty = "display.name";
    private final String strippedDisplayNameProperty = "stripped.display.name";
    private File featureManifestFile;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tools.ant.Task#execute()
     */
    @Override
    public void execute() throws BuildException {
        try {
            ProvisioningFeatureDefinition feature = new SubsystemFeatureDefinitionImpl(null, this.featureManifestFile);
            setProperty(this.appliesToProperty, feature.getHeader("IBM-AppliesTo"));
            setProperty(this.descriptionProperty, feature.getHeader("Subsystem-Description", Locale.ENGLISH));
            String displayName = feature.getHeader("Subsystem-Name", Locale.ENGLISH);
            setProperty(this.displayNameProperty, displayName);
            setProperty(this.strippedDisplayNameProperty, getStrippedAssetName(displayName));
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Gets the resource name without any word, "." or "-" characters.
     * 
     * @param resource
     * @return
     */
    private String getStrippedAssetName(String name) {
        /**************************************
         * ***************WARNING**************
         * This is duplicated in RepositoryTask
         * ***************WARNING**************
         **************************************/
        name = name.replaceAll("[^\\w.-]", "");
        return name;
    }

    /**
     * @param appliesTo
     */
    private void setProperty(String propertyName, String propertyValue) {
        if (propertyValue != null && propertyName != null && !propertyName.isEmpty()) {
            this.getProject().setProperty(propertyName, propertyValue);
        }
    }

    /**
     * Sets the property name that the applies to should be set to, defaults to <code>applies.to</code>.
     * 
     * @param appliesToProperty the appliesToProperty to set
     */
    public void setAppliesToProperty(String appliesToProperty) {
        this.appliesToProperty = appliesToProperty;
    }

    /**
     * Sets the property name that the description should be set to, defaults to <code>description</code>.
     * 
     * @param descriptionProperty the descriptionProperty to set
     */
    public void setDescriptionProperty(String descriptionProperty) {
        this.descriptionProperty = descriptionProperty;
    }

    /**
     * Sets the property name that the display name should be set to, defaults to <code>display.name</code>.
     * 
     * @param displayNameProperty the displayNameProperty to set
     */
    public void setDisplayNameProperty(String displayNameProperty) {
        this.displayNameProperty = displayNameProperty;
    }

    /**
     * @param featureManifestFile the featureManifestFile to set
     */
    public void setFeatureManifestFile(File featureManifestFile) {
        this.featureManifestFile = featureManifestFile;
    }

}
