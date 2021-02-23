<%--
    Copyright (c) 2014 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html

    Contributors:
        IBM Corporation - initial API and implementation
 --%>
<!DOCTYPE html>
<html lang="en">
	<head>
		<meta charset="utf-8">
		<meta http-equiv="X-UA-Compatible" content="IE=edge">
		<meta name="viewport" content="width=device-width, initial-scale=1">
		<title data-externalizedString="SERVER_CONFIGURATION_EDITOR"></title>
		<link href="lib/bootstrap/css/bootstrap.min.css" rel="stylesheet">
		<link href="lib/orion_editor/code_edit/built-codeEdit.css" rel="stylesheet">
		<link href="css/editor.css" rel="stylesheet">
		<link rel="icon" type="image/png" href="img/server-config-16-D.png">


		<%
			boolean isAdmin = request.isUserInRole("Administrator");
		%>
		<script type="text/javascript">
			globalIsAdmin=<%=isAdmin%>
		</script>

        <%
			// Set security headers	
			response.setHeader("X-XSS-Protection", "1");	
			response.setHeader("X-Content-Type-Options", "nosniff");	
			response.setHeader("X-Frame-Options", "SAMEORIGIN");
		%>
	</head>
	<body>

		<div id="navigationBar" role="navigation" aria-labelledby="navigationBarTitle">
			<span id="navigationBarTitle" tabindex="-1"></span>
			<div id="navigationBarActions">
				<div id="navbarChangeServerSection" class="navbarActionContainer hidden">
					<a href="#" draggable="false" id="navbarChangeServerButton" role="button" class="btn navbar-btn navbar-btn-default" data-externalizedString="CHANGE_SERVER"></a>
				</div>

				<div id="navbarEditorSection" class="navbarActionContainer hidden">
					<div id="navbarEditorReadOnlyMessage" class="hidden" data-externalizedString="READ_ONLY"></div>
					<div id="navbarEditorSavingMessage" class="hidden" data-externalizedString="SAVING"><img src="img/config-tool-home-progress-bgdark-D.gif" draggable="false" alt=""></div>
					<div id="navbarEditorChangesSavedMessage" class="hidden" data-externalizedString="CHANGES_SAVED"></div>
					<a href="#" draggable="false" id="navbarEditorButtonsSave" role="button" class="btn navbar-btn navbar-btn-primary hidden" data-externalizedString="SAVE"></a>
					<a href="#" draggable="false" id="navbarEditorButtonsClose" role="button" class="btn navbar-btn navbar-btn-default" data-externalizedString="CLOSE"></a>
				</div>

				<div id="navbarSignOutSection" class="navbarActionContainer hidden">
					<a href="#" draggable="false" id="navbarSignOutButton" role="button" class="btn navbar-btn navbar-btn-default" data-externalizedString="SIGN_OUT"></a>
				</div>

				<div id="navbarSignOutPromptSection" class="navbarActionContainer hidden">
					<div id="navbarSignOutPromptMessage" data-externalizedString="SIGN_OUT_PROMPT"></div>
					<a href="#" draggable="false" id="navbarSignOutYesButton" role="button" class="btn navbar-btn navbar-btn-default" data-externalizedString="YES"></a>
					<a href="#" draggable="false" id="navbarSignOutNoButton" role="button" class="btn navbar-btn navbar-btn-primary" data-externalizedString="NO"></a>
				</div>
			</div>
		</div>

		<div id="mainContainer" class="container-fluid" role="main"> <!-- main container start -->

			<div id="messageContainer"></div>

			<div id="progress" class="center-block hidden">
		  		<label for="progressBar" data-externalizedString="ONE_MOMENT_PLEASE"></label>
			  	<div class="progress">
					<div id="progressBar" class="progress-bar progress-bar-striped active" role="progressbar" aria-valuenow="100" aria-valuemin="0" aria-valuemax="100" style="width: 100%">
						<span class="sr-only" data-externalizedString="ONE_MOMENT_PLEASE"></span>
					</div>
				</div>
		  	</div>

		  	<div id="login" class="center-block hidden">
				<form id="loginForm">
					<div class="form-group">
					    <label for="loginFormUsername" data-externalizedString="USER_NAME"></label>
					    <input type="text" autocorrect="off" autocapitalize="off" class="form-control" id="loginFormUsername">
					</div>
				  	<div class="form-group">
				    	<label for="loginFormPassword" data-externalizedString="PASSWORD"></label>
				    	<input type="password" autocorrect="off" autocapitalize="off" class="form-control" id="loginFormPassword">
				  	</div>
				  	<button type="submit" id="loginSubmit" class="btn btn-primary" data-externalizedString="SIGN_IN"></button>
					<div id="loginErrorMessages">
						<div id="loginErrorMessagesMissingUserName" class="alert alert-danger hidden" role="alert" data-externalizedString="MISSING_USER_NAME"></div>
						<div id="loginErrorMessagesLoginFailed" class="alert alert-danger hidden" role="alert" data-externalizedString="LOGIN_FAIL"></div>
					</div>
				</form>
			</div>

		  	<div id="serverExplorer" class="hidden">
		  		<div id="serverExplorerTableTitle" tabindex="0">
		  			<div id="serverExplorerTableTitleIcon"></div>
		  			<div id="serverExplorerTableTitleText"></div>
		  			<div id="serverExplorerTableTitleLimit" class="hidden" data-externalizedString="SHOWING_FIRST_N_SERVERS"></div>
		  			<div id="serverExplorerSearchContainer">
		  				<div id="serverExplorerSearchInputContainer">
		  					<label for="serverExplorerSearchInput" class="hidden" data-externalizedString="SEARCH"></label>
							<input id="serverExplorerSearchInput" type="text" data-externalizedPlaceholder="SEARCH">
							<a href="#" id="serverExplorerClearButton" draggable="false" class="hidden" role="button" data-externalizedStringTitle="CLEAR">
								<img data-externalizedStringAlt="CLEAR" src="img/entryfield-clear-D.png">
							</a>
						</div>
						<a href="#" id="serverExplorerSearchButton" class="btn btn-default" role="button" draggable="false" disabled="disabled" tabIndex="-1">
							<span class="sr-only" data-externalizedString="SEARCH"></span>
							<span id="serverExplorerSearchButtonIcon"></span>
						</a>
		  			</div>
		  		</div>
		  		<table id="serverExplorerTable">
		  			<thead id="serverExplorerTableHeader">
		  				<tr class="noSelect">
		  					<th class="columnHeader ascending" tabindex="0" data-key="name" data-externalizedString="SERVER_NAME"></th>
		  					<th class="columnHeader" tabindex="0" data-key="cluster" data-externalizedString="CLUSTER"></th>
		  					<th class="columnHeader" tabindex="0" data-key="host" data-externalizedString="HOST"></th>
		  					<th class="columnHeader" tabindex="0" data-key="wlpUserDir" data-externalizedString="USER_DIRECTORY_PATH"></th>
		  				</tr>
		  			</thead>
		  			<tbody id="serverExplorerTableBody">
		  			</tbody>
		  		</table>
		  		<div id="serverExplorerTableFooterSpace" class="banner-size"></div>
		  		<footer id="serverExplorerTableFooter">
  				</footer>
		  	</div>
		  	<div id="fileExplorer" class="hidden">
		  		<div id="fileExplorerContent" class="list-group">
		  		</div>
		  	</div>

		  	<div id="editor" class="hidden">
		  		<div id="editorContent">
		  			<div id="editorNavigation">
						<a id="editorNavigationDesignLink" class="editorNavigationTab" href="#" draggable="false" data-externalizedString="DESIGN"></a>
						<a id="editorNavigationSourceLink" class="editorNavigationTab" href="#" draggable="false" data-externalizedString="SOURCE"></a>
						<div id="editorNavigationSpacer"></div>
						<div id="contentAssistHint" class="hidden" data-externalizedString="CONTENT_ASSIST_AVAILABLE"></div>
						<a href="#" id="settingsButtonLink" data-externalizedStringTitle="SETTINGS" class="pull-right" role="button" aria-haspopup="true" aria-expanded="false">
							<img data-externalizedStringAlt="SETTINGS" src="img/config-setting-D.png">
						</a>
		  			</div>
		  			<div id="editorDesignView" class="hidden">
		  				<div id="editorDesignViewErrorMessage" class="alert alert-danger hidden" role="alert"></div>
		  				<div id="editorDesignContent">
		  					<div id="editorTree" role="tree" aria-controls="editorForm" aria-labelledby="editorFormLabel"></div>
		  					<div id="editorFormContainer">
		  						<label id="editorFormLabel" data-externalizedString="ELEMENT_INFORMATION_FORM" class="hidden"></label>
			  					<div id="editorForm" role="region" aria-labelledby="editorFormLabel" aria-live="polite"></div>
		  					</div>
		  				</div>
		  			</div>
		  			<div id="editorSourceView" class="hidden">
		  				<div id="orionEditorContainer">
							<div id="orionEditor"></div>
						</div>
		  			</div>
		  		</div>
		  	</div>

		</div> <!-- main container end -->

		<label id="labelDialogAddChildElement" class="hidden" data-externalizedString="ADD_CHILD_ELEMENT_DIALOG"></label>
		<div class="modal" id="dialogAddChildElement" role="dialog" aria-labelledby="labelDialogAddChildElement" tabindex="-1"> <!-- dialog add child element start -->
  			<div class="modal-dialog">
    			<div class="modal-content">
					<div class="modal-body">
						<a class="dialog-close-link" href="#" draggable="false" data-dismiss="modal">
							<span class="sr-only" data-externalizedString="CANCEL"></span>
						</a>
       					<h3 class="modal-title text-center" data-externalizedString="ADD_CHILD"></h3>
						<div id="dialogAddChildElementSearchContainer">
							<label for="dialogAddChildElementSearch" class="hidden" data-externalizedString="ELEMENT_SEARCH"></label>
							<input id="dialogAddChildElementSearch" type="text" class="form-control" data-externalizedPlaceholder="SEARCH">
						</div>
						<div id="dialogAddChildElementListContainer" role="list">
						</div>
						<div id="dialogAddChildElementDescriptionContainer">
							<label for="dialogAddChildElementDescription" class="hidden" data-externalizedString="ELEMENT_DESCRIPTION"></label>
							<textarea id="dialogAddChildElementDescription" class="form-control descriptionTextArea" rows="3" readonly="readonly"></textarea>
						</div>
						<a href="#" draggable="false" id="dialogAddChildElementOKButton" role="button" data-externalizedString="ADD" class="btn btn-primary dialog-btn"></a>
					</div>
				</div>
			</div>
		</div> <!-- dialog add child element end -->


		<label id="labelDialogRemoveElement" class="hidden" data-externalizedString="REMOVE_ELEMENT_DIALOG"></label>
		<div class="modal" id="dialogRemoveElement" role="dialog" aria-labelledby="labelDialogRemoveElement" tabindex="-1"> <!-- dialog remove element start -->
  			<div class="modal-dialog">
    			<div class="modal-content">
					<div class="modal-body">
						<a class="dialog-close-link" href="#" draggable="false" data-dismiss="modal">
							<span class="sr-only" data-externalizedString="CANCEL"></span>
						</a>
						<h3 class="modal-title text-center" data-externalizedString="REMOVE"></h3>
						<div id="dialogRemoveElementMessageContainer">
							<img id="dialogRemoveElementMessageIcon" draggable="false" src="img/alert.png" data-externalizedStringAlt="WARNING">
							<div id="dialogRemoveElementMessageText" data-externalizedString="REMOVE_ELEMENT_CONFIRMATION"></div>
						</div>
						<a href="#" draggable="false" id="dialogRemoveElementOKButton" role="button" data-externalizedString="REMOVE" class="btn dialog-btn dialog-btn-danger"></a>
					</div>
				</div>
			</div>
		</div> <!-- dialog remove element end -->

		<label id="labelDatabaseValidateElement" class="hidden" data-externalizedString="VALIDATE_DATASOURCE_DIALOG"></label>
		<div class="modal" id="dialogDatasourceValidateElement" role="dialog" aria-labelledby="labelDatabaseValidateElement" tabindex="-1"> <!-- dialog validate database element start -->
  			<div class="modal-dialog">
    			<div class="modal-content">
					<div class="modal-body">
						<a class="dialog-close-link" href="#" draggable="false" data-dismiss="modal">
							<span class="sr-only" data-externalizedString="CANCEL"></span>
						</a>
       					<h3 class="modal-title text-center" data-externalizedString="VALIDATE_DATASOURCE"></h3>

       					<div id="authModeTabs">
							<a tabindex="0" draggable="false" href="#" id="useContainerValidationButton" class="tabs active" data-externalizedString="CONTAINER_AUTHENTICATION"></a>
							<a tabindex="0" draggable="false" href="#" id="useApplicationValidationButton" class="tabs" data-externalizedString="APPLICATION_AUTHENTICATION"></a>
						</div>

						<label id="betaLabel" class="hidden">Beta Feature</label> <!-- TODO PII -->
						<form id="testParameters" aria-labelledby="betaLabel">
							<div id="testUserPassParameters" class="form-group hidden">
								<div id="testNoReference" class="form-check">
									<input id="testNoReferenceCheckbox" type="checkbox" class="checkbox-btn" aria-labelledby="betaLabel">
									<label id="labelTestNoReferenceCheckbox" data-externalizedString="NO_RESOURCE_REFERENCE"></label>
								</div>
								<h4 id="testUsernameInputTitle" data-externalizedString="USER_NAME"></h4>
								<input id="testUsernameInput" type="text" class="form-control" data-externalizedPlaceholder="NO_VALUE" aria-labelledby="betaLabel">
								<h4 id="testPasswordInputTitle" data-externalizedString="PASSWORD"></h4>
								<input id="testPasswordInput" type="password" class="form-control" data-externalizedPlaceholder="NO_VALUE" aria-labelledby="betaLabel">
							</div>
							<div id="testAuthAliasParameter" class="form-group">
								<h4 id="authAliasInputTitle" data-externalizedString="AUTH_ALIAS"></h4>
								<input id="authAliasInput" type="text" class="form-control" data-externalizedPlaceholder="NO_VALUE" aria-labelledby="betaLabel">
							</div>
						</form>

						<button draggable="false" id="dialogTestDatabaseButton" data-externalizedString="TEST_CONNECTION" class="btn btn-primary dialog-btn"></button>

						<div id="testEndResult">
							<h4 id="testDatasourceResponseSuccess" class="hidden" data-externalizedString="SUCCESS"></h4>
							<h4 id="testDatasourceResponseFailed" class="hidden" data-externalizedString="FAILED"></h4> <!-- Has different messages -->
						</div>

						<div id="dialogTestDatasourceJSONContainer" class="hidden">
							<h4 id="testDatasourceResponseLabel" class="" data-externalizedString="RESPONSE"></h4>
							<h4 for="dialogTestDatasourceDescription" class="hidden" data-externalizedString="TEST_RESULTS"></h4>
							<textarea aria-labelledby="betaLabel" id="dialogTestDatasourceDescription" data-externalizedPlaceholder="TEST_RESULTS" class="form-control descriptionTextArea" rows="3" readonly="readonly"></textarea>
						</div>
					</div>
				</div>
			</div>
		</div> <!-- dialog validate database element end -->

		<label id="labelDialogEnumerationSelection" class="hidden" data-externalizedString="ENUMERATION_SELECTION_DIALOG"></label>
		<div class="modal" id="dialogEnumerationSelect" role="dialog" aria-labelledby="dialogEnumerationSelectTitle" tabindex="-1"> <!-- dialog enumeration select start -->
  			<div class="modal-dialog">
    			<div class="modal-content">
					<div class="modal-body">
						<a class="dialog-close-link" href="#" draggable="false" data-dismiss="modal">
							<span class="sr-only" data-externalizedString="CANCEL"></span>
						</a>
						<h3 class="modal-title text-center" id="dialogEnumerationSelectTitle">Placeholder</h3>
						<div id="dialogEnumerationSelectListContainer" role="list">
						</div>
						<a href="#" draggable="false" id="dialogEnumerationSelectOKButton" role="button" data-externalizedString="SELECT" class="btn dialog-btn dialog-btn-primary"></a>
					</div>
				</div>
			</div>
		</div> <!-- dialog enumeration select end -->


		<label id="labelDialogSaveBeforeClosing" class="hidden" data-externalizedString="SAVE_BEFORE_CLOSING_DIALOG"></label>
		<div class="modal" id="dialogSaveBeforeClosing" role="dialog" aria-labelledby="labelDialogSaveBeforeClosing" tabindex="-1"> <!-- dialog save before closing start -->
  			<div class="modal-dialog">
    			<div class="modal-content">
					<div class="modal-body">
						<a class="dialog-close-link" href="#" draggable="false" data-dismiss="modal">
							<span class="sr-only" data-externalizedString="CANCEL"></span>
						</a>
						<h3 class="modal-title text-center" data-externalizedString="SAVE_BEFORE_CLOSING_DIALOG_TITLE"></h3>
						<div id="dialogSaveBeforeClosingMessageContainer">
							<img id="dialogSaveBeforeClosingIcon" src="img/alert.png" draggable="false" data-externalizedStringAlt="WARNING">
							<div id="dialogSaveBeforeClosingText"></div>
						</div>
						<div>
							<a href="#" draggable="false" id="dialogSaveBeforeClosingSaveButton" role="button" data-externalizedString="SAVE" class="btn dialog-btn dialog-btn-primary"></a>
							<span id="dialogSaveBeforeClosingButtonSeparator"></span>
							<a href="#" draggable="false" id="dialogSaveBeforeClosingDontSaveButton" role="button" data-externalizedString="DONT_SAVE" class="btn dialog-btn dialog-btn-danger"></a>
						</div>
					</div>
				</div>
			</div>
		</div> <!-- dialog save before closing end -->


		<label id="labelDialogErrorSavingFile" class="hidden" data-externalizedString="ERROR_SAVING_FILE_DIALOG"></label>
		<div id="dialogErrorSavingFile" class="modal" role="dialog" aria-labelledby="labelDialogErrorSavingFile" tabindex="-1"> <!-- dialog error saving file start -->
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-body">
						<h3 class="modal-title text-center" data-externalizedString="ERROR"></h3>
						<div id="dialogErrorSavingFileMessageContainer">
							<img id="dialogErrorSavingFileIcon" src="img/alert.png" draggable="false" data-externalizedStringAlt="WARNING">
							<div id="dialogErrorSavingFileText" data-externalizedString="ERROR_SAVING_FILE_MESSAGE"></div>
						</div>
						<a href="#" draggable="false" id="dialogErrorSavingFileReturnToEditorButton" role="button" class="btn btn-primary dialog-btn" data-externalizedString="RETURN_TO_EDITOR"></a>
					</div>
				</div>
			</div>
		</div> <!-- dialog error saving file end -->


		<label id="labelDialogFileChangedDuringEditing" class="hidden" data-externalizedString="FILE_CHANGED_DURING_EDITING_DIALOG"></label>
		<div class="modal" id="dialogFileChangedDuringEditing" role="dialog" aria-labelledby="labelDialogFileChangedDuringEditing" tabindex="-1"> <!-- dialog file changed during editing start -->
  			<div class="modal-dialog">
    			<div class="modal-content">
					<div class="modal-body">
						<h3 class="modal-title text-center" data-externalizedString="FILE_CHANGED_DURING_EDITING_DIALOG_TITLE"></h3>
						<div id="dialogFileChangedDuringEditingMessageContainer">
							<img id="dialogFileChangedDuringEditingIcon" src="img/alert.png" draggable="false" data-externalizedStringAlt="WARNING">
							<div id="dialogFileChangedDuringEditingText"></div>
						</div>
						<a href="#" draggable="false" id="dialogFileChangedDuringEditingOverwriteButton" role="button" data-externalizedString="OVERWRITE" class="btn dialog-btn dialog-btn-danger"></a>
						<span id="labelDialogFileChangedDuringEditingButtonSeparator"></span>
						<a href="#" draggable="false" id="dialogFileChangedDuringEditingCancelButton" role="button" data-externalizedString="RETURN_TO_EDITOR" class="btn dialog-btn dialog-btn-primary"></a>
					</div>
				</div>
			</div>
		</div> <!-- dialog file changed during editing end -->

		<label id="labelDialogSelectFeature" class="hidden" data-externalizedString="SELECT_FEATURE_DIALOG"></label>
		<div class="modal" id="dialogSelectFeature" role="dialog" aria-labelledby="labelDialogSelectFeature" tabindex="-1"> <!-- dialog select feature start -->
  			<div class="modal-dialog">
    			<div class="modal-content">
					<div class="modal-body">
						<a class="dialog-close-link" href="#" draggable="false" data-dismiss="modal">
							<span class="sr-only" data-externalizedString="CANCEL"></span>
						</a>
       					<h3 class="modal-title text-center" data-externalizedString="SELECT_FEATURE"></h3>
						<div id="dialogSelectFeatureSearchContainer">
							<label for="dialogSelectFeatureSearch" class="hidden" data-externalizedString="ELEMENT_SEARCH"></label>
							<input id="dialogSelectFeatureSearch" type="text" class="form-control" data-externalizedPlaceholder="SEARCH">
						</div>
						<div id="dialogSelectFeatureListContainer" role="list">
						</div>
						<div id="dialogSelectFeatureDescriptionContainer">
							<label for="dialogSelectFeatureDescription" class="hidden" data-externalizedString="FEATURE_DESCRIPTION"></label>
							<textarea id="dialogSelectFeatureDescription" class="form-control descriptionTextArea" rows="3" readonly="readonly"></textarea>
						</div>
						<a href="#" draggable="false" id="dialogSelectFeatureOKButton" role="button" data-externalizedString="SELECT" class="btn btn-primary dialog-btn"></a>
					</div>
				</div>
			</div>
		</div> <!-- dialog select feature end -->

		<div class="hidden"> <!-- Image preload -->
			<img src="img/alert.png" alt="">
			<img src="img/button-search-D.png" alt="">
			<img src="img/button-search-disabled-D.png" alt="">
			<img src="img/close-D.png" alt="">
			<img src="img/close-disabled-D.png" alt="">
			<img src="img/close-hover-D.png" alt="">
			<img src="img/close.png" alt="">
			<img src="img/config-progress-D.gif" alt="">
			<img src="img/config-setting-D.png" alt="">
			<img src="img/config-tool-dialog-progress-bgred-D.gif" alt="">
			<img src="img/config-tool-home-progress-bgblue-D.gif" alt="">
			<img src="img/config-tool-home-progress-bgdark-D.gif" alt="">
			<img src="img/config-tool-home-progress-bgwhite-D.gif" alt="">
			<img src="img/draghandle-normal-DT.png" alt="">
			<img src="img/draghandle-selected-DT.png" alt="">
			<img src="img/entryfield-clear-D.png" alt="">
			<img src="img/folder-DT.png" alt="">
			<img src="img/navigation-collapse-hover-bluebackground-D.png" alt="">
			<img src="img/navigation-collapse-hover-whitebackground-D.png" alt="">
			<img src="img/navigation-collapse-normal-D.png" alt="">
			<img src="img/navigation-collapse-selected-D.png" alt="">
			<img src="img/navigation-expand-hover-bluebackground-D.png" alt="">
			<img src="img/navigation-expand-hover-whitebackground-D.png" alt="">
			<img src="img/navigation-expand-normal-D.png" alt="">
			<img src="img/navigation-expand-selected-D.png" alt="">
			<img src="img/radiobutton-hover-D.png" alt="">
			<img src="img/radiobutton-selected-D.png" alt="">
			<img src="img/radiobutton-selected-hover-D.png" alt="">
			<img src="img/radiobutton-unselected-D.png" alt="">
			<img src="img/readonly-DT.png" alt="">
			<img src="img/server-20-DT.png" alt="">
			<img src="img/sorting-order.png" alt="">
			<img src="img/sorting-reverse-order.png" alt="">
			<img src="img/status-success-16-DT.png" alt="">
			<img src="img/warning-D.png" alt="">
		</div>

		<div id="uiBlock" class="hidden"></div>

    	<script src="lib/jquery.min.js"></script>

    	<script src="lib/bootstrap/js/bootstrap.min.js"></script>
    	<script src="lib/orion_editor/code_edit/built-codeEdit.min.js"></script>

    	<script src="js/prod.min.js"></script>

	</body>

</html>
