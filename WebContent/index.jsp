<html>
<head>
<title>Quick Groups</title>
<link rel='stylesheet' href='style/jquery.ui.all.css'>
<script src='scripts/jquery-1.7.1.js'></script>
<script src='scripts/ui/jquery.ui.core.js'></script>
<script src='scripts/ui/jquery.ui.widget.js'></script>
<script src='scripts/ui/jquery.ui.position.js'></script>
<script src='scripts/ui/jquery.ui.autocomplete.js'></script>
<script src='scripts/ui/jquery.jeditable.js'></script>

<script src='scripts/grouper/object_definitions.js'></script>
<script src='scripts/grouper/backend.js'></script>
<script src='scripts/grouper/library.js'></script>
<script src='scripts/grouper/display.js'></script>
<script src='scripts/color.js'></script>

<link rel='stylesheet' type='text/css' href='style/grouper.css' />

<script type="text/javascript">
var logged_in_username = "<%=request.getRemoteUser()%>";
var company_domain = "${company_domain}";
</script>

</head>
<body style='font-family: sans-serif'>

	<div class='login-info'>
		Logged in as <b><%=request.getRemoteUser()%></b>
	</div>


	<span class="main_instructions" id="instructions_no_groups"
		style="display: none">Click "New group" to begin.</span>
	<span class="status_message" id="status_message">Loading...</span>


	<table id="container">
		<tr>
			<td style="min-width: 250px">

				<span
					style="vertical-align: middle">Show groups with <select
						onchange="renderGroups();" id="tag_filter_criteria">
							<option value="any" selected="selected">any</option>
							<option value="all">all</option>
					</select> of these tags:
				</span>

				<div id="filter_tags_list" style="margin-top: 0.4em; margin-bottom: 0.4em"></div>

				<div class='ui-widget'>
					<label for='filter_tag_input'>Add tag filter: </label><input
						id='filter_tag_input' /> <img
						style='display: none; vertical-align: middle;'
						id='filter_hourglass_img' src='images/square-ajax-loader.gif' />
				</div>

				<div id="outer_grouplist_container">

					<h3>
						<span id="group_list_header">Loading...</span>
						<button onclick='newGroup();'>New group</button>
						<img
							style='display: none; vertical-align: middle;'
							id='group_load_hourglass_img' src='images/square-ajax-loader.gif' />
					</h3>


					<p>
						<a id="filtered_groups_email_url" href="mailto:">Start a
							discussion with everyone in these groups</a>
					</p>

					
					Sort by: <label><input type="radio"
						onchange="renderGroups();" name="group_sort_radio_group"
						value="member_count" />Member count</label> <label><input
						type="radio" onchange="renderGroups();"
						name="group_sort_radio_group" value="alphabetical"
						checked="checked" />Alphabetical</label>

					<ul id="group_list"></ul>

				</div>

			</td>
			<td style="min-width: 250px">

				<div class="group_info_display" style="display: none">


					<h2>
						<span id='group_label_editbox'>Group Title</span> <img
							src="images/tiny_pencil.png" />
					</h2>

					<fieldset>
						<legend>Group properties</legend>
						<table>
							<tr>
								<td>
								<td><input class="modifying_actions"
									onchange="changePublicVisibility(this);" id='is_public'
									type='checkbox' /></td>
								<td><label for="is_public"
									title="Everyone can see this group"> Publicly visible</label></td>
								<td><input class="modifying_actions"
									onchange="changeSelfServe(this);" id='is_self_serve'
									type='checkbox' /></td>
								<td><label for="is_self_serve"
									title="People can add and remove themselves">
										Self-serve membership</label></td>
								<td><input class="modifying_actions"
									onchange="changeIsSkill(this);" id='is_skill' type='checkbox' /></td>
								<td><label for="is_skill"
									title="This group is about a skill"> Skill</label></td>
							</tr>

						</table>
					</fieldset>

					<fieldset>
						<legend>Tags</legend>

						<p><span id="group_tags_list"></span></p>
						<div class='ui-widget'>
							<label for='group_tag_input'>Add Tag: </label><input
								class="modifying_actions" id='group_tag_input' /> <img
								style='display: none; vertical-align: middle;'
								id='group_tag_hourglass_img'
								src='images/square-ajax-loader.gif' />
						</div>
					</fieldset>

					<fieldset>
						<legend>Group maintainer</legend>
						<p>
						<span id="current_group_maintainer" style="font-weight: bold">Nobody</span>
						</p>
						Select new maintainer:
						<div class='ui-widget'>
							<label for='maintainer_field'></label><input
								class="modifying_actions" id='maintainer_field' /> <img
								style='display: none; vertical-align: middle;'
								id='maintainer_hourglass_img'
								src='images/square-ajax-loader.gif' />
							<button class="modifying_actions"
								onclick='transferOwnership($("#maintainer_field").val());'>Transfer
								ownership</button>
						</div>

					</fieldset>

					<fieldset>
						<legend>Group manipulation</legend>
						<button class="modifying_actions"
							onclick='saveGroup(active_group_id);' id="save_button">Save</button>
						<button onclick='copyGroup(active_group_id);'>Copy</button>
						<button class="modifying_actions"
							onclick='deleteGroup(active_group_id);'>Delete</button>
						<button class="modifying_actions" onclick='bulkMemberAdd();'>Bulk
							Add</button>
						<button class="modifying_actions"
							onclick="toggle_merge_options()">Merge &gt;&gt;</button>
						<div id="merge_options" style="display: none">From group:
						</div>

					</fieldset>

					<fieldset>
						<legend>Group functions</legend>
						<div>
							<button onclick='exportGroup(active_group_id);'>Export</button>
						</div>

						<a id="group_email_url" href="mailto:">Start a discussion
							with the people in this group</a>
					</fieldset>

				</div>
						
				<button onclick="toggle_advanced_bulk_options(this)">Show
					Advanced &gt;&gt;</button>

				<fieldset id="advanced_bulk_options" style="display: none">
					<legend>Exporting</legend>

					<button onclick='exportAllGroups();'>Export all</button>
					<p>
						Format:<br /> <label><input
							onchange="changeExportType(this);" type="radio"
							name="export_radio_group" value="XML" />XML</label><br /> <label><input
							onchange="changeExportType(this);" type="radio"
							name="export_radio_group" value="JSON" />JSON</label><br /> <label><input
							onchange="changeExportType(this);" type="radio"
							name="export_radio_group" value="CSV" />CSV</label>
					</p>
					<a id="query_url" href="blah">Machine query link</a>
				</fieldset>
				
			</td>


			<td style="min-width: 250px">


				<div class="group_info_display" style="display: none">

					<div class='ui-widget'>
						<label for='namefield'>Add Member: </label><input
							class="modifying_actions" id='namefield' /> <img
							style='display: none; vertical-align: middle;'
							id='hourglass_img' src='images/square-ajax-loader.gif' />
					</div>
					<div id="search_filter_criteria" style="display: none">
						Search by: <label><input type="radio"
							name="search_by_radio_group" value="alias" />Alias</label> <label><input
							type="radio" name="search_by_radio_group" value="name" />Full
							Name</label> <label><input type="radio"
							name="search_by_radio_group" value="both" checked="checked" />Both</label>
					</div>
					
					<div id="current_members_box">
						<span id="member_count">0 member(s).</span>
						<ul id="members_holder"></ul>
					</div>

				</div>
		
			</td>
		</tr>
	</table>
	<hr class="clear" />
	<a href="about.html">About</a>
</body>
</html>