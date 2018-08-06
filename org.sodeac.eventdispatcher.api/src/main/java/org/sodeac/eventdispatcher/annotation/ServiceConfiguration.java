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
import java.util.concurrent.TimeUnit;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
@EventDispatcherAnnotation(tag="org.sodeac.eventdispatcher.api.IEventDispatcher")
@Repeatable(ServiceConfigurations.class)
public @interface ServiceConfiguration
{
	String id();
	String name() default "";
	String category() default "";
	long timeOut() default -1;
	TimeUnit timeOutUnit() default TimeUnit.MILLISECONDS;
	long heartbeatTimeOut() default -1;
	TimeUnit heartbeatTimeOutUnit() default TimeUnit.MILLISECONDS;
	long startDelay() default 0;
	TimeUnit startDelayUnit() default TimeUnit.MILLISECONDS;
	long periodicRepetitionInterval() default -1;
	TimeUnit periodicRepetitionIntervalyUnit() default TimeUnit.MILLISECONDS;
}
