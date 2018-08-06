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
package org.sodeac.eventdispatcher.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.MetricsRequirement;
import org.sodeac.eventdispatcher.api.PrivateQueueWorkerRequirement;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
@EventDispatcherAnnotation(tag="org.sodeac.eventdispatcher.api.IEventDispatcher")
@Repeatable(QueuesMatchedToFilter.class)
public @interface QueueMatchedToFilter
{
	String value();
	String dispatcherId() default IEventDispatcher.DEFAULT_DISPATCHER_ID;
	String name() default "";
	String category() default "";
	MetricsRequirement queueMetricRequirement() default MetricsRequirement.RequireMetrics;
	PrivateQueueWorkerRequirement privateQueueWorkerRequirement() default PrivateQueueWorkerRequirement.NoPreferenceOrRequirement;
}
