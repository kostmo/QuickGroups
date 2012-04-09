var fullname_cache = {};	// A mapping from aliases to full names

var active_group_id = null;
var group_objects_by_id = {};
var filter_tags = [];

//============================================================================
function reloadGroupData(completion_callback, callback_args) {

	console.log("Reloading group data...");

	$("#group_load_hourglass_img").show();

	$.getJSON("load", function(data) {

		$("#status_message").hide();
		$("#group_load_hourglass_img").hide();

		// clear the dictionary first
		group_objects_by_id = {};

		if (data.success) {
			$.each(data.groups, function(index, group_as_dict) {
				var g = new Group(group_as_dict["label"]);
				g.id = group_as_dict["id"];
				g.is_self_serve = group_as_dict["is_self_serve"];
				g.is_public = group_as_dict["is_public"];
				g.tags = group_as_dict["tags"];
				g.mine = group_as_dict["mine"];

				var members_as_dicts = group_as_dict["members_as_dicts"];
				$.each(members_as_dicts, function(alias, member_as_dict) {

					var m = new GroupMember(alias);
					m.set_by = member_as_dict["set_by"];
					m.modified = member_as_dict["modified"];
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

	console.log("Making new group...");

	active_group_id = -1;

	if (active_group_id in group_objects_by_id) {
		alert("Error: There is already an unsaved pending group!");
		return;
	}

	var newgroup = new Group("New Group");
	group_objects_by_id[active_group_id] = newgroup;

	newgroup.markDirty();

	$("#group_label").focus();
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
		});
	}

	active_group_id = null;
	delete group_objects_by_id[group_id];
	renderGroups();

	showGroup(null);
}

// ============================================================================
function getActiveGroup() {
	return group_objects_by_id[active_group_id];
}

// ============================================================================
function saveGroup() {

	var new_group = getActiveGroup();

	var group_label = $("#group_label").val();
	new_group.label = group_label;
	new_group.is_self_serve = $('#is_self_serve').is(':checked');
	new_group.is_public = $('#is_public').is(':checked');

	// TODO We could check for all dirty groups and implement a "Save All"
	// command
	var groups = {};
	groups[new_group.label] = new_group.asDictionary();

	var action_type = new_group.id < 0 ? "insert" : "modify";
	var jsonString = JSON.stringify(groups);
	$.post("save", {
		action : action_type,
		json : jsonString,
	}, function(data) {

		new_group.dirty = false;

		if (data.created_new_group) {

			var old_group_id = new_group.id;
			new_group.id = data.new_group_id;
			group_objects_by_id[new_group.id] = new_group;
			new_group.mine = true;
			delete group_objects_by_id[old_group_id];
		}

		renderGroups();
		showGroup(new_group.id);
	});
}