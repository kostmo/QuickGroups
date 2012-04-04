var fullname_cache = {};	// A mapping from aliases to full names

var active_group_id = null;
var group_objects_by_id = {};

//============================================================================
//Handler for .ready() called.
$(function() {

	showGroup(null);
	
	var first_radio_element = $('input[name="export_radio_group"]')[0];
	$(first_radio_element).attr('checked', true);
	first_radio_element.onchange();
	
	var search_field = $('input:radio[name=search_by_radio_group]:checked').val();
	$( "#namefield" ).autocomplete({
		source: "search?field=" + search_field,
		select: function(event, ui) {
			var alias = ui.item.value;
			var full_name = ui.item.label;
			console.log("Selected item: " + alias);
			addMember(alias, full_name);
		},
		change: function(event, ui) {
			if (ui.item != null) {
				var alias = ui.item.value;
				var full_name = ui.item.label;
				console.log("Changed item: " + alias);
				addMember(alias, full_name);
			} else
				console.log("Changed item, but it was null.");
		},
		search: function(event, ui) {
			$( "#hourglass_img" ).show();
		},
		open: function(event, ui) {
			$( "#hourglass_img" ).hide();
		}
	});
	
	$("#group_label").mouseup(function(e){
        e.preventDefault();
	});
	$("#group_label").focus(function() {
		$(this).select();
	});

	
	reloadGroupData(function(dummy_arg) {
		renderGroups();
	}, 4);
});

//============================================================================
$(window).bind('beforeunload', function(){
	var unsaved_groups_count = 0;
	$.each(group_objects_by_id, function(group_id, group_object) {
		if (group_object.dirty)
			unsaved_groups_count++;
	});
	
	if (unsaved_groups_count)
		return "You have unsaved changes to " + unsaved_groups_count + " group(s).";

	return null;
});

//============================================================================
function toggle_advanced_bulk_options(link_element) {
	
	var bulk_options_section = $("#advanced_bulk_options");
	bulk_options_section.toggle();
	var is_visible = bulk_options_section.is(":visible");
	$(link_element).text( (is_visible ? "Hide Advanced <<": "Show Advanced >>") );
}

//============================================================================
function changeAutocompleteSource(radio_button) {
	var radio_button_value = $(radio_button).val();
	$( "#namefield" ).autocomplete( "option", "source", "search?field=" + radio_button_value );
}

//============================================================================
function renderGroups() {

	var sorted_dictionary_keys = getSortedDictionaryKeys(group_objects_by_id, function(dict, key) {
		return dict[key].label.toLowerCase();
	});

	var group_count = 0;
	var li_elements = [];
	$.each(sorted_dictionary_keys, function(key_index, key_value) {
		var group_object = group_objects_by_id[key_value];
		var group_id = group_object.id;
		li_elements.push( "<li onclick='showGroup(" + group_id + ")'>" + group_object.label + "</li>" );
		group_count++;
	});

	$( "#group_list_header" ).html( "" + group_count + " Group(s)" );	
	$( "#group_list" ).html( li_elements.join("") );
	
	if (!group_count) {
		$( "#instructions_no_groups" ).show();
	} else {
		
		if (active_group_id == null) {
			$( "#instructions_no_groups" ).text("Click on a group name to view or edit.");
			$( "#instructions_no_groups" ).show();
		} else {
			$( "#instructions_no_groups" ).hide();
		}
	}
}

//============================================================================
function getExportDownloadURl(format) {
	return "load?format=" + format + "&action=exportAll";
}

//============================================================================
function exportAllGroups(groups) {
	var radio_value = $('input:radio[name=export_radio_group]:checked').val();
	document.location = getExportDownloadURl(radio_value);
}

//============================================================================
function changeExportType(radio_button) {
	var radio_button_value = $(radio_button).val();
	$( "#query_url" ).attr( "href", getExportDownloadURl(radio_button_value) );
	$( "#query_url" ).text( radio_button_value + " query link");
}



//============================================================================
function changePublicVisibility(checkbox_element) {

	var group_object = getActiveGroup();
	var is_public_checked = $( checkbox_element ).is(':checked');
	group_object.is_public = is_public_checked;
	
	if (!group_object.is_public)
		group_object.is_self_serve = false;
	
	group_object.markDirty();
}

//============================================================================
function changeSelfServe(checkbox_element) {

	var group_object = getActiveGroup();
	var is_self_serve_checked = $( checkbox_element ).is(':checked');
	group_object.is_self_serve = is_self_serve_checked;

	group_object.markDirty();
}

//============================================================================
function changeGroupName(input_textbox) {
	var active_group = getActiveGroup();
	active_group.label = $( input_textbox ).val();
	active_group.markDirty();
}

//============================================================================
function showGroup(group_id) {

	if (group_id == null) {
		$( "#group_info_display" ).hide();
		return;
	} else {
		$( "#group_info_display" ).show();
	}
	
	active_group_id = group_id;
	
	var group_object = getActiveGroup();
	$( "#group_label" ).val(group_object.label);
	$( "#is_public" ).attr('checked', group_object.is_public);
	$( "#is_self_serve" ).attr('checked', group_object.is_self_serve);


	if (group_object.mine || group_object.is_self_serve) {
		
		$(".modifying_actions").removeAttr('disabled');
		
		if (group_object.is_public) {
			$( "#is_self_serve" ).removeAttr('disabled');
		} else {
			// Group is not public, therefore self-serve should be disabled and unchecked.
			$( "#is_self_serve" ).attr("disabled", "disabled");
		}
		
		if (group_object.dirty)
			$( "#save_button" ).removeAttr('disabled');
		else
			$( "#save_button" ).attr("disabled", "disabled");

	} else
		$(".modifying_actions").attr("disabled", "disabled");
	
	
	var sorted_dictionary_keys = getSortedDictionaryKeys(group_object.member_objects_by_alias, function(dict, key) {
		return fullname_cache[key].toLowerCase();
	});
	
	var html_string = "";
	var email_addresses = [];
	$.each(sorted_dictionary_keys, function(key_index, alias) {

		var member_object = group_object.member_objects_by_alias[alias];
		
//		member_object.modified
//		member_object.set_by
		html_string += renderMemberItem(group_object, member_object.alias);
		email_addresses.push(alias + "@" + company_domain);
	});
	
	$( "#member_count" ).html( sorted_dictionary_keys.length + " member(s)." );
	$( "#group_holder" ).html( html_string );

	$( "#group_email_url" ).attr( "href", "mailto:" + email_addresses.join(";"));
}

//============================================================================
function renderMemberItem(group, alias) {
	var remove_command = "";
	
	var is_me = alias == logged_in_username;
	var command_text = "Remove";
	if (is_me)
		command_text = "Remove myself";
	
	if (group.mine || (group.is_self_serve && is_me)) {
		remove_command = " <span style='color: #800000' onclick='removeMember(\"" + alias + "\");'>[" + command_text + "]</span>";
	}
	
	var full_name = fullname_cache[alias];
	return "<li><span style='color: #000080'>" + full_name + "</span> (<span style='color: #008000'>" + alias + "</span>)" + remove_command +"</li>";
}

//============================================================================
function copyGroup(group_id) {

	var current_group = getActiveGroup();

	var old_group_name = current_group.label;
	var new_group_label = prompt("Destination group name", "Copy of " + old_group_name);
	if (new_group_label != null) {

		$.post("save", {
			action: "copy",
			group_id: group_id,
			label: new_group_label,
		},
		function(data) {
			console.log("Success: " + data.success + "; New group id: " + data.new_group_id);
			
			if (data.success) {
				
				// Copy the group clientside as well if the server indicates copy success.
//				var new_group = new Group(new_group_label);
//				new_group.id = data.new_group_id;
//				group_objects_by_id[new_group.id] = new_group;
//				renderGroups();

				// XXX Actually, it's easier just to reload the groups from the server.
				// FIXME Although, we lose any unsaved changes in other groups.
				reloadGroupData(function(new_group_id) {

					renderGroups();
					showGroup(new_group_id);
					
				}, data.new_group_id);
			}
		});
		
		console.log("Started copying...");
	}
}
