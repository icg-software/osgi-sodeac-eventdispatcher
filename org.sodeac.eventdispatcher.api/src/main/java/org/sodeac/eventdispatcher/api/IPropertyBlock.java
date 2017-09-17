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

import java.util.List;
import java.util.Map;

public interface IPropertyBlock
{
	public Object setProperty(String key,Object value);
	public Object getProperty(String key);
	public Object removeProperty(String key);
	public Map<String, Object> getProperties();
	public List<String> getPropertyKeys();
	public void clear();
}
