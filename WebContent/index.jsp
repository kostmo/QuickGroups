<html>
<head>
<title>Quick Groups</title>
<link rel='stylesheet' href='style/jquery.ui.all.css'>
<script src='scripts/jquery-1.7.1.js'></script>
<script src='scripts/ui/jquery.ui.core.js'></script>
<script src='scripts/ui/jquery.ui.widget.js'></script>
<script src='scripts/ui/jquery.ui.position.js'></script>
<script src='scripts/ui/jquery.ui.autocomplete.js'></script>

<script src='scripts/grouper/object_definitions.js'></script>
<script src='scripts/grouper/backend.js'></script>
<script src='scripts/grouper/library.js'></script>
<script src='scripts/grouper/display.js'></script>

<link rel='stylesheet' type='text/css' href='style/grouper.css' />

<script type="text/javascript">
var logged_in_username = "<%=request.getRemoteUser()%>";
var company_domain = "example.com"; // FIXME
</script>

</head>
<body style='font-family: sans-serif'>

	<div class='login-info'>
		Logged in as <b><%=request.getRemoteUser()%></b>
	</div>

	<h1>Quick Groups</h1>
	<span class="main_instructions" id="instructions_no_groups"
		style="display: none">Click "New group" to begin.</span>
	<span class="status_message" id="status_message">Loading...</span>


	<table id="container">
		<tr>
			<td style="min-width: 250px">Show groups with tags:

				<div id="filter_tags_list"></div>
				<div class='ui-widget'>
					<label for='filter_tag_input'>Add tag filter: </label><input
						id='filter_tag_input' /> <img
						style='display: none; vertical-align: middle;'
						id='filter_hourglass_img' src='images/square-ajax-loader.gif' />
				</div> <br />
				<button onclick='newGroup();'>New group</button> <br />
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

				<div id="outer_grouplist_container">


					<h3>
						<span id="group_list_header">Loading...</span> <img
							style='display: none; vertical-align: middle;'
							id='group_load_hourglass_img' src='images/square-ajax-loader.gif' />
					</h3>
					<ul id="group_list"></ul>

				</div>

			</td>
			<td style="min-width: 250px">

				<table id="group_info_display" style="display: none">
					<tr>
						<td style="vertical-align: top">
							<h3>Group membership:</h3>
							<div class='ui-widget'>
								<label for='namefield'>Add Member: </label><input
									class="modifying_actions" id='namefield' /> <img
									style='display: none; vertical-align: middle;'
									id='hourglass_img' src='images/square-ajax-loader.gif' />
							</div> Search by: <label><input type="radio"
								name="search_by_radio_group" value="alias" checked="checked" />Alias</label>
							<label><input type="radio" name="search_by_radio_group"
								value="name" />Full Name</label>

							<p>
								<button class="modifying_actions" onclick='bulkMemberAdd();'>Bulk
									Member Add</button>
							</p>
							<h4>Current members:</h4>
							<div id="current_members_box">
								<span id="member_count">0 member(s).</span>
								<ul id='group_holder'></ul>
							</div>

						</td>
						<td style="vertical-align: top; padding-left: 20px">

							<fieldset>
								<legend>Group properties</legend>

								Group label: <input onchange="changeGroupName(this);"
									type='text' id='group_label' value='Untitled Group' /><br />
								<table>
									<tr>
										<td>
										<td><input class="modifying_actions"
											onchange="changePublicVisibility(this);" id='is_public'
											type='checkbox' /></td>
										<td><label for="is_public"> Publicly visible</label></td>
										<td><input class="modifying_actions"
											onchange="changeSelfServe(this);" id='is_self_serve'
											type='checkbox' /></td>
										<td><label for="is_self_serve"> Self-serve
												membership</label></td>
									</tr>



								</table>
								<h4>Tags:</h4>
								<div id="group_tags_list"></div>
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

								<a id="group_email_url" href="mailto:">Send email to group</a>
							</fieldset>

						</td>
					</tr>
				</table>
			</td>
		</tr>
	</table>
	<hr class="clear" />
	<a href="about.html">About</a>
</body>
</html>