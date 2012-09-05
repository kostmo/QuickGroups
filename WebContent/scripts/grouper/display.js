//Handler for .ready() called.
$(function() {

	showGroup(null);
	
//	var first_radio_element = $('input[name="export_radio_group"]')[0];
//	$(first_radio_element).attr('checked', true);
//	first_radio_element.onchange();

	
	$('#group_label_editbox').editable(function(value, settings) {
		
		 changeGroupName(value);

	     console.log(value);
	     return value;
	  }, {
	     type    : 'text',
	     submit  : 'OK',
	 });

	$( "#namefield" ).autocomplete({
		source: function(request, response) {
			$( "#hourglass_img" ).show();
			$.getJSON("search", {
				term: request.term,
				field: $('input:radio[name=search_by_radio_group]:checked').val()},
				function(data) {
					$( "#hourglass_img" ).hide();
					response(data);
			});
		},
		autoFocus: true,
		select: function(event, ui) {
			var alias = ui.item.value;
			var full_name = ui.item.label;
//			console.log("Selected item: " + alias);
			addMember(alias, full_name);
		},
		change: function(event, ui) {
			if (ui.item != null) {
				var alias = ui.item.value;
				var full_name = ui.item.label;
//				console.log("Changed item: " + alias);
				addMember(alias, full_name);
			} else {
//				console.log("Changed item, but it was null.");
			}
		},
	});

	$( "#maintainer_field" ).autocomplete({
		source: function(request, response) {
			$( "#maintainer_hourglass_img" ).show();
			$.getJSON("search", {
				term: request.term,
				field: "alias"},
				function(data) {
					$( "#maintainer_hourglass_img" ).hide();
					response(data);
			});
		},
		autoFocus: true,
//		select: function(event, ui) {
//			var alias = ui.item.value;
//			transferOwnership(alias);
//		},
	});

	
	setupTagList("filter_tag_input", "filter_hourglass_img", addFilterTag);
	setupTagList("group_tag_input", "group_tag_hourglass_img", addGroupTag);
	
	
	$("#group_label_editbox").mouseup(function(e){
        e.preventDefault();
	});
	$("#group_label_editbox").focus(function() {
		$(this).select();
	});

	renderGroupFilter();
	
	reloadGroupData(function(dummy_arg) {
		renderGroups();
	}, 4);
});

//============================================================================
function getInterpolatedCssColor(fraction) {
	
	var USED_COLOR_CIRCLE_FRACTION = 1/3;
	var red_green_span = USED_COLOR_CIRCLE_FRACTION * (1 - fraction);
	return Color.hsl(red_green_span, 0.9, 0.7).hexTriplet();
}

//============================================================================
function setupTagList(input_id, throbber_id, tag_adding_function) {
	
	var autocomplete_element = $( "#" + input_id );
	autocomplete_element.autocomplete({
		source: function(request, response) {

			var throbber_element = $( "#" + throbber_id );
			throbber_element.show();
			$.getJSON("tags", {
				term: request.term
			},
			function(data) {
				throbber_element.hide();
				response(data);
			});
		},
		autoFocus: true,
		select: function(event, ui) {
			var tag = ui.item.value;
			tag_adding_function(tag);
		},
	});
	
	
	autocomplete_element.keyup(function(event){
	    if(event.keyCode == 13){
	    	tag_adding_function( $( this ).val() );
	    }
	});
}

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
function toggle_merge_options(link_element) {
	
	var bulk_options_section = $("#merge_options");
	bulk_options_section.toggle();
	var is_visible = bulk_options_section.is(":visible");
	$(link_element).text( (is_visible ? "Hide Merge options <<": "Merge >>") );
}

//============================================================================
function getSortedGroupKeys() {

	var group_sort_value = $('input:radio[name=group_sort_radio_group]:checked').val();

	var alphabetical_sorting_function = function(dict, key) {
		var group_object = dict[key];
		return group_object.label.toLowerCase();
	};
	
	var member_count_sorting_function = function(dict, key) {
		var group_object = dict[key];
		return -group_object.getMemberCount();
	};
	
	var ordered_sorting_functions = [];
	if (group_sort_value == "alphabetical") {
		ordered_sorting_functions.push(alphabetical_sorting_function);
		ordered_sorting_functions.push(member_count_sorting_function);
	} else if (group_sort_value == "member_count") {
		ordered_sorting_functions.push(member_count_sorting_function);
		ordered_sorting_functions.push(alphabetical_sorting_function);
	}
	
	var sorted_dictionary_keys = getSortedDictionaryKeys(group_objects_by_id, ordered_sorting_functions);
	return sorted_dictionary_keys;
}

//============================================================================
function renderGroups() {

	var sorted_dictionary_keys = getSortedGroupKeys();

	var filter_criteria_value = $('#tag_filter_criteria').val();
	
	var shown_groups_member_objects = {};	// To remove duplicates, simulate a Set by using only the keys of a Hash
	var group_count = 0;

	
	
	var filtered_group_objects = [];

	var skills_only = $('#skills_filter_checkbox').is(':checked');
	$.each(sorted_dictionary_keys, function(key_index, key_value) {

		
		var group_object = group_objects_by_id[key_value];
		
		var filter_function = filter_criteria_value == "all" ? Array.prototype.every : Array.prototype.some;
		if (filter_tags.length > 0)
			if (!filter_function.call(filter_tags, function(tag) {return group_object.tags.indexOf(tag) >= 0;}))
				return;

		if (skills_only && !group_object.is_skill)
			return;

		// Gather unique members in the currently shown groups
		$.each(group_object.member_objects_by_alias, function(alias, member_object) {
			shown_groups_member_objects[alias] = member_object;
		});
		
		filtered_group_objects.push(group_object);
		group_count++;
	});
	
	
	var li_elements = [];
	$.each(filtered_group_objects, function(key_index, group_object) {
		var interpolation_fraction = 1.0*key_index/group_count;
		li_elements.push( renderGroupListItem(group_object, interpolation_fraction) );
	});
	
	
	var unique_filtered_member_count = 0;
	var cumulative_member_objects = [];
	$.each(shown_groups_member_objects, function(alias, member_object) {
		cumulative_member_objects.push(member_object);
		unique_filtered_member_count++;
	});

	$( "#filtered_groups_email_url" ).attr("href", getEmailLink(cumulative_member_objects, filter_tags.join(", ")));
	$( "#group_list_header" ).html( "" + group_count + " group(s), " + unique_filtered_member_count + " " + ((unique_filtered_member_count == 1) ? "person" : "people") );	
	$( "#group_list" ).html( li_elements.join("") );
}

//============================================================================
function renderGroupListItem(group_object, interpolation_fraction) {
	var interpolated_color = getInterpolatedCssColor(interpolation_fraction);
//	var style = " style='background-color: " + interpolated_color + ";'";	// TODO Use this rainbow coloring somewhere
	var style = "";
	var status_signifiers = [];
	if (group_object.containsAlias(logged_in_username)) {
		status_signifiers.push("<img style='vertical-align: middle' src='images/star.svg' width='16'/>");
	}
	return "<li title='id: " + group_object.id + "' " + style + " onclick='showGroup(" + group_object.id + ")'>" + group_object.label + " <b>(" + group_object.getMemberCount() + ")</b>" + status_signifiers.join(" ") + "</li>";
}

//============================================================================
function getExportDownloadURL(format) {
	return "load?format=" + format + "&action=exportAll";
}

//============================================================================
function exportAllGroups(groups) {
	var radio_value = $('input:radio[name=export_radio_group]:checked').val();
	document.location = getExportDownloadURL(radio_value);
}

//============================================================================
function changeExportType(radio_button) {
	var radio_button_value = $(radio_button).val();
	$( "#query_url" ).attr( "href", getExportDownloadURL(radio_button_value) );
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
function changeIsSkill(checkbox_element) {

	var group_object = getActiveGroup();
	var is_skill_checked = $( checkbox_element ).is(':checked');
	group_object.is_skill = is_skill_checked;
	group_object.markDirty();
}

//============================================================================
function changeGroupName(new_group_name) {
	var active_group = getActiveGroup();
	active_group.label = new_group_name;
	active_group.markDirty();
}

//============================================================================
function toggleSkillsFilter() {
	renderGroups();
}

//============================================================================
function renderGroupFilter() {
	var html_contents = "&lt;<i>no tags selected</i>&gt;";
	if (filter_tags.length) {
		html_contents = filter_tags.map(renderTagItem, {editable: true, remove_function_name: "removeFilterTag"}).join(", ");
	}
	$( "#filter_tags_list" ).html(html_contents);
}

//============================================================================
function updateGroupFilter() {
	renderGroupFilter();
	renderGroups();
}

//============================================================================
function getSortedGroupMemberKeys(group_object) {

	var member_sort_value = $('input:radio[name=members_sort_radio_group]:checked').val();
	
	var alphabetical_sorting_function = function(dict, key) {
		if (key in fullname_cache)
			return fullname_cache[key].toLowerCase();
		else
			return "";
	};
	
	var proficiency_sorting_function = function(dict, key) {
		var member_object = dict[key];
		return -member_object.proficiency;
	};
	
	var ordered_sorting_functions = [];
	if (!group_object.is_skill || member_sort_value == "alphabetical") {
		ordered_sorting_functions.push(alphabetical_sorting_function);
		ordered_sorting_functions.push(proficiency_sorting_function);
	} else if (member_sort_value == "proficiency") {
		ordered_sorting_functions.push(proficiency_sorting_function);
		ordered_sorting_functions.push(alphabetical_sorting_function);
	}
	
	var sorted_dictionary_keys = getSortedDictionaryKeys(group_object.member_objects_by_alias, ordered_sorting_functions);
	return sorted_dictionary_keys;
}

//============================================================================
function renderSingleGroupTagsList(group_object) {

	var html_contents = "&lt;<i>no tags selected</i>&gt;";
	if (group_object.tags.length) {
		html_contents = group_object.tags.map(renderTagItem, {editable: group_object.mine, remove_function_name: "removeGroupTag"}).join(", ");
	}
	$( "#group_tags_list" ).html(html_contents);
}

//============================================================================
function showGroup(group_id) {

	if (group_id == null) {
		$( ".group_info_display" ).hide();
		return;
	} else {
		$( ".group_info_display" ).show();
	}

	active_group_id = group_id;
	
	var group_object = getActiveGroup();
	$( "#group_label_editbox" ).text(group_object.label);
	$( "#is_public" ).attr('checked', group_object.is_public);
	$( "#is_self_serve" ).attr('checked', group_object.is_self_serve);
	$( "#is_skill" ).attr('checked', group_object.is_skill);

	if (group_object.is_skill) {
		$( "#member_sort_criteria_wiget" ).show();
	} else { 
		$( "#member_sort_criteria_wiget" ).hide();
	}
	
	// TODO Maintain a list of all fields that should be only be
	// enabled when you are the group owner
	if (group_object.mine || user_is_admin) {
		$( "#maintainer_selector_area" ).show();
		$( "#group_tags_selector_area" ).show();
	} else {
		$( "#maintainer_selector_area" ).hide();
		$( "#group_tags_selector_area" ).hide();
	}
	
	
	renderSingleGroupTagsList(group_object);

	if (group_object.mine || group_object.is_self_serve || user_is_admin) {
		
		$(".modifying_actions").removeAttr('disabled');
		$( "#add_member_input_area" ).show();
		
		if (group_object.is_public) {
			$( "#is_self_serve" ).removeAttr('disabled');
		} else {
			// Group is not public, therefore self-serve should be disabled and unchecked.
			$( "#is_self_serve" ).attr("disabled", "disabled");
		}
		
		if (group_object.dirty) {
			$( "#save_button" ).removeAttr('disabled');
			$( "#save_button" ).addClass( "dirty_save_button" );
		} else {
			$( "#save_button" ).attr("disabled", "disabled");
			$( "#save_button" ).removeClass( "dirty_save_button" );
		}

	} else {
		$(".modifying_actions").attr("disabled", "disabled");
		$( "#add_member_input_area" ).hide();
	}

	var sorted_dictionary_keys = getSortedGroupMemberKeys(group_object);

	var html_string = "";
	var member_object_list = [];
	$.each(sorted_dictionary_keys, function(index, alias) {

		var member_object = group_object.member_objects_by_alias[alias];
		member_object_list.push(member_object);
		
		html_string += renderMemberItem(group_object, member_object);
	});
	
	$( "#member_count" ).html( sorted_dictionary_keys.length + " member(s)." );
	$( "#members_holder" ).html( html_string );
	
	$( "#current_group_maintainer" ).text( fullname_cache[group_object.owner] );

	$( "#group_email_url" ).attr("href", getEmailLink(member_object_list, group_object.label));
}

//============================================================================
function getEmailLink(member_objects, subject) {
	
	var email_addresses = [];
	$.each(member_objects, function(index, member_object) {
		email_addresses.push(member_object.getEmailAddress());
	});
	
	var subject_extension = "";
	if (subject != null)
		subject_extension = "?Subject=" + subject;
	
	return "mailto:" + email_addresses.join(";") + subject_extension;
}

//============================================================================
function renderTagItem(tag) {
	var remove_command = "";

	if (this.editable)
		remove_command = " <span class='remove_member' onclick='" + this.remove_function_name + "(\"" + tag + "\");'><img style='vertical-align: top' src='images/tiny_trashcan.png' title='Remove' alt='trash can'></span>";

	// chiclet style
	return "<span class='group_tag'>" + tag + remove_command + "</span>";
}

//============================================================================
function renderMemberItem(group, member_object) {
	var command_set = [];
	
	var email_icon = "<a href='mailto:" + member_object.getEmailAddress() + "'><img style='vertical-align: middle' src='images/tiny_envelope.png' title='Email' alt='envelope'></a>";
	
	var alias = member_object.alias;
	
	var is_me = alias == logged_in_username;
	var command_text = "Remove";
	if (is_me)
		command_text = "Remove myself";
	
	if (group.mine || (group.is_self_serve && is_me) || user_is_admin) {
		command_set.push("<span class='remove_member' onclick='removeMember(\"" + alias + "\");'><img style='vertical-align: middle' src='images/tiny_trashcan.png' title='" + command_text + "' alt='trash can'></span>");
	}
	
	if (group.is_skill) {
		
		var skill_display = "";
		if (group.mine || (group.is_self_serve && is_me)) {
			
			skill_display += "<select title='Proficiency' onchange='updateRating(this, \"" + member_object.alias + "\")'>";
			$.each(proficiency_labels, function(rating, text_fields) {
				skill_display += "<option value='" + rating + "'" + (rating == member_object.proficiency ? " selected='selected'" : "") + ">" + text_fields["name"] + "</option>";
			});
			skill_display += "</select>";

		} else {
			// chiclet style
			var proficiency_fraction = 1.0 * member_object.proficiency / Object.keys(proficiency_labels).length;
			var css_color = Color.hsl(0, 0, 0.8).hexTriplet();
			if (member_object.proficiency)
				css_color = getInterpolatedCssColor(proficiency_fraction);
			skill_display += "<span title='Proficiency' class='proficiency_chiclet' style='background-color: " + css_color + "'>" + proficiency_labels[member_object.proficiency]["name"] + "</span>";
		}
		
		command_set.push(skill_display);
	}

	// TODO
//	member_object.modified
//	member_object.set_by
	
	var full_name = fullname_cache[alias];
	return "<li>" + email_icon + " <span style='font-weight: bold' title='" + alias + "'>" + full_name + "</span> " + command_set.join(" ") +"</li>";
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
//			console.log("Success: " + data.success + "; New group id: " + data.new_group_id);
			
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
