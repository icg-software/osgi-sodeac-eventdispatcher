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

public class PushPolicy
{
	// Todo NotifyMask
	
	private long allowedLatenceTotal = -1L; // unabhaengig vom letzten Push
	private long allowedLatenceLastPush = -1L; // abhangig vom letzten Push
	private long allowedDataBufferSize = -1L; // anzahl der Datensaetze
	private long timeoutAck = -1; // Warte auf Empfangsbestaetigung
	private int sendWithoutAckCount = -1; // Anzahl der SendePuffers LinkedList?
	private int resendUnAckedCount = -1; // Erneutes Senden nach Timeout - Wie oft
	
}
