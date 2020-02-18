define([ 'dojo/Deferred', 'js/common/platform', 'js/widgets/YesNoDialog', 'dojo/_base/lang', 'dojo/i18n!jsExplore/nls/explorerMessages', 
         'dojo/dom-construct', 'dojo/dom', 'jsShared/utils/imgUtils', 'jsExplore/resources/utils' ],
         function(Deferred, platform, YesNoDialog, lang, i18n, domConstruct, dom, imgUtils, utils) {  
  return {
    waitIfDirtyEditor : __waitIfDirtyEditor
  };

  function __waitIfDirtyEditor() { 
    var deferredDirtyConfigDialog = new Deferred();
    var configIframe = dom.byId('exploreContainerForConfigTool');
    if (configIframe) {
      var configWin = configIframe.contentWindow;
      if (configWin && configWin.editor && configWin.editor.isDocumentDirty && configWin.editor.isDocumentDirty() && !dom.byId('dirtyConfigDialog')) {
        var alertIcon = imgUtils.getSVGSmallName('status-alert-gray');
        
        // We could get the filename from the hash, but this is how serverConfig obtains it.  We should obtain the name 
        // in the same manner as serverConfig, but we should do it through an 'API' similar to isDocumentDirty()
        var filename = configWin.$("#navigationBarTitle").text();
        var widgetId = "dirtyConfigDialog";
        utils.destroyWidgetIfExists(widgetId);
        var dirtyConfigDialog = new YesNoDialog({
          id : widgetId,
          title : i18n.CLOSE,
          message : '',
          description : lang.replace(i18n.SAVE_BEFORE_CLOSING_DIALOG_MESSAGE, [filename]),
          descriptionIcon : alertIcon,
          destructiveAction : "no",
          yesButtonText: i18n.SAVE,
          noButtonText: i18n.DONT_SAVE,
          yesFunction : function() {
            configWin.editor.save();
            deferredDirtyConfigDialog.resolve();
          },
          noFunction : function() {
            // Destroy the view so we don't get prompted about dirty config again
            domConstruct.destroy(configIframe);
            deferredDirtyConfigDialog.resolve();
          }
        });
        dirtyConfigDialog.placeAt(document.body);
        dirtyConfigDialog.startup();
        dirtyConfigDialog.show();
        // The below would be if instead we wanted to use the server config dialog instead of our own
//      configWin.$("#dialogSaveBeforeClosing").modal("show");
//      var dialog = configIframe.contentdom.byId('dialogSaveBeforeClosing');
      } else {
        // Config wasn't dirty so resolve the deferred and move on
        deferredDirtyConfigDialog.resolve();
      }
    } else {
      // No config iframe so resolve the deferred and move on
      deferredDirtyConfigDialog.resolve();
    }
    return deferredDirtyConfigDialog;
  }
});