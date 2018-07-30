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
package org.sodeac.eventdispatcher.impl;

import java.util.UUID;

import org.sodeac.eventdispatcher.api.IPropertyLock;

public class PropertyLockImpl implements IPropertyLock
{
	private PropertyBlockImpl block;
	private String key;
	private UUID pin;
	
	protected PropertyLockImpl(PropertyBlockImpl block,String key, UUID pin)
	{
		super();
		this.block = block;
		this.key = key;
		this.pin = pin;
	}

	@Override
	public boolean unlock()
	{
		return block.unlockProperty(this);
	}

	protected PropertyBlockImpl getBlock()
	{
		return block;
	}

	protected String getKey()
	{
		return key;
	}

	protected UUID getPin()
	{
		return pin;
	}

}
