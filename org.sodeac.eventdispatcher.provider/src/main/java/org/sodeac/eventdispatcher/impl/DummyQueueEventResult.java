/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.impl;

import java.util.List;

public class DummyQueueEventResult extends QueueEventResultImpl
{
	public DummyQueueEventResult(){}

	@Override
	public void markQueued(){}

	@Override
	public boolean isQeueued()
	{
		return false;
	}

	@Override
	public void addError(Throwable throwable){}

	@Override
	public boolean hasErrors()
	{
		return false;
	}

	@Override
	public List<Throwable> getErrorList()
	{
		return null;
	}

	@Override
	public Object getDetailResultObject()
	{
		return null;
	}

	@Override
	public void setDetailResultObject(Object detailResultObject){}

	@Override
	public List<Object> getDetailResultObjectList()
	{
		return null;
	}

	@Override
	public boolean isDummy()
	{
		return true;
	}

	@Override
	public void addDetailResultObjectList(Object detailResultObject)
	{
	}

	@Override
	protected void waitForProcessingIsFinished(){}

	@Override
	protected void processPhaseIsFinished(){}
	
	
}
