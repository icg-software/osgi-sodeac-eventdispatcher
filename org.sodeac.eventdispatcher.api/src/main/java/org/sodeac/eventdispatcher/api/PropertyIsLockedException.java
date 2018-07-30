/*******************************************************************************
 * Copyright (c) 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.api;

/**
 * 
 * @author Sebastian Palarus
 *
 */
public class PropertyIsLockedException extends RuntimeException
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6738548351245631901L;

	public PropertyIsLockedException(String message, Throwable cause, boolean enableSuppression,boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public PropertyIsLockedException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public PropertyIsLockedException(String message)
	{
		super(message);
	}

	public PropertyIsLockedException(Throwable cause)
	{
		super(cause);
	}

}
