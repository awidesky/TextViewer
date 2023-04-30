/*
 * Copyright (c) 2023 Eugene Hong
 *
 * This software is distributed under license. Use of this software
 * implies agreement with all terms and conditions of the accompanying
 * software license.
 * Please refer to LICENSE
 * */

package main;

/**
 * Class that holds reference of object so that it can transfer(send) object from callee to caller.
 * If synchronization is needed, use <code>AtomicReference<T></code>
 * 
 * */
public class ReferenceDTO<T> {

	private T ref = null;
	
	public ReferenceDTO() {}
	public ReferenceDTO(T initialRef) {
		ref = initialRef;
	}
	
	public void set(T newRef) {
		ref = newRef;
	}
	public T get() {
		return ref;
	}
	
}
