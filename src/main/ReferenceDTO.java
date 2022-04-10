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
