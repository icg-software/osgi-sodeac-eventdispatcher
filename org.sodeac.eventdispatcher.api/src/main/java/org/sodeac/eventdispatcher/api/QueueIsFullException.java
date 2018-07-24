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
public class QueueIsFullException extends IllegalStateException
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5515576205080660949L;
	
	private String queueId;
	private int limit;

	public QueueIsFullException(String queueId,int limit)
	{
		super();
		this.queueId = queueId;
		this.limit = limit;
	}

	public QueueIsFullException(String queueId,int limit, String message, Throwable cause)
	{
		super(message, cause);
		this.queueId = queueId;
		this.limit = limit;
	}

	public QueueIsFullException(String queueId,int limit, String s)
	{
		super(s);
		this.queueId = queueId;
		this.limit = limit;
	}

	public QueueIsFullException(String queueId,int limit, Throwable cause)
	{
		super(cause);
		this.queueId = queueId;
		this.limit = limit;
	}

	public String getQueueId()
	{
		return queueId;
	}

	public int getLimit()
	{
		return limit;
	}

}
