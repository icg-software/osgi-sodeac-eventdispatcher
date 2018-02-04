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
package org.sodeac.eventdispatcher.api;

/**
 * 
 * @author Sebastian Palarus
 *
 */
public class QueueNotFoundException extends RuntimeException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 9068625039710702255L;
	
	private String queueId = null;

	public QueueNotFoundException(String queueId)
	{
		super();
		this.queueId = queueId;
	}

	public QueueNotFoundException(String queueId,String message, Throwable cause, boolean enableSuppression,boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
		this.queueId = queueId;
	}

	public QueueNotFoundException(String queueId, String message, Throwable cause)
	{
		super(message, cause);
		this.queueId = queueId;
	}

	public QueueNotFoundException(String queueId,String message)
	{
		super(message);
		this.queueId = queueId;
	}

	public QueueNotFoundException(String queueId,Throwable cause)
	{
		super(cause);
		this.queueId = queueId;
	}

	public String getQueueId()
	{
		return queueId;
	}
	
}
