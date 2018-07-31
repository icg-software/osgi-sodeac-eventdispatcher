/*******************************************************************************
 * Copyright (c) 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.common.reactiveservice.api;

public class ReactiveServiceException extends Exception 
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7054053366027811461L;

	public ReactiveServiceException() 
	{
		super();
	}

	public ReactiveServiceException(String message, Throwable cause, boolean enableSuppression,boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ReactiveServiceException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ReactiveServiceException(String message)
	{
		super(message);
	}

	public ReactiveServiceException(Throwable cause)
	{
		super(cause);
	}

}
