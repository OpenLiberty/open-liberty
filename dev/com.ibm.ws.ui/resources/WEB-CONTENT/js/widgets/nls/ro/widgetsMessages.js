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
    LIBERTY_HEADER_TITLE: "Liberty Admin Center",
    LIBERTY_HEADER_PROFILE: "Preferinţe",
    LIBERTY_HEADER_LOGOUT: "Delogare",
    LIBERTY_HEADER_LOGOUT_USERNAME: "Delogare {0}",
    TOOLBOX_BANNER_LABEL: "Banner {0}",  // TOOLBOX_TITLE
    TOOLBOX_TITLE: "Trusă de unelte",
    TOOLBOX_TITLE_LOADING_TOOL: "Încărcare unealtă...",
    TOOLBOX_TITLE_EDIT: "Editare trusă de unelte",
    TOOLBOX_EDIT: "Editare",
    TOOLBOX_DONE: "Gata",
    TOOLBOX_SEARCH: "Filtrare",
    TOOLBOX_CLEAR_SEARCH: "Curăţare criterii de filtrare",
    TOOLBOX_END_SEARCH: "Oprire filtrare",
    TOOLBOX_ADD_TOOL: "Adăugare unealtă",
    TOOLBOX_ADD_CATALOG_TOOL: "Adăugare unealtă",
    TOOLBOX_ADD_BOOKMARK: "Adăugare semn de carte",
    TOOLBOX_REMOVE_TITLE: "Înlăturare unealtă {0}",
    TOOLBOX_REMOVE_TITLE_NO_TOOL_NAME: "Înlăturare unealtă",
    TOOLBOX_REMOVE_MESSAGE: "Sunteţi sigur că vreţi să înlăturaţi {0}?",
    TOOLBOX_BUTTON_REMOVE: "Înlăturare",
    TOOLBOX_BUTTON_OK: "OK",
    TOOLBOX_BUTTON_GO_TO: "Deplasare la Trusa de unelte",
    TOOLBOX_BUTTON_CANCEL: "Anulare",
    TOOLBOX_BUTTON_BGTASK: "Taskuri de fundal",
    TOOLBOX_BUTTON_BACK: "Înapoi",
    TOOLBOX_BUTTON_USER: "Utilizator",
    TOOLBOX_ADDTOOL_ERROR_MESSAGE: "A apărut o eroare la adăugarea uneltei {0}: {1}",
    TOOLBOX_REMOVETOOL_ERROR_MESSAGE: "A apărut o eroare la înlăturarea uneltei {0}: {1}",
    TOOLBOX_GET_ERROR_MESSAGE: "A apărut o eroare la extragerea uneltelor din trusa de unelte: {0}",
    TOOLCATALOG_TITLE: "Catalog de unelte",
    TOOLCATALOG_ADDTOOL_TITLE: "Adăugare unealtă",
    TOOLCATALOG_ADDTOOL_MESSAGE: "Sunteţi sigur că doriţi să adăugaţi unealta {0} la trusa dumneavoastră de unelte?",
    TOOLCATALOG_BUTTON_ADD: "Adăugare",
    TOOL_FRAME_TITLE: "Cadru unealtă",
    TOOL_DELETE_TITLE: "Ştergere {0}",
    TOOL_ADD_TITLE: "Adăugare {0}",
    TOOL_ADDED_TITLE: "{0} a fost deja adăugat",
    TOOL_LAUNCH_ERROR_MESSAGE_TITLE: "Unealta nu a fost găsită",
    TOOL_LAUNCH_ERROR_MESSAGE: "Unealta solicitată nu s-a lansat pentru că unealta nu este în catalog.",
    LIBERTY_UI_ERROR_MESSAGE_TITLE: "Eroare",
    LIBERTY_UI_WARNING_MESSAGE_TITLE: "Avertisment",
    LIBERTY_UI_INFO_MESSAGE_TITLE: "Informaţii",
    LIBERTY_UI_CATALOG_GET_ERROR: "A apărut o eroare la primirea catalogului: {0}",
    LIBERTY_UI_CATALOG_GET_TOOL_ERROR: "A apărut o eroare la primirea uneltei {0} din catalog: {1}",
    PREFERENCES_TITLE: "Preferinţe",
    PREFERENCES_SECTION_TITLE: "Preferinţe",
    PREFERENCES_ENABLE_BIDI: "Activare suport bidirecţional",
    PREFERENCES_BIDI_TEXTDIR: "Direcţie text",
    PREFERENCES_BIDI_TEXTDIR_LTR: "De la stânga la dreapta",
    PREFERENCES_BIDI_TEXTDIR_RTL: "De la dreapta la stânga",
    PREFERENCES_BIDI_TEXTDIR_CONTEXTUAL: "Contextual",
    PREFERENCES_SET_ERROR_MESSAGE: "A apărut o eroare la setarea preferinţelor de utilizator în trusa de unelte: {0}",
    BGTASKS_PAGE_LABEL: "Taskuri de fundal",
    BGTASKS_DEPLOYMENT_INSTALLATION_POPUP: "Implementare instalare {0}", // {0} is the token number associated with the deployment
    BGTASKS_DEPLOYMENT_INSTALLATION: "Implementare instalare {0} - {1}", // {0} is the token number associated with the deployment, {1} is the server name
    BGTASKS_STATUS_RUNNING: "În rulare",
    BGTASKS_STATUS_FAILED: "Eşuat",
    BGTASKS_STATUS_SUCCEEDED: "Terminat", 
    BGTASKS_STATUS_WARNING: "Reuşit parţial",
    BGTASKS_STATUS_PENDING: "În aşteptare",
    BGTASKS_INFO_DIALOG_TITLE: "Detalii",
    //BGTASKS_INFO_DIALOG_DESC: "Description:",
    BGTASKS_INFO_DIALOG_STDOUT: "Ieşire standard:",
    BGTASKS_INFO_DIALOG_STDERR: "Eroare standard:",
    BGTASKS_INFO_DIALOG_EXCEPTION: "Excepţie:",
    //BGTASKS_INFO_DIALOG_RETURN_CODE: "Return code:",
    BGTASKS_INFO_DIALOG_RESULT: "Rezultat:",
    BGTASKS_INFO_DIALOG_DEPLOYED_ARTIFACT_NAME: "Nume server:",
    BGTASKS_INFO_DIALOG_DEPLOYED_USER_DIR: "Director utilizator:",
    BGTASKS_POPUP_RUNNING_TASK_TITLE: "Taskuri active de fundal",
    BGTASKS_POPUP_RUNNING_TASK_NONE: "Fără",
    BGTASKS_POPUP_RUNNING_TASK_NO_ACTIVE_TASK: "Nu există taskuri active în fundal",
    BGTASKS_DISPLAY_BUTTON: "Detalii şi istoric de taskuri",
    BGTASKS_EXPAND: "Expandare secţiune",
    BGTASKS_COLLAPSE: "Restrângere secţiune",
    PROFILE_MENU_HELP_TITLE: "Ajutor",
    DETAILS_DESCRIPTION: "Descriere",
    DETAILS_OVERVIEW: "Privire generală",
    DETAILS_OTHERVERSIONS: "Alte versiuni",
    DETAILS_VERSION: "Versiune: {0}",
    DETAILS_UPDATED: "Actualizat: {0}",
    DETAILS_NOTOPTIMIZED: "Neoptimizat pentru dispozitivul curent.",
    DETAILS_ADDBUTTON: "Adăugare la Trusa mea de unelte",
    DETAILS_OPEN: "Deschidere",
    DETAILS_CATEGORY: "Categoria {0}",
    DETAILS_ADDCONFIRM: "Unealta {0} a fost adăugată cu succes la trusa de unelte.",
    CONFIRM_DIALOG_HELP: "Ajutor",
    YES_BUTTON_LABEL: "{0} da",  // insert is dialog title
    NO_BUTTON_LABEL: "{0} nu",  // insert is dialog title

    YES: "Da",
    NO: "Nu",

    TOOL_OIDC_ACCESS_DENIED: "Utilizatorul nu este în rolul care are permisiunea de a finaliza această cerere.",
    TOOL_OIDC_GENERIC_ERROR: "A apărut o eroare. Pentru informaţii suplimentare, examinaţi eroarea în istoric.",
    TOOL_DISABLE: "Utilizatorul nu are permisiunea de a utiliza această unealtă. Numai utilizatorii din rolul Administrator au permisiunea de a folosi această unealtă." 
});
