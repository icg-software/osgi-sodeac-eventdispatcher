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

import java.util.List;

public interface IScheduleResult
{
	public void addError(Throwable throwable);
	public boolean hasErrors();
	public List<Throwable> getErrorList();
	public boolean isScheduled();
	public void setScheduled();
	public Object getDetailResultObject();
	public void setDetailResultObject(Object detailResultObject);
	public List<Object> getDetailResultObjectList();
	public void addDetailResultObjectList(Object detailResultObject);
	
}
