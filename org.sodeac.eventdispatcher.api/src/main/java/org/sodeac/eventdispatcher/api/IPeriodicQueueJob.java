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
 * An extenstion interface for {@link IQueueJob}. Jobs implements this interface will not finished by default and re-run periodically by worker, until the job set done manually with state-handler {@link IJobControl}.
 * 
 * @author Sebastian Palarus
 *
 */
public interface IPeriodicQueueJob extends IQueueJob
{
	/**
	 * default periodic interval
	 * 
	 * @return
	 */
	public long getPeriodicRepetitionInterval();
}
