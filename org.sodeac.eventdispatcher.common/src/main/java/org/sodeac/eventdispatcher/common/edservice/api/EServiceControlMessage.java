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
package org.sodeac.eventdispatcher.common.edservice.api;

public class EServiceControlMessage
{
	private String sourceURI = null;
	private String destinationURI = null;
	private String domain = null;
	private String boundedContext = null;
	private Long messageId = null;
	private int messageType = 0; // TODO enum
	private String message = null;
	private Exception exception = null;
	
	// TODO WindowImpl
}
