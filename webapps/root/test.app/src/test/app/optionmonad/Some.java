/******************************************************************************
 * Copyright (c) David Orme and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    David Orme - initial API and implementation
 ******************************************************************************/
package test.app.optionmonad;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * An Option instance that contains a value.
 * 
 * @param <T> The type that this Option is encapsulating.
 */
public final class Some<T> implements Option<T> {
    private final T value;
    private IStatus status = null;
 
    /**
     * Construct an Option with Some(value).
     * 
     * @param value The value to encapsulate.
     */
    public Some(T value) {
        this.value = value;
    }
 
    /**
     * Construct an Option with Some(value).
     * 
     * @param value The value to encapsulate.
     */
    public Some(T value, IStatus status) {
        this.value = value;
        this.status = status;
    }
 
	/**
	 * A convenience factory method meant to be imported statically and that
	 * eliminates a lot of the boilerplate that Java generics impose.
	 * 
	 * @param <T> The type of Option object to create.  Usually inferred 
	 * automatically by the compiler.
	 * @param value The value to return.
	 * 
	 * @return a new Some<T> containing the specified value.
	 */
	public static <T> Option<T> some(T value) { return new Some<T>(value); }
	
	/**
	 * A convenience factory method meant to be imported statically and that
	 * eliminates a lot of the boilerplate that Java generics impose.
	 * 
	 * @param <T> The type of Option object to create.  Usually inferred 
	 * automatically by the compiler.
	 * @param value The value to return.
	 * @param status The IStatus containing extra information (possibly for logging).
	 * 
	 * @return a new Some<T> containing the specified value.
	 */
	public static <T> Option<T> some(T value, IStatus status) { return new Some<T>(value, status); }
	
    /* (non-Javadoc)
     * @see org.eclipse.e4.core.functionalprog.optionmonad.Option#get()
     */
    public T get() {
        return value;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.e4.core.functionalprog.optionmonad.Option#getOrSubstitute(java.lang.Object)
	 */
	public T getOrSubstitute(T defaultValue) {
		return value;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.e4.core.functionalprog.optionmonad.Option#getOrThrow(java.lang.Throwable)
	 */
	public <E extends Throwable> T getOrThrow(E exception) {
		return value;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.e4.core.functionalprog.optionmonad.Option#hasValue()
	 */
	public boolean hasValue() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.e4.core.functionalprog.optionmonad.Option#getStatus()
	 */
	public IStatus getStatus() {
		return Nulls.valueOrSubstitute(status, Status.OK_STATUS);
	}
}
 
