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

public interface IHistogram
{
	public interface Snapshot
	{
		public double getValue(double quantile);
		public long[] getValues();
		public int size();
		public double getMedian();
		public double get75thPercentile();
		public double get95thPercentile();
		public double get98thPercentile();
		public double get99thPercentile();
		public double get999thPercentile();
		public long getMax();
		public double getMean();
		public long getMin();
	}
	
	public void update(int value);
	public void update(long value);
	public long getCount();
	public Snapshot getSnapshot();
}
