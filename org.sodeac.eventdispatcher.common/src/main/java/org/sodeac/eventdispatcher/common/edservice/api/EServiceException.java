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
package org.sodeac.eventdispatcher.common.edservice.api;

public class EServiceException extends Exception 
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7054053366027811461L;

	public EServiceException() 
	{
		super();
	}

	public EServiceException(String message, Throwable cause, boolean enableSuppression,boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public EServiceException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public EServiceException(String message)
	{
		super(message);
	}

	public EServiceException(Throwable cause)
	{
		super(cause);
	}

}
