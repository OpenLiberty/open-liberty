var messages = {
//General
"DEPLOY_TOOL_TITLE": "Implementieren",
"SEARCH" : "Suchen",
"SEARCH_HOSTS" : "Hosts suchen",
"EXPLORE_TOOL": "TOOL 'UNTERSUCHEN'",
"EXPLORE_TOOL_INSERT": "Probieren Sie das Tool 'Untersuchen' aus",
"EXPLORE_TOOL_ARIA": "Hosts im Tool 'Untersuchen' auf einer neuen Registerkarte suchen",

//Rule Selector Panel
"RULESELECT_EDIT" : "Bearbeiten",
"RULESELECT_CHANGE_SELECTION" : "Auswahl bearbeiten",
"RULESELECT_SERVER_DEFAULT" : "Standardservertypen",
"RULESELECT_SERVER_CUSTOM" : "Angepasste Typen",
"RULESELECT_SERVER_CUSTOM_ARIA" : "Angepasste Servertypen",
"RULESELECT_NEXT" : "Weiter",
"RULESELECT_SERVER_TYPE": "Servertyp",
"RULESELECT_SELECT_ONE": "Eine Regel auswählen",
"RULESELECT_DEPLOY_TYPE" : "Regel implementieren",
"RULESELECT_SERVER_SUBHEADING": "Server",
"RULESELECT_CUSTOM_PACKAGE": "Angepasstes Paket",
"RULESELECT_RULE_DEFAULT" : "Standardregeln",
"RULESELECT_RULE_CUSTOM" : "Angepasste Regeln",
"RULESELECT_FOOTER" : "Wählen Sie einen Servertyp und einen Regeltyp aus, bevor Sie zum Implementierungsformular zurückkehren.",
"RULESELECT_CONFIRM" : "BESTÄTIGEN",
"RULESELECT_CUSTOM_INFO": "Sie können eigene Regeln mit angepassten Eingaben und eigenem Implementierungsverhalten definieren.",
"RULESELECT_CUSTOM_INFO_LINK": "Weitere Informationen",
"RULESELECT_BACK": "Zurück",

//Rule Selector Aria
"RULESELECT_PANEL_ARIA" : "Regelauswahlanzeige {0}", //RULESELECT_OPEN or RULESELECT_CLOSED
"RULESELECT_OPEN" : "Geöffnet",
"RULESELECT_CLOSED" : "Geschlossen",
"RULESELECT_SCROLL_UP": "Zurückblättern",
"RULESELECT_SCROLL_DOWN": "Vorblättern",
"RULESELECT_EDIT_SERVER_ARIA" : "Servertyp bearbeiten, aktuelle Auswahl {0}", // Insert for SERVER TYPES
"RULESELECT_EDIT_RULE_ARIA" : "Regel bearbeiten, aktuelle Auswahl {0}", //Insert for PACKAGE TYPES
"RULESELECT_NEXT_ARIA" : "Nächste Anzeige",

//SERVER TYPES
"LIBERTY_SERVER" : "Liberty-Server",
"NODEJS_SERVER" : "Node.js-Server",

//PACKAGE TYPES
"APPLICATION_PACKAGE" : "Anwendungspaket", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_APPLICATION_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"SERVER_PACKAGE" : "Serverpaket", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_SERVER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"DOCKER_CONTAINER" : "Docker-Container", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_DOCKER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops

//Deployment parameters
"DEPLOYMENT_PARAMETERS": "Implementierungsparameter",
// "PARAMETERS_TOGGLE": "Toggle between upload and use a file located on the collective controller",
"GROUP_DEPLOYMENT_PARAMETERS": "Implementierungsparameter ({0})",
"PARAMETERS_DESCRIPTION": "Die Einzelangaben basieren auf dem ausgewählten Server- und Vorlagentyp.",
"PARAMETERS_TOGGLE_CONTROLLER": "Auf dem Verbundcontroller befindliche Datei verwenden",
"PARAMETERS_TOGGLE_UPLOAD": "Datei hochladen",
"SEARCH_IMAGES": "Images suchen",
"SEARCH_CLUSTERS": "Cluster suchen",
"CLEAR_FIELD_BUTTON_ARIA": "Eingabewert löschen",

// file browser and upload
"SERVER_BROWSE_DESCRIPTION": "Serverpaketdatei hochladen",
"BROWSE_TITLE": "{0} hochladen",
"STRONGLOOP_BROWSE": "Datei hierhin ziehen oder {0}, um den Dateinamen anzugeben.", //BROWSE_INSERT
"BROWSE_INSERT" : "Durchsuchen",
"BROWSE_ARIA": "Dateien anzeigen",
"FILE_UPLOAD_PREVIOUS" : "Auf dem Verbundcontroller befindliche Datei verwenden",
"IS_UPLOADING": "{0} wird hochgeladen...",
"CANCEL" : "Abbrechen",
"UPLOAD_SUCCESSFUL" : "{0} wurde erfolgreich hochgeladen!", // Package Name
"UPLOAD_FAILED" : "Upload fehlgeschlagen",
"RESET" : "Zurücksetzen",
"FILE_UPLOAD_WRITEDIRS_EMPTY_ERROR" : "Die Liste der Verzeichnisse mit Schreibzugriff ist leer.",
"FILE_UPLOAD_WRITEDIRS_PATH_ERROR" : "Der angegebene Pfad muss sich in der Liste der Verzeichnisse mit Schreibzugriff befinden.",
"PARAMETERS_FILE_ARIA" : "Implementierungsparameter oder {0}", // Upload Package name

// docker
"DOCKER_MISSING_REPOSITORY_ERROR": "Sie müssen ein Docker-Repository konfigurieren.",
"DOCKER_EMPTY_IMAGE_ERROR": "Es wurden keine Images im konfigurierten Docker-Repository gefunden.",
"DOCKER_GENERIC_ERROR": "Es wurden keine Docker-Images geladen. Stellen Sie sicher, dass ein konfiguriertes Docker-Repository vorhanden ist.",
"REFRESH": "Aktualisieren",
"REFRESH_ARIA": "Docker-Images aktualisieren",
"PARAMETERS_DOCKER_ARIA": "Implementierungsparameter oder Docker-Images suchen",
"DOCKER_IMAGES_ARIA" : "Liste der Docker-Images",
"LOCAL_IMAGE": "Name des lokalen Image",
"DOCKER_INVALID_CONTAINER__NAME_ERROR" : "Der Containername muss mit dem folgenden Format übereinstimmen: [a-zA-Z0-9][a-zA-Z0-9_.-]*",

//Host selection
"ONE_SELECTED_HOST" : "{0} ausgewählter Host", //quantity
"N_SELECTED_HOSTS": "{0} ausgewählte Hosts", //quantity
"SELECT_HOSTS_MESSAGE": "Wählen Sie aus der Liste der verfügbaren Hosts aus. Sie können Hosts nach Namen oder Tag(s) suchen.",
"ONE_HOST" : "{0} Ergebnis", //quantity
"N_HOSTS": "{0} Ergebnisse", //quantity
"SELECT_HOSTS_FOOTER": "Möchten Sie eine komplexere Suche durchführen? {0}", //EXPLORE_TOOL_INSERT
"NAME": "Name",
"NAME_FILTER": "Hosts nach Namen filtern", // Used for aria-label
"TAG": "Tag",
"TAG_FILTER": "Hosts nach Tag filtern",
"ALL_HOSTS_LIST_ARIA" : "Liste aller Hosts",
"SELECTED_HOSTS_LIST_ARIA": "Liste ausgewählter Hosts",

//Security Details
"SECURITY_DETAILS": "Sicherheitsdetails",
"SECURITY_DETAILS_FOR_GROUP": "Sicherheitsdetails für {0}",  // {0} is the translated group name
"SECURITY_DETAILS_MESSAGE": "Für die Serversicherheit sind weitere Berechtigungsnachweise erforderlich.",
"SECURITY_CREATE_PASSWORD" : "Kennwort erstellen",
"KEYSTORE_PASSWORD_MESSAGE": "Geben Sie ein Kennwort für den Schutz der neu generierten Keystore-Dateien mit den Berechtigungsnachweisen für die Serverauthentifizierung an.",
"PASSWORD_MESSAGE": "Geben Sie ein Kennwort für den Schutz der neu generierten Dateien mit den Berechtigungsnachweisen für die Serverauthentifizierung an.",
"KEYSTORE_PASSWORD": "Keystore-Kennwort",
"CONFIRM_KEYSTORE_PASSWORD": "Keystore-Kennwort bestätigen",
"PASSWORDS_DONT_MATCH": "Die Kennwörter stimmen nicht überein.",
"GROUP_GENERIC_PASSWORD": "{0} ({1})", // {0} is the display label for the initial password field
                                       // {1} is the group name if the password belongs to a group
"CONFIRM_GENERIC_PASSWORD": "{0} bestätigen", // {0} is the display label for the initial password field
"CONFIRM_GROUP_GENERIC_PASSWORD": "{0} ({1}) bestätigen", // {0} is the display label for the initial password field
                                                       // {1} is the group name if the password belongs to a group

//Deploy
"REVIEW_AND_DEPLOY": "Überprüfen und implementieren",
"REVIEW_AND_DEPLOY_MESSAGE" : "Vor der Implementierung müssen alle Felder {0}.", //REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS
"REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS": "ausgefüllt sein",
"READY_FOR_DEPLOYMENT": "bereit für Implementierung.",
"READY_FOR_DEPLOYMENT_CAPS": "Bereit für die Implementierung.",
"READY_TO_DEPLOY": "Das Formular ist ausgefüllt. {0}", //READY_FOR_DEPLOYMENT_CAPS
"READY_TO_DEPLOY_SERVER": "Das Formular ist ausgefüllt. Das Serverpaket ist {0}.", //READY_FOR_DEPLOYMENT
"READY_TO_DEPLOY_DOCKER": "Das Formular ist ausgefüllt. Der Docker-Container ist {0}.", //READY_FOR_DEPLOYMENT
"DEPLOY": "IMPLEMENTIEREN",

"DEPLOY_UPLOADING" : "Warten Sie, bis das Serverpaket vollständig hochgeladen wurde...",
"DEPLOY_FILE_UPLOADING" : "Hochladen von Datei wird beendet...",
"UPLOADING": "Upload wird durchgeführt...",
"DEPLOY_UPLOADING_MESSAGE" : "Schließen Sie dieses Fenster erst, wenn der Implementierungsprozess begonnen hat.",
"DEPLOY_AFTER_UPLOADED_MESSAGE" : "Nachdem der Upload von {0} beendet wurde, können Sie den Fortschritt Ihrer Implementierung hier verfolgen.", // Package name
"DEPLOY_FILE_UPLOAD_PERCENT" : "{0} - {1} % abgeschlossen", // Package name, number
"DEPLOY_WATCH_FOR_UPDATES": "Sie können den Aktualisierungsvorgang hier verfolgen oder Sie schließen dieses Fenster und lassen den Vorgang im Hintergrund laufen! ",
"DEPLOY_CHECK_STATUS": "Sie können den Status Ihrer Implementierung jederzeit prüfen, indem Sie in der oberen rechten Ecke dieser Anzeige auf das Symbol für Hintergrundtasks klicken. ",
"DEPLOY_IN_PROGRESS": "Ihre Implementierung ist in Bearbeitung!",
"DEPLOY_VIEW_BG_TASKS": "Hintergrundtasks anzeigen",
"DEPLOYMENT_PROGRESS": "Implementierungsfortschritt",
"DEPLOYING_IMAGE": "{0} auf {1} Hosts", // Package name (e.g.,  someServerName.zip, aLibertyDockerImage), number of hosts
"VIEW_DEPLOYED_SERVERS": "Erfolgreich implementierte Server anzeigen",
"DEPLOY_PERCENTAGE": "{0} % abgeschlossen", //quantity
"DEPLOYMENT_COMPLETE_TITLE" : "Ihre Implementierung ist abgeschlossen!",
"DEPLOYMENT_COMPLETE_WITH_ERRORS_TITLE" : "Ihre Implementierung ist abgeschlossen, aber es sind einige Fehler aufgetreten.",
"DEPLOYMENT_COMPLETE_MESSAGE" : "Sie können Fehler genauer untersuchen, Ihre neu implementierten Server prüfen oder eine weitere Implementierung starten.",
"DEPLOYING": "Implementierung von...",
"DEPLOYMENT_FAILED": "Ihre Implementierung ist fehlgeschlagen.",
"RETURN_DEPLOY": "Kehren Sie zum Implementierungsformular zurück und übergeben Sie es erneut.",
"REUTRN_DEPLOY_HEADER": "Erneut versuchen",

//Footer
"FOOTER": "Weitere Implementierungen? ",
"FOOTER_BUTTON_MESSAGE" : "Weitere Implementierung starten",

//Error stuff
"ERROR_TITLE": "Zusammenfassung der Fehler",
"ERROR_VIEW_DETAILS" : "Fehlerdetails anzeigen",
"ONE_ERROR_ONE_HOST": "Auf einem Host ist ein Fehler aufgetreten.",
"ONE_ERROR_MULTIPLE_HOST": "Auf mehreren Hosts ist ein Fehler aufgetreten.",
"MULTIPLE_ERROR_ONE_HOST": "Auf einem Host sind mehrere Fehler aufgetreten.",
"MULTIPLE_ERROR_MULTIPLE_HOST": "Auf mehreren Hosts sind mehrere Fehler aufgetreten.",
"INITIALIZATION_ERROR_MESSAGE": "Es kann weder auf den Host zugegriffen noch können Regelinformationen auf dem Server implementiert werden.",
"TRANSLATIONS_ERROR_MESSAGE" : "Kein Zugriff auf externalisierte Zeichenfolgen möglich.",
"MISSING_HOST": "Wählen Sie mindestens einen Host in der Liste aus.",
"INVALID_CHARACTERS" : "Sonderzeichen wie '()$%&' sind im Feld nicht zulässig.",
"INVALID_DOCKER_IMAGE" : "Image wurde nicht gefunden.",
"ERROR_HOSTS" : "{0} und {1} weitere" // if there are more than three hosts reporting error, the message for errors from 6 hosts would be like:
                                      // host1.com, host2.com, host3.com and 3 others
};
