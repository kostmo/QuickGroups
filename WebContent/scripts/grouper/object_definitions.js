function Group(label) {
	this.label = label;
	this.id = -1;	// -1 indicates a group that has not been persisted to the database, yet.
	this.dirty = false;
	this.member_objects_by_alias = {};
	this.tags = [];
	this.mine = true;
	
	this.is_self_serve = false;
	this.is_public = true;
	this.is_skill = false;
	
	this.containsAlias = function(alias) {
		return this.aliases.indexOf(alias) != -1;
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
		showGroup(this.id);
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