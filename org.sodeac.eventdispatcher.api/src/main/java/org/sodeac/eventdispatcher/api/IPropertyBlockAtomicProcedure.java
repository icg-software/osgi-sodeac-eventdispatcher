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
package org.sodeac.eventdispatcher.api;

import java.util.function.Consumer;

/**
 * An operation handler for complex editing a property block in atomic way.
 * 
 * @author Sebastian Palarus
 *
 */
public interface IPropertyBlockAtomicProcedure extends Consumer<IPropertyBlock>
{
	/**
	 * Edit property block in locked mode. Use {@code propertyBlock}  to read or edit block.
	 * 
	 * @param propertyBlock wrapper to origin blocked property block.
	 */
	public void accept(IPropertyBlock propertyBlock);
}
