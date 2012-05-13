package com.kostmo.grouper.persistence;



public class Tag {
	
	public final String name;
	public final int usage_count;
	
	public Tag(String name, int usage_count) {
		this.name = name;
		this.usage_count = usage_count;
	}
}