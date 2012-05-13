
function compareValues(dictionary, a, b, value_getter) {
	
	var val_a = value_getter(dictionary, a);
	var val_b = value_getter(dictionary, b);
	
	if (val_a < val_b) return -1;
	else if (val_a > val_b) return 1;
	else return 0;
};

function getSortedDictionaryKeys(dictionary, value_getters) {

    var keys = [];
    for (var key in dictionary)
        keys.push(key);

    keys.sort(function(a, b) {
    	
    	for (var i=0; i<value_getters.length; i++) {
    		var value_getter = value_getters[i];
    		
    		var comparison_result = compareValues(dictionary, a, b, value_getter);
    		if (comparison_result)
    			return comparison_result;
    	}
    	
    	return 0;
    });

    return keys;
}