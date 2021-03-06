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
 * An Option instance that does not contain any value.
 * 
 * @param <T> The type that this Option is encapsulating.
 */
public final class None<T> implements Option<T> {
 
    private IStatus status = null;

	/**
     * Construct a None<T>.
     */
    public None() {}
 
	/**
	 * Construct a None<T>, supplying an IStatus for the reason there is no result.
	 * 
	 * @param status the reason we can't provide an answer, typically some sort of error status.
	 */
	public None(IStatus status) {
		this.status = status;
	}

	/**
	 * A convenience factory method meant to be imported statically and that
	 * eliminates a lot of the boilerplate that Java generics impose.
	 * 
	 * @param <T> The type of Option object to create.  Usually inferred 
	 * automatically by the compiler.
	 * 
	 * @return a new None<T>.
	 */
	public static <T> Option<T> none() { return new None<T>(); }
	
	public static <T> Option<T> none(IStatus reason) {
		return new None<T>(reason);
	}

    /* (non-Javadoc)
     * @see org.eclipse.e4.core.functionalprog.optionmonad.Option#get()
     */
    public T get() {
        throw new UnsupportedOperationException("Cannot resolve value on None");
    }

	/* (non-Javadoc)
	 * @see org.eclipse.e4.core.functionalprog.optionmonad.Option#getOrSubstitute(java.lang.Object)
	 */
	public T getOrSubstitute(T defaultValue) {
		return defaultValue;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.e4.core.functionalprog.optionmonad.Option#getOrThrow(java.lang.Throwable)
	 */
	public <E extends Throwable> T getOrThrow(E exception) throws E {
		throw exception;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.e4.core.functionalprog.optionmonad.Option#hasValue()
	 */
	public boolean hasValue() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.e4.core.functionalprog.optionmonad.Option#getReason()
	 */
	public IStatus getStatus() {
		return Nulls.valueOrSubstitute(status, Status.CANCEL_STATUS);
	}
}