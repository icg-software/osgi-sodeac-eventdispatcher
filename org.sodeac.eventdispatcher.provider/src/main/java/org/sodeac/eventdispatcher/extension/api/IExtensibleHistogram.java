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
package org.sodeac.eventdispatcher.extension.api;

import org.sodeac.eventdispatcher.api.IHistogram;

public interface IExtensibleHistogram extends IHistogram
{
	public String getKey();
	public String getName();
	public IExtensibleMetrics getMetrics();
}
