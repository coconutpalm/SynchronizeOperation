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

import static test.app.optionmonad.None.none;
import static test.app.optionmonad.Some.some;

/**
 * Convenience methods encoding the Option<T> style of dealing with an "empty"
 * value when the value follows the traditional pattern of returning null 
 * as the "empty" value.
 * <p>
 * This is mainly related to Option monads in that it lets the programmer deal
 * with null values similarly to the way s/he would use a proper Option monad
 * with the goal of making null processing more explicit in the code.
 * <p>
 * However, we additionally supply a type-safe Option<T> factory for 
 * simply/easily wrapping a value that might or might not be null into Some<T> 
 * or None<T> without messy Java typecasts.
 * <p>
 * These methods are meant to be imported statically.
 */
public class Nulls {
	/**
	 * A common way of processing results where a null value indicates
	 * failure is to throw an exception. This method makes this coding pattern
	 * explicit.
	 * 
	 * @param <T>
	 *            The type of value to process.
	 * @param <E>
	 *            The type of exception we might throw.
	 * @param value
	 *            The value to return if non-null.
	 * @param exception
	 *            The exception to throw if value==null.
	 * @return value iff value != null.
	 * @throws E
	 *             iff value == null.
	 */
	public static <T, E extends Throwable> T valueOrThrow(T value, E exception) throws E {
		if (value != null) return value;
		throw exception;
	}
	
	/**
	 * A common way of processing results where a null value indicates
	 * failure is to substitute a default value. This method makes this coding
	 * pattern explicit.
	 * 
	 * @param <T>
	 *            The type of value we are processing.
	 * @param value
	 *            The value to return iff value != null.
	 * @param defaultValue
	 *            The default value to return iff value == null.
	 * @return value if value != null else return defaultValue.
	 */
	public static <T> T valueOrSubstitute(T value, T defaultValue) {
		if (value != null) return value;
		return defaultValue;
	}
	
	/**
	 * A static factory for Options.  Turns something that is either a value
	 * or null into a proper Option with Some(value) or None().
	 * 
	 * @param <T> The type of thing to wrap up in an Option.
	 * @param value The value to wrap.
	 * @return Some(value) iff value != null or None() otherwise.
	 */
	public static <T> Option<T> option(T value) {
		if (value != null) return some(value);
		else return none();
	}
}
