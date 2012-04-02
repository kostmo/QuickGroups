<html><head><title>Active Directory lookup</title>
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
var logged_in_username = "<%= request.getRemoteUser() %>";
</script>

</head><body style='font-family: sans-serif'>

<div class='login-info'>Logged in as <b><%= request.getRemoteUser() %></b></div>

<h1>AD Grouper</h1>
<span class="main_instructions" id="instructions_no_groups" style="display: none">Click "New group" to begin.</span>
<span class="status_message" id="status_message">Loading...</span>


<div id="container">

<div id="left">

<h3><span id="group_list_header"></span></h3>
<ul id="group_list"></ul>
<img style='display:none; float: left' id='group_load_hourglass_img' src='images/square-ajax-loader.gif'/>
<button onclick='newGroup();'>New group</button>


<h3>Bulk actions:</h3>
<button onclick='exportAllGroups();'>Export all</button><br />
<a id="query_url" href="blah">Machine query link</a>
<p>
Format:<br />
<label><input onchange="changeExportType(this);" type="radio" name="export_radio_group" value="XML"/>XML</label><br />
<label><input onchange="changeExportType(this);" type="radio" name="export_radio_group" value="JSON"/>JSON</label><br />
<label><input onchange="changeExportType(this);" type="radio" name="export_radio_group" value="CSV"/>CSV</label>
</p>
</div>

</div>


<div id="right">

<table id="group_info_display" style="display: none"><tr><td style="vertical-align: top">
<h3>Group membership:</h3>
Search by:<br/>
<label><input type="radio" onchange="changeAutocompleteSource(this);" name="search_by_radio_group" value="alias" checked="checked"/>Alias</label><br/>
<label><input type="radio" onchange="changeAutocompleteSource(this);" name="search_by_radio_group" value="name"/>Full Name</label><br/>
<div class='ui-widget'>
<label for='namefield'>Add Member: </label><input class="modifying_actions" id='namefield'/> <img style='display:none; vertical-align: middle;' id='hourglass_img' src='images/square-ajax-loader.gif'/>
</div>
<h4>Current members:</h4>
<span id="member_count">0 member(s).</span>
<ul id='group_holder'></ul>

</td><td style="vertical-align: top; padding-left: 20px">

<h3>Group properties:</h3>
<div>Group label: <input onchange="changeGroupName(this);" type='text' id='group_label' value='Untitled Group'/><br/>
<table><tr>
<td>
<td><input class="modifying_actions" onchange="changePublicVisibility(this);" id='is_public' type='checkbox'/></td><td><label for="is_public"> Publicly visible</label></td>
<td><input class="modifying_actions" onchange="changeSelfServe(this);" id='is_self_serve' type='checkbox'/></td><td><label for="is_self_serve"> Self-serve membership</label></td>
</tr></table>
<h3>Group manipulation:</h3>
<button class="modifying_actions" onclick='saveGroup(active_group_id);' id="save_button">Save</button>
<button onclick='copyGroup(active_group_id);'>Copy</button>
<button class="modifying_actions" onclick='deleteGroup(active_group_id);'>Delete</button>

<h3>Group functions:</h3>
<div><button onclick='exportGroup(active_group_id);'>Export</button> </div>

<a id="group_email_url" href="mailto:">Send email to group</a>
</div>
</td></tr></table>


</div>
<hr class="clear"/>
<a href="about.html">About</a>

</body></html>