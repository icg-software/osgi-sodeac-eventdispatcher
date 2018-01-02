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

import java.util.List;

public interface IPropertyBlockModifyListener
{
	public static enum ModifyType {INSERT,UPDATE,REMOVE}
	
	public void onModify(ModifyType type,String key, Object valueOld, Object valueNew);
	public void onModifySet(List<PropertyBlockModifyItem> modifySet);
}
