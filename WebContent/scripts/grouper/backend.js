var fullname_cache = {};	// A mapping from aliases to full names

var active_group_id = null;
var group_objects_by_id = {};
var proficiency_labels = {};
var filter_tags = [];

var user_is_admin = false;

var AUTOSAVE_TIMEOUT_MILLISECONDS = 2000;	// milliseconds
//============================================================================
function queueAutoSave(group_id) {
	
	$("#saving_status_indicator").text("Will autosave soon...");
	$("#saving_status_indicator").show();

	var active_group = group_objects_by_id[group_id];
	if (active_group.autosave_timeout != null)
		clearTimeout(active_group.autosave_timeout);
		
	active_group.autosave_timeout = setTimeout("saveGroup(" + group_id + ")", AUTOSAVE_TIMEOUT_MILLISECONDS);
}

//============================================================================
function saveGroup(group_id) {
	var active_group = group_objects_by_id[group_id];
	active_group.autosave_timeout = null;
	active_group.save();
}

//============================================================================
function reloadGroupData(completion_callback, callback_args) {

	console.log("Reloading group data...");

	$("#group_load_hourglass_img").show();

	$.getJSON("load", function(data) {

		$("#status_message").hide();
		$("#group_load_hourglass_img").hide();

		// clear the dictionary first
		group_objects_by_id = {};
		proficiency_labels = {};
		
		if (data.success) {
			
			user_is_admin = data.user_is_admin;
			
			proficiency_labels = data.proficiency_labels;
			
			$.each(data.groups, function(index, group_as_dict) {
				var g = new Group(group_as_dict["label"]);
				g.id = group_as_dict["id"];
				g.is_public = group_as_dict["is_public"];
				g.is_self_serve = group_as_dict["is_self_serve"];
				g.is_skill = group_as_dict["is_skill"];
				g.tags = group_as_dict["tags"];
				g.mine = group_as_dict["mine"];
				g.owner = group_as_dict["owner"];

				var members_as_dicts = group_as_dict["members_as_dicts"];
				$.each(members_as_dicts, function(alias, member_as_dict) {

					var m = new GroupMember(alias);
					m.set_by = member_as_dict["set_by"];
					m.modified = member_as_dict["modified"];
					m.proficiency = member_as_dict["proficiency"];
					g.member_objects_by_alias[alias] = m;
				});

				group_objects_by_id[g.id] = g;
			});

			var unique_unknown_aliases = gatherUnknownAliasesFromGroups();
			fetchUnknownNames(unique_unknown_aliases, completion_callback, callback_args);
			
		} else {
			alert(data.error);
		}
	});
}

// ============================================================================
function transferOwnership(new_owner) {

	var active_group = getActiveGroup();
	if (active_group.dirty) {
		alert("You need to save the group first.");
		return;
	}

	if (confirm("Are you sure you want to make \"" + new_owner + "\" the new maintainer? You won't be able to edit the group after this."))
		$.post("save", {
			action : "bequeath",
			group_id : active_group.id,
			new_owner : new_owner,
		}, function(data) {
	
			if (data.success) {
				active_group.owner = new_owner;
				
				renderGroups();
				showGroup(active_group.id);
			} else {
				alert("Error: " + data.error);
			}
		});

}

//============================================================================
function gatherUnknownAliasesFromGroups() {
	
	var unknown_aliases = {};
	$.each(group_objects_by_id, function(group_id, group_object) {
		
		if (!(group_object.owner in fullname_cache))
			unknown_aliases[group_object.owner] = null;
		
		for ( var alias in group_object.member_objects_by_alias)
			if (!(alias in fullname_cache))
				unknown_aliases[alias] = null;
	});

	var unique_unknown_aliases = [];
	$.each(unknown_aliases, function(key, value) {
		unique_unknown_aliases.push(key);
	});
	
	return unique_unknown_aliases;
}

// ============================================================================
function fetchUnknownNames(unique_unknown_aliases, completion_callback, callback_args) {

	if (unique_unknown_aliases.length) {
		$.getJSON("lookup", {
			aliases : unique_unknown_aliases.join(",")
		}, function(data) {
			$.each(data, function(alias, full_name) {
				fullname_cache[alias] = full_name;
			});

			completion_callback(callback_args);
		});
	} else
		completion_callback(callback_args);
}

// ============================================================================
function addMember(alias, full_name) {

	var active_group = getActiveGroup();
	if (!(alias in active_group.member_objects_by_alias)) {
		fullname_cache[alias] = full_name;
		active_group.member_objects_by_alias[alias] = new GroupMember(alias);
		active_group.markDirty();
	}
}

// ============================================================================
function removeMember(alias) {

	var active_group = getActiveGroup();
	if (alias in active_group.member_objects_by_alias) {
		delete active_group.member_objects_by_alias[alias];
		active_group.markDirty();
	}
}

//============================================================================
function updateRating(dropdown, alias) {

	var active_group = getActiveGroup();
	if (alias in active_group.member_objects_by_alias) {
		
		var prof = parseInt($(dropdown).val());
		active_group.member_objects_by_alias[alias].proficiency = prof;
		active_group.markDirty();
	}
}

//============================================================================
function bulkMemberAdd() {
	
	var members = prompt("Paste a comma-separated list of user aliases below:");
	if (members != null) {
		var array = members.split(",");
		var trimmed_members = array.map(function (val) {
			return $.trim(val);
		});

		$.getJSON("lookup", {
			aliases : trimmed_members.join(",")
		}, function(data) {
			
			console.log("Completed name validation.");
			
			$.each(data, function(alias, full_name) {
				addMember(alias, full_name);
			});

			var unfound_aliases = [];
			$.each(trimmed_members, function(index, alias) {
				if ( !(alias in fullname_cache) ) {
					unfound_aliases.push(alias);
				}
			});

			if (unfound_aliases.length > 0)
				alert("Could not validate these aliases: " + unfound_aliases.join(", "));
		});
	}
}

//============================================================================
function addFilterTag(tag) {

	var idx = filter_tags.indexOf(tag);
	if (idx < 0) {
		filter_tags.push(tag);
		updateGroupFilter();
	}
}

//============================================================================
function removeFilterTag(tag) {

	var idx = filter_tags.indexOf(tag);
	if (idx >= 0) {
		filter_tags.splice(idx, 1);
		updateGroupFilter();
	}
}

// ============================================================================
function addGroupTag(tag) {

	var active_group = getActiveGroup();
	var idx = active_group.tags.indexOf(tag);
	if (idx < 0) {
		active_group.tags.push(tag);
		active_group.markDirty();
	}
}

// ============================================================================
function removeGroupTag(tag) {

	var active_group = getActiveGroup();
	var idx = active_group.tags.indexOf(tag);
	if (idx >= 0) {
		active_group.tags.splice(idx, 1);
		active_group.markDirty();
	}
}

// ============================================================================
function newGroup() {

	active_group_id = -1;

	if (active_group_id in group_objects_by_id) {
		alert("Error: There is already an unsaved pending group!");
		return;
	}

	var newgroup = new Group("New Group");
	group_objects_by_id[active_group_id] = newgroup;

	
//	newgroup.markDirty();	// Don't kick off the autosave immediately
	newgroup.dirty = true;
	showGroup(active_group_id);
	
	$("#group_label_editbox").click();
}

// ============================================================================
function deleteGroup(group_id) {

	var group_object = group_objects_by_id[group_id];
	if (confirm("Really delete \"" + group_object.label + "\" ("
			+ group_object.getMemberCount() + " members)?")) {
		$.post("save", {
			action : "delete",
			group_id : group_id,
		}, function(data) {
			console.log("Deletion success: " + data.success);
			if (data.success) {

				console.log("Successfully deleted.");
				
				active_group_id = null;
				delete group_objects_by_id[group_id];
				renderGroups();

				showGroup(null);
				
				
			} else {
				alert("Error: " + data.message);
			}
		});
	}
}

// ============================================================================
function getActiveGroup() {
	return group_objects_by_id[active_group_id];
}

//============================================================================
//function modifyActiveGroupProperties() {
//
//	var new_group = getActiveGroup();
//	
//	new_group.is_public = $('#is_public').is(':checked');
//	new_group.is_self_serve = $('#is_self_serve').is(':checked');
//	new_group.is_skill = $('#is_skill').is(':checked');
//
//	new_group.markDirty();
//}

// ============================================================================
function saveGroup() {

	// TODO We could check for all dirty groups and implement a "Save All" command
 
	var new_group = getActiveGroup();
	new_group.save();
}