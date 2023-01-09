package com.foo;


import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

@Entity
public class ReferenceDocument {
	@Id
	public int id;
}

