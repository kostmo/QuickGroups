function reloadGroupData(completion_callback, callback_args) {
	
	console.log("Reloading group data...");

	$( "#group_load_hourglass_img" ).show();

	$.getJSON("load", function(data) {

		$( "#status_message" ).hide();
		$( "#group_load_hourglass_img" ).hide();

		// clear the dictionary first
		group_objects_by_id = {};

		$.each(data, function(index, group_as_dict) {
			var g = new Group(group_as_dict["label"]);
			g.id = group_as_dict["id"];
			g.is_self_serve = group_as_dict["is_self_serve"];
			g.is_public = group_as_dict["is_public"];
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

		fetchUnknownNames(completion_callback, callback_args);
	});
}

//============================================================================
function fetchUnknownNames(completion_callback, callback_args) {

	var unknown_aliases = {};
	$.each(group_objects_by_id, function(group_id, group_object) {
		for (var alias in group_object.member_objects_by_alias)
			if ( !(alias in fullname_cache) )
				unknown_aliases[alias] = null;
	});
	
	var unique_unknown_aliases = [];
	$.each(unknown_aliases, function(key, value) {
		unique_unknown_aliases.push(key);
	});
	
	if (unique_unknown_aliases.length) {
		$.getJSON("lookup", {aliases: unique_unknown_aliases.join(",")}, function(data) {
			$.each(data, function(alias, full_name) {
				fullname_cache[alias] = full_name;
			});

			completion_callback(callback_args);
		});
	} else
		completion_callback(callback_args);
}

//============================================================================
function addMember(alias, full_name) {
	
	var active_group = getActiveGroup();
	
	if ( !(alias in active_group.member_objects_by_alias) ) {
		fullname_cache[alias] = full_name;
		active_group.member_objects_by_alias[alias] = new GroupMember(alias);
	}
	
	active_group.markDirty();
}

//============================================================================
function removeMember( alias ) {

	console.log("will remove: " + alias );
	var active_group = getActiveGroup();

	if (alias in active_group.member_objects_by_alias) {
		delete active_group.member_objects_by_alias[alias];
	}

	active_group.markDirty();
}

//============================================================================
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

//============================================================================
function deleteGroup(group_id) {
	
	var group_object = group_objects_by_id[group_id];
	if (confirm("Really delete \"" + group_object.label + "\" (" + group_object.getMemberCount() + " members)?")) {
		$.post("save", {
				action: "delete",
				group_id: group_id,
			},
			function(data) {
				console.log("Deletion success: " + data.success);
			}
		);
	}

	active_group_id = null;
	delete group_objects_by_id[group_id];
	renderGroups();
	
	showGroup(null);
}

//============================================================================
function getActiveGroup() {
	return group_objects_by_id[active_group_id];
}

//============================================================================
function saveGroup() {
		
	var new_group = getActiveGroup();
	
	var group_label = $( "#group_label" ).val();
	new_group.label = group_label;
	new_group.is_self_serve = $('#is_self_serve').is(':checked');
	new_group.is_public = $('#is_public').is(':checked');

	console.log("Saving group with label: " + group_label );
	
	var groups = {};
	groups[new_group.label] = new_group.asDictionary();
	
	var jsonString = JSON.stringify(groups);
	$.post("save", {
			action: "insert",
			json: jsonString,
		},
		function(data) {
			
			var group = getActiveGroup();
		
			group.dirty = false;
			
			if (data.created_new_group) {
				
				var old_group_id = group.id;
				group.id = data.new_group_id;
				group_objects_by_id[group.id] = group;
				delete group_objects_by_id[old_group_id];	
			}

			renderGroups();
			showGroup(group.id);

			console.log("Count: " + data.count + "\nSuccess: " + data.success);
		}
	);
}