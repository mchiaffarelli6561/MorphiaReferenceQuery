package com.foo;


import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;

@Entity
public class RootDocument {
	@Id
	public int id;
	@Reference
	public ReferenceDocument ref;
}
