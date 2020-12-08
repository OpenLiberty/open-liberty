/* jshint strict: false */
define([
	"dojo/_base/declare", // declare
    "dojo/i18n", // i18n.getLocalization
	"dijit/Tooltip",
    "dojo/query",
    "js/common/platform",
    "dojo/dom",
    "dojo/dom-class",
    "dojo/text!./templates/ValidationTextBox.html",
    "dojo/i18n!./nls/validate" // leaving this in on purpose
], function(declare, i18n, Tooltip, query, platform, dom, domClass, template){

	// module:
	//		dijit/form/ValidationTextBox


	/*=====
	var __Constraints = {
		// locale: String
		//		locale used for validation, picks up value from this widget's lang attribute
		// _flags_: anything
		//		various flags passed to pattern function
	};
	=====*/

	return declare("js.widgets._ValidationTextBoxMixin", null, {
		// summary:
		//		Base class for textbox widgets with the ability to validate content of various types and provide user feedback.
        templateString: template,

		// required: Boolean
		//		User is required to enter data into this field.
		required: false,

		// promptMessage: String
		//		If defined, display this hint string immediately on focus to the textbox, if empty.
		//		Also displays if the textbox value is Incomplete (not yet valid but will be with additional input).
		//		Think of this like a tooltip that tells the user what to do, not an error message
		//		that tells the user what they've done wrong.
		//
		//		Message disappears when user starts typing.
		promptMessage: "",

		// invalidMessage: String
		//		The message to display if value is invalid.
		//		The translated string value is read from the message file by default.
		//		Set to "" to use the promptMessage instead.
		invalidMessage: "$_unset_$",

		// missingMessage: String
		//		The message to display if value is empty and the field is required.
		//		The translated string value is read from the message file by default.
		//		Set to "" to use the invalidMessage instead.
		missingMessage: "$_unset_$",

		// message: String
		//		Currently error/prompt message.
		//		When using the default tooltip implementation, this will only be
		//		displayed when the field is focused.
		message: "",

		// constraints: __Constraints
		//		user-defined object needed to pass parameters to the validator functions
		constraints: {},

		// pattern: [extension protected] String|Function(constraints) returning a string.
		//		This defines the regular expression used to validate the input.
		//		Do not add leading ^ or $ characters since the widget adds these.
		//		A function may be used to generate a valid pattern when dependent on constraints or other runtime factors.
		//		set('pattern', String|Function).
		pattern: ".*",

		// regExp: Deprecated [extension protected] String.  Use "pattern" instead.
		regExp: "",

		regExpGen: function(/*__Constraints*/ /*===== constraints =====*/){
			// summary:
			//		Deprecated.  Use set('pattern', Function) instead.
		},

		// state: [readonly] String
		//		Shows current state (ie, validation result) of input (""=Normal, Incomplete, or Error)
		state: "",

		// tooltipPosition: String[]
		//		See description of `dijit/Tooltip.defaultPosition` for details on this parameter.
		tooltipPosition: [],
		
		// incomplete: String
		// control which tooltip css class to use: bookmarkTooltipError or bookmarkTooltipIncomplete 
		incomplete: "",
		
		_setValueAttr: function(){
			// summary:
			//		Hook so set('value', ...) works.
			this.inherited(arguments);
			this._refreshState();
		},

		validator: function(/*anything*/ value, /*__Constraints*/ constraints){
			// summary:
			//		Overridable function used to validate the text input against the regular expression.
			// tags:
			//		protected
			return (new RegExp("^(?:" + this._computeRegexp(constraints) + ")"+(this.required?"":"?")+"$")).test(value) &&
				(!this.required || !this._isEmpty(value)) &&
				(this._isEmpty(value) || this.parse(value, constraints) !== undefined); // Boolean
		},

		_isValidSubset: function(){
			// summary:
			//		Returns true if the value is either already valid or could be made valid by appending characters.
			//		This is used for validation while the user [may be] still typing.
			return this.textbox.value.search(this._partialre) === 0;
		},

		isValid: function(/*Boolean*/ /*===== isFocused =====*/){
			// summary:
			//		Tests if value is valid.
			//		Can override with your own routine in a subclass.
			// tags:
			//		protected
			return this.validator(this.textbox.value, this.get('constraints'));
		},

		_isEmpty: function(value){
			// summary:
			//		Checks for whitespace
			return (this.trim ? /^\s*$/ : /^$/).test(value); // Boolean
		},

		getErrorMessage: function(/*Boolean*/ /*===== isFocused =====*/){
			// summary:
			//		Return an error message to show if appropriate
			// tags:
			//		protected
			var invalid = this.invalidMessage === "$_unset_$" ? this.messages.invalidMessage :
				!this.invalidMessage ? this.promptMessage : this.invalidMessage;
			var missing = this.missingMessage === "$_unset_$" ? this.messages.missingMessage :
				!this.missingMessage ? invalid : this.missingMessage;
      console.log("missing: " + missing + ", invalid: " + invalid);
      this.incomplete = (this.required && this._isEmpty(this.textbox.value)) ? "true" : "false";
			return (this.required && this._isEmpty(this.textbox.value)) ? missing : invalid; // String
		},

		getPromptMessage: function(/*Boolean*/ /*===== isFocused =====*/){
			// summary:
			//		Return a hint message to show when widget is first focused
			// tags:
			//		protected
      this.incomplete = "true";
			return this.promptMessage; // String
		},

		_maskValidSubsetError: true,
		validate: function(/*Boolean*/ isFocused){
			// summary:
			//		Called by oninit, onblur, and onkeypress.
			// description:
			//		Show missing or invalid messages if appropriate, and highlight textbox field.
			// tags:
			//		protected
			var message = "";
			var isValid = this.disabled || this.isValid(isFocused);
			if(isValid){ this._maskValidSubsetError = true; }
			var isEmpty = this._isEmpty(this.textbox.value);
			var isValidSubset = !isValid && isFocused && this._isValidSubset();
			this._set("state", isValid ? "" : (((((!this._hasBeenBlurred || isFocused) && isEmpty) || isValidSubset) && (this._maskValidSubsetError || (isValidSubset && !this._hasBeenBlurred && isFocused))) ? "Incomplete" : "Error"));
			this.focusNode.setAttribute("aria-invalid", isValid ? "false" : "true");
			
			if(this.state === "Error"){
				this._maskValidSubsetError = isFocused && isValidSubset; // we want the error to show up after a blur and refocus
				message = this.getErrorMessage(isFocused);
            console.log("message: ", message);
			}else if(this.state === "Incomplete"){
				message = this.getPromptMessage(isFocused); // show the prompt whenever the value is not yet complete
				this._maskValidSubsetError = !this._hasBeenBlurred || isFocused; // no Incomplete warnings while focused
			}else if(isEmpty){
				message = this.getPromptMessage(isFocused); // show the prompt whenever there's no error and no text
			}

			this.set("message", message);

			return isValid;
		},

		displayMessage: function(/*String*/ message){
			// summary:
			//		Overridable method to display validation errors/hints.
			//		By default uses a tooltip.
			// tags:
      //		extension	 
      console.log("message: " + message + " --- focused: " + this.focused + " ---- state: " + this.state);
			var tooltipConnector, tooltipContainer;
			var tooltipDirection = "";
      if(message && this.focused){
          console.log("this.domNode: ", this.domNode);
          var errorMessageDivNode = dom.byId(this.id + "ErrorMessageDiv");
          if ( platform.isPhone() &&  errorMessageDivNode) {
              errorMessageDivNode.innerHTML = message;
              errorMessageDivNode.setAttribute("dir", this.textDir);
              if (this.incomplete === "false") {
                  domClass.add(errorMessageDivNode, "warningMessageBookmarkName");
                  domClass.add(this.domNode, "bookmarkBorderError");
                  console.log("bookmarkBorderError");
                  }
              else {
                  domClass.add(errorMessageDivNode, "errorMessageBookmarkName");
                  domClass.add(this.domNode, "bookmarkBorderIncomplete");
                  console.log("bookmarkBorderIncomplete");
                  }
              console.log("this.id=" + this.id);
          } else {
              Tooltip.show(message, this.domNode, this.tooltipPosition, !this.isLeftToRight());

              query(".dijitTooltipConnector").forEach(function(node) {
                  tooltipConnector = node;
              });
              query(".dijitTooltipRight").forEach(function(node) {
                  tooltipDirection = "right";
              });
              if (tooltipDirection === "") {
                  query(".dijitTooltipLeft").forEach(function(node) {
                      tooltipDirection = "left";
                  });
              }
              query(".dijitTooltipContainer").forEach(function(node) {
                  tooltipContainer = node;
              });

              console.log("tooltipconnector: ", tooltipConnector);
              console.log("tooltip container: ", tooltipContainer);
              console.log("tooltipDirection: ", tooltipDirection);
              if (this.incomplete === "false") {
                  console.log("add error tooltip css");
                  domClass.remove(tooltipContainer, "bookmarkTooltipIncomplete");
                  domClass.add(tooltipContainer, "bookmarkTooltipError");
                  if (tooltipDirection === "right") {
                      domClass.remove(tooltipConnector, "bookmarkTooltipConnectorRightIncomplete");
                      domClass.add(tooltipConnector, "bookmarkTooltipConnectorRightError");
                  } else if (tooltipDirection === "left") {
                      domClass.remove(tooltipConnector, "bookmarkTooltipConnectorLeftIncomplete");
                      domClass.add(tooltipConnector, "bookmarkTooltipConnectorLeftError");
                  }     
                  domClass.remove(this.domNode, "bookmarkBorderIncomplete");
                  domClass.add(this.domNode, "bookmarkBorderError");
              } else {               
                  console.log("add incomplete tooltip css");
                  domClass.remove(tooltipContainer, "bookmarkTooltipError");
                  domClass.add(tooltipContainer, "bookmarkTooltipIncomplete");
                  if (tooltipDirection === "right") {
                    domClass.remove(tooltipConnector, "bookmarkTooltipConnectorRightError");
                      domClass.add(tooltipConnector, "bookmarkTooltipConnectorRightIncomplete");
                  } else if (tooltipDirection === "left") {
                    domClass.remove(tooltipConnector, "bookmarkTooltipConnectorLeftError");
                      domClass.add(tooltipConnector, "bookmarkTooltipConnectorLeftIncomplete");
                  }   
                  domClass.remove(this.domNode, "bookmarkBorderError");
                  domClass.add(this.domNode, "bookmarkBorderIncomplete");
              }
          }
          

          }else{
              var enode = dom.byId(this.id + "ErrorMessageDiv");
              if ( platform.isPhone() &&  enode) {
                  enode.innerHTML = "";
                  domClass.remove(enode, "warningMessageBookmarkName");
                  domClass.remove(enode, "errorMessageBookmarkName");
                  domClass.remove(this.domNode, "bookmarkBorderError");
                  domClass.remove(this.domNode, "bookmarkBorderIncomplete");
              }
              else {
          /* if (this.state === "") { */ 
              console.log("hiding tooltip");
              Tooltip.hide(this.domNode);
              domClass.remove(this.domNode, "bookmarkBorderError");
              domClass.remove(this.domNode, "bookmarkBorderIncomplete");
              
           /* } else {
              console.log("not hiding");
          } */
			}
				}
		},

		_refreshState: function(){
			// Overrides TextBox._refreshState()
			if(this._created){ // should instead be this._started but that would require all programmatic ValidationTextBox instantiations to call startup()
				this.validate(this.focused);
			}
			this.inherited(arguments);
		},

		//////////// INITIALIZATION METHODS ///////////////////////////////////////

		constructor: function(params /*===== , srcNodeRef =====*/){
			// summary:
			//		Create the widget.
			// params: Object|null
			//		Hash of initialization parameters for widget, including scalar values (like title, duration etc.)
			//		and functions, typically callbacks like onClick.
			//		The hash can contain any of the widget's properties, excluding read-only properties.
			// srcNodeRef: DOMNode|String?
			//		If a srcNodeRef (DOM node) is specified, replace srcNodeRef with my generated DOM tree.

			this.constraints = {};
			this.baseClass += ' dijitValidationTextBox';
		},

		startup: function(){
			this.inherited(arguments);
			this._refreshState(); // after all _set* methods have run
		},

		_setConstraintsAttr: function(/*__Constraints*/ constraints){
			if(!constraints.locale && this.lang){
				constraints.locale = this.lang;
			}
			this._set("constraints", constraints);
			this._refreshState();
		},

		_setPatternAttr: function(/*String|Function*/ pattern){
			this._set("pattern", pattern); // don't set on INPUT to avoid native HTML5 validation
		},

		_computeRegexp: function(/*__Constraints*/ constraints){
			// summary:
			//		Hook to get the current regExp and to compute the partial validation RE.

			var p = this.pattern;
			if(typeof p === "function"){
				p = p.call(this, constraints);
			}
			if(p !== this._lastRegExp){
				var partialre = "";
				this._lastRegExp = p;
				// parse the regexp and produce a new regexp that matches valid subsets
				// if the regexp is .* then there's no use in matching subsets since everything is valid
				if(p !== ".*"){
					p.replace(/\\.|\[\]|\[.*?[^\\]{1}\]|\{.*?\}|\(\?[=:!]|./g,
					function(re){
						switch(re.charAt(0)){
							case '{':
							case '+':
							case '?':
							case '*':
							case '^':
							case '$':
							case '|':
							case '(':
								partialre += re;
								break;
							case ")":
								partialre += "|$)";
								break;
							default:
								partialre += "(?:"+re+"|$)";
								break;
						}
					});
				}
				try{ // this is needed for now since the above regexp parsing needs more test verification
					"".search(partialre);
				}catch(e){ // should never be here unless the original RE is bad or the parsing is bad
					partialre = this.pattern;
					console.warn('RegExp error in ' + this.declaredClass + ': ' + this.pattern);
				} // should never be here unless the original RE is bad or the parsing is bad
				this._partialre = "^(?:" + partialre + ")$";
			}
			return p;
		},

        postMixInProperties: function(){
            this.inherited(arguments);
            this.messages = i18n;
            this._setConstraintsAttr(this.constraints); // this needs to happen now (and later) due to codependency on _set*Attr calls attachPoints
        },

		_setDisabledAttr: function(/*Boolean*/ value){
			this.inherited(arguments);	// call FormValueWidget._setDisabledAttr()
			this._refreshState();
		},

		_setRequiredAttr: function(/*Boolean*/ value){
			this._set("required", value);
			this.focusNode.setAttribute("aria-required", value);
			this._refreshState();
		},

		_setMessageAttr: function(/*String*/ message){
			this._set("message", message);
			this.displayMessage(message);
		},

		reset:function(){
			// Overrides dijit/form/TextBox.reset() by also
			// hiding errors about partial matches
			this._maskValidSubsetError = true;
			this.inherited(arguments);
		},

		_onBlur: function(){
			// the message still exists but for back-compat, and to erase the tooltip
			// (if the message is being displayed as a tooltip), call displayMessage('')
			this.displayMessage('');

			this.inherited(arguments);
		}
	});
});
