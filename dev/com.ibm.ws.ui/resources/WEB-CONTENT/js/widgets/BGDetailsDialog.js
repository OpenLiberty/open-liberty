/* jshint strict: false */
define(["dojo/_base/declare",
        "dojo/_base/lang", 
        "dijit/_Widget", 
        "dijit/_TemplatedMixin",
        "dijit/_WidgetsInTemplateMixin",
        "dijit/Dialog",
        "dojo/text!./templates/BGDetailsDialog.html", 
        "dojo/i18n!./nls/widgetsMessages",
        "dojo/on",
        "dojo/domReady!"],
        function(declare,
            lang,
            _Widget,
            _TemplatedMixin,
            _WidgetsInTemplateMixin,
            Dialog,
            template,
            i18n,
            on) {
  // summary:
  //		A modal message dialog to display background task details.
  // description:
  //		Pops up a modal message dialog window.
  // example:
  //	var msgDialog = new BGDetailsDialog({
  //    result: item.result,
  //    stdOut: item.stdOut,
  //    stdErr: item.stdErr,
  //    exception: item.exception,
  //    deployedArtifactName: item.deployedArtifactName,
  //    deployedUserDir: item.deployedUserDir
  //	});
  //	msgDialog.placeAt(win.body());
  //	msgDialog.startup();
  //	msgDialog.show();

  return declare("BGDetailsDialog", [Dialog, _TemplatedMixin, _WidgetsInTemplateMixin], { 
    baseClass: "bgdetailsDialog",
    id: "BGDetailsDialog",

    title : i18n.BGTASKS_INFO_DIALOG_TITLE,
    //detailsLabel:  i18n.BGTASKS_INFO_DIALOG_DESC,
    resultLabel: i18n.BGTASKS_INFO_DIALOG_RESULT,
    stdOutLabel: i18n.BGTASKS_INFO_DIALOG_STDOUT,
    stdErrLabel: i18n.BGTASKS_INFO_DIALOG_STDERR,
    exceptionLabel: i18n.BGTASKS_INFO_DIALOG_EXCEPTION,
    deployedArtifactNameLabel: i18n.BGTASKS_INFO_DIALOG_DEPLOYED_ARTIFACT_NAME,
    deployedUserDirLabel: i18n.BGTASKS_INFO_DIALOG_DEPLOYED_USER_DIR,

    constructor: function(args) {
     
    },

    postMixInProperties:function(){
      this.inherited(arguments);      
    },
    
    // called when the close button is clicked
    hide: function() {
      this.inherited(arguments).then(lang.hitch(this, function(){
        this.destroyRecursive();
      }));
    },

    postCreate: function(){
      this.inherited(arguments);

      var me = this;

      // build the content from the template and inputs
      var contents = new (declare([_Widget, _TemplatedMixin, _WidgetsInTemplateMixin], {
        templateString: template,
        resultLabel: me.resultLabel,
        stdOutLabel: me.stdOutLabel,
        stdErrLabel:  me.stdErrLabel,
        exceptionLabel: me.exceptionLabel,
        deployedArtifactNameLabel: me.deployedArtifactNameLabel,
        deployedUserDirLabel: me.deployedUserDirLabel,
        result:  me.get("result"),
        stdOut:  me.get("stdOut"),
        stdErr:  me.get("stdErr"),
        exception: me.get("exception"),
        deployedArtifactName: me.get("deployedArtifactName"),
        deployedUserDir: me.get("deployedUserDir")
      }))();
      contents.startup();

      this.set("content", contents);
    }

  });
});
