function getSortedDictionaryKeys(dictionary, value_getter) {

    var keys = [];
    for (var key in dictionary)
        keys.push(key);

    keys.sort(function(a, b) {
    	var val_a = value_getter(dictionary, a);
    	var val_b = value_getter(dictionary, b);
    	
    	if (val_a < val_b) return -1;
    	else if (val_a > val_b) return 1;
    	else return 0;
    });
    return keys;
}