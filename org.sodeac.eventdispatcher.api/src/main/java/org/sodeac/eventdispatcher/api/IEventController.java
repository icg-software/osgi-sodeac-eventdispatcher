/*******************************************************************************
 * Copyright (c) 2017 Sebastian Palarus
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
 * An eventcontroller reacts to a wide variety of queue happenings, if it implements appropriate extension interfaces.
 * 
 * @author Sebastian Palarus
 *
 */
public interface IEventController
{
	public static final String CONSUME_EVENT_TOPIC = "consume.event.topic";
}
