define([
	"dojo/_base/declare", // declare
	"dijit/MenuItem",
	"dojo/text!./templates/MenuItemNoIcon.html"
], function(declare, MenuItem, template){

	var MenuItemNoIcon = declare("jsExplore.widgets.MenuItemNoIcon",
	    [MenuItem], {
		// summary:
		//		A line item in a Menu Widget without the icon <td>

		templateString: template
	});

	return MenuItemNoIcon;
});
