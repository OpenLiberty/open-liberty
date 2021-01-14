/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 
 define({
    LIBERTY_HEADER_TITLE: "Centro di gestione Liberty",
    LIBERTY_HEADER_PROFILE: "Preferenze",
    LIBERTY_HEADER_LOGOUT: "Logout",
    LIBERTY_HEADER_LOGOUT_USERNAME: "Logout di {0}",
    TOOLBOX_BANNER_LABEL: "Banner {0}",  // TOOLBOX_TITLE
    TOOLBOX_TITLE: "Toolbox",
    TOOLBOX_TITLE_LOADING_TOOL: "Caricamento strumento...",
    TOOLBOX_TITLE_EDIT: "Modifica toolbox",
    TOOLBOX_EDIT: "Modifica",
    TOOLBOX_DONE: "Eseguito",
    TOOLBOX_SEARCH: "Filtra",
    TOOLBOX_CLEAR_SEARCH: "Cancella criteri di filtro",
    TOOLBOX_END_SEARCH: "Modifica filtro",
    TOOLBOX_ADD_TOOL: "Aggiungi strumento",
    TOOLBOX_ADD_CATALOG_TOOL: "Aggiungi strumento",
    TOOLBOX_ADD_BOOKMARK: "Aggiungi segnalibro",
    TOOLBOX_REMOVE_TITLE: "Rimuovi strumento {0}",
    TOOLBOX_REMOVE_TITLE_NO_TOOL_NAME: "Rimuovi strumento",
    TOOLBOX_REMOVE_MESSAGE: "Rimuovere {0}?",
    TOOLBOX_BUTTON_REMOVE: "Rimuovi",
    TOOLBOX_BUTTON_OK: "OK",
    TOOLBOX_BUTTON_GO_TO: "Vai al Toolbox",
    TOOLBOX_BUTTON_CANCEL: "Annulla",
    TOOLBOX_BUTTON_BGTASK: "Attività in background",
    TOOLBOX_BUTTON_BACK: "Indietro",
    TOOLBOX_BUTTON_USER: "Utente",
    TOOLBOX_ADDTOOL_ERROR_MESSAGE: "Si è verificato un errore durante l'aggiunta dello strumento {0}: {1}",
    TOOLBOX_REMOVETOOL_ERROR_MESSAGE: "Si è verificato un errore durante la rimozione dello strumento {0}: {1}",
    TOOLBOX_GET_ERROR_MESSAGE: "Si è verificato un errore durante il richiamo degli strumenti nel toolbox: {0}",
    TOOLCATALOG_TITLE: "Catalogo strumenti",
    TOOLCATALOG_ADDTOOL_TITLE: "Aggiungi strumento",
    TOOLCATALOG_ADDTOOL_MESSAGE: "Aggiungere lo strumento {0} al toolbox?",
    TOOLCATALOG_BUTTON_ADD: "Aggiungi",
    TOOL_FRAME_TITLE: "Frame strumento",
    TOOL_DELETE_TITLE: "Elimina {0}",
    TOOL_ADD_TITLE: "Aggiungi {0}",
    TOOL_ADDED_TITLE: "{0} già aggiunto",
    TOOL_LAUNCH_ERROR_MESSAGE_TITLE: "Strumento non trovato",
    TOOL_LAUNCH_ERROR_MESSAGE: "Non è stato possibile avviare lo strumento richiesto perché non si trova nel catalogo.",
    LIBERTY_UI_ERROR_MESSAGE_TITLE: "Errore",
    LIBERTY_UI_WARNING_MESSAGE_TITLE: "Avvertenza",
    LIBERTY_UI_INFO_MESSAGE_TITLE: "Informazioni",
    LIBERTY_UI_CATALOG_GET_ERROR: "Si è verificato un errore durante il richiamo del catalogo: {0}",
    LIBERTY_UI_CATALOG_GET_TOOL_ERROR: "Si è verificato un errore durante il richiamo dello strumento {0} dal catalogo: {1}",
    PREFERENCES_TITLE: "Preferenze",
    PREFERENCES_SECTION_TITLE: "Preferenze",
    PREFERENCES_ENABLE_BIDI: "Abilita supporto bidirezionale",
    PREFERENCES_BIDI_TEXTDIR: "Direzione testo",
    PREFERENCES_BIDI_TEXTDIR_LTR: "Da sinistra a destra",
    PREFERENCES_BIDI_TEXTDIR_RTL: "Da destra a sinistra",
    PREFERENCES_BIDI_TEXTDIR_CONTEXTUAL: "Contestuale",
    PREFERENCES_SET_ERROR_MESSAGE: "i è verificato un errore durante l'impostazione delle preferenze utente nel toolbox: {0}",
    BGTASKS_PAGE_LABEL: "Attività in background",
    BGTASKS_DEPLOYMENT_INSTALLATION_POPUP: "Distribuisci installazione {0}", // {0} is the token number associated with the deployment
    BGTASKS_DEPLOYMENT_INSTALLATION: "Distribuisci installazione {0} - {1}", // {0} is the token number associated with the deployment, {1} is the server name
    BGTASKS_STATUS_RUNNING: "In esecuzione",
    BGTASKS_STATUS_FAILED: "Non riuscito",
    BGTASKS_STATUS_SUCCEEDED: "Finito", 
    BGTASKS_STATUS_WARNING: "Parzialmente riuscito",
    BGTASKS_STATUS_PENDING: "In sospeso",
    BGTASKS_INFO_DIALOG_TITLE: "Dettagli",
    //BGTASKS_INFO_DIALOG_DESC: "Description:",
    BGTASKS_INFO_DIALOG_STDOUT: "Output standard:",
    BGTASKS_INFO_DIALOG_STDERR: "Errore standard:",
    BGTASKS_INFO_DIALOG_EXCEPTION: "Eccezione:",
    //BGTASKS_INFO_DIALOG_RETURN_CODE: "Return code:",
    BGTASKS_INFO_DIALOG_RESULT: "Risultato:",
    BGTASKS_INFO_DIALOG_DEPLOYED_ARTIFACT_NAME: "Nome server:",
    BGTASKS_INFO_DIALOG_DEPLOYED_USER_DIR: "Directory utente:",
    BGTASKS_POPUP_RUNNING_TASK_TITLE: "Attività di background attive",
    BGTASKS_POPUP_RUNNING_TASK_NONE: "Nessuno",
    BGTASKS_POPUP_RUNNING_TASK_NO_ACTIVE_TASK: "Nessuna attività di background attiva",
    BGTASKS_DISPLAY_BUTTON: "Dettagli e cronologia attività",
    BGTASKS_EXPAND: "Espandi sezione",
    BGTASKS_COLLAPSE: "Comprimi sezione",
    PROFILE_MENU_HELP_TITLE: "Guida",
    DETAILS_DESCRIPTION: "Descrizione",
    DETAILS_OVERVIEW: "Panoramica",
    DETAILS_OTHERVERSIONS: "Altre versioni",
    DETAILS_VERSION: "Versione: {0}",
    DETAILS_UPDATED: "Aggiornata: {0}",
    DETAILS_NOTOPTIMIZED: "Non ottimizzato per il dispositivo corrente:",
    DETAILS_ADDBUTTON: "Aggiungi al Toolbox personale",
    DETAILS_OPEN: "Apri",
    DETAILS_CATEGORY: "Categoria {0}",
    DETAILS_ADDCONFIRM: "Strumento {0} aggiunto correttamente al toolbox.",
    CONFIRM_DIALOG_HELP: "Guida",
    YES_BUTTON_LABEL: "{0} sì",  // insert is dialog title
    NO_BUTTON_LABEL: "{0} no",  // insert is dialog title

    YES: "Sì",
    NO: "No",

    TOOL_OIDC_ACCESS_DENIED: "L'utente non è nel ruolo che dispone dell'autorizzazione per completare questa richiesta.",
    TOOL_OIDC_GENERIC_ERROR: "Si è verificato un errore. Esaminare l'errore nel log per ulteriori informazioni.",
    TOOL_DISABLE: "L'utente non dispone dell'autorizzazione per utilizzare questo strumento. Solo gli utenti con il ruolo di amministratore dispongono dell'autorizzazione per utilizzare questo strumento." 
});
