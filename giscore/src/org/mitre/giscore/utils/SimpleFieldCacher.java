/****************************************************************************************
 *  SimpleFieldCacher.java
 *
 *  Created: Dec 16, 2009
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2009
 *
 *  The program is provided "as is" without any warranty express or implied, including
 *  the warranty of non-infringement and the implied warranties of merchantibility and
 *  fitness for a particular purpose.  The Copyright owner will not be liable for any
 *  damages suffered by you as a result of using the Program.  In no event will the
 *  Copyright owner be liable for any special, indirect or consequential damages or
 *  lost profits even if the Copyright owner has been advised of the possibility of
 *  their occurrence.
 *
 ***************************************************************************************/
package org.mitre.giscore.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.mitre.giscore.events.SimpleField;

/**
 * This cacher recognizes and caches simple fields to reduce the overhead in the
 * object buffer.
 * 
 * @author DRAND
 */
public class SimpleFieldCacher implements IObjectCacher {
	/**
	 * Cached fields
	 */
	private Map<SimpleField, Long> fields = new HashMap<SimpleField, Long>();

	/**
	 * Counter
	 */
	private AtomicLong counter = new AtomicLong();

	@Override
	public void addToCache(Object field) {
		if (fields.containsKey(field)) {
			throw new IllegalStateException("Field is already in the collection");
		}
		fields.put((SimpleField) field, new Long(counter.incrementAndGet()));
	}

	@Override
	public Long getObjectOutputReference(Object field) {
		return fields.get(field);
	}

	@Override
	public boolean hasBeenCached(Object field) {
		return fields.containsKey(field);
	}

	@Override
	public boolean shouldCache(Object field) {
		return field instanceof SimpleField;
	}
}
