function Group(label) {
	this.label = label;
	this.id = -1;	// -1 indicates a group that has not been persisted to the database, yet.
	this.dirty = false;
	this.member_objects_by_alias = {};
	this.tags = [];
	this.mine = true;
	this.owner = logged_in_username;
	
	this.is_public = true;
	this.is_self_serve = true;	// By default, newly created groups will be self-serve
	this.is_skill = false;
	
	this.autosave_timeout = null;
	this.is_save_in_progress = false;
	
	this.containsAlias = function(alias) {
		return alias in this.member_objects_by_alias;
	};
	
	this.removeAlias = function(alias) {
		var idx = this.aliases.indexOf(alias);
		if (idx != -1) {
			this.aliases.splice(idx, 1);
			return true;
		}
		
		return false;
	};
	
	this.asDictionary = function() {
		
		var members_as_dicts = {};
		$.each(this.member_objects_by_alias, function(alias, member_object) {
			members_as_dicts[alias] = member_object.asDictionary();
		});
		
		var dict = {
				"label": this.label,
				"is_public": this.is_public,
				"is_self_serve": this.is_self_serve,
				"is_skill": this.is_skill,
				"id": this.id,
				"tags": this.tags,
				"members_as_dicts": members_as_dicts,
		};
		return dict;
	};
	
	this.getMemberCount = function() {
		var count = 0;
		$.each(this.member_objects_by_alias, function(index, group_as_dict) {
			count++;
		});
		return count;
	};
	

	this.markDirty = function() {
		this.dirty = true;
		
		queueAutoSave(this.id);
		
		showGroup(this.id);
	};
	
	this.save = function() {

		if (this.is_save_in_progress) {
			console.log("Save of group " + this.id + " is already in progress. Aborting.");
			return;
		}

		this.is_save_in_progress = true;
		var save_message = "Saving group \"" + this.label + "\"...";
		console.log(save_message);
		$("#saving_status_indicator").text(save_message);
		$("#saving_status_indicator").show();
		
		
		
		var groups = {};
		groups[this.label] = this.asDictionary();

		var action_type = this.id < 0 ? "insert" : "modify";
		var jsonString = JSON.stringify(groups);
		
		
		
		var group_object_reference = this;
		$.post("save", {
			action : action_type,
			json : jsonString,
		}, function(data) {
			if (data.success) {
				
				group_object_reference.dirty = false;
		
				if (data.created_new_group) {
		
					var old_group_id = group_object_reference.id;
					group_object_reference.id = data.new_group_id;
					group_objects_by_id[group_object_reference.id] = new_group;
					group_object_reference.mine = true;
					delete group_objects_by_id[old_group_id];
				}
		
				renderGroups();
				showGroup(group_object_reference.id);
			} else {
				alert("Failed saving group " + group_object_reference.label + ":\n" + data.message);
			}
			

			group_object_reference.is_save_in_progress = false;
			$("#saving_status_indicator").hide();	// FIXME Another group may be concurrently saving...
		});
	};
}

//============================================================================
function GroupMember(alias) {
	
	this.alias = alias;
	this.set_by = null;
	this.modified = null;
	this.proficiency = 0;
	
	this.getName = function() {
		if (this.alias in fullname_cache) {
			return fullname_cache[this.alias];
		} else {
			return "-unknown-";
		}
	};
	
	this.isEqual = function(other_person) {
		return this.alias == other_person.alias;
	};
	
	this.asDictionary = function() {
		var dict = {
				"alias": this.alias,
				"set_by": this.set_by,
				"modified": this.modified,
				"proficiency": this.proficiency
		};
		return dict;
	};
	
	this.getEmailAddress = function() {
		return alias + "@" + company_domain;
	};
}