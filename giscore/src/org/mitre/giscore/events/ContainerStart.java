/****************************************************************************************
 *  ContainerStart.java
 *
 *  Created: Jan 26, 2009
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
package org.mitre.giscore.events;

import org.mitre.giscore.output.StreamVisitorBase;


/**
 * We've seen the start of a container. A container can hold zero or more features
 * and zero or more sub containers.
 * 
 * @author DRAND
 */
public class ContainerStart extends BaseStart {
	private static final long serialVersionUID = 1L;
	
	private String type;
	
	/**
	 * Ctor
	 */
	public ContainerStart(String type) {
		if (type == null) {
			throw new IllegalArgumentException("type should never be null");
		}
		this.type = type;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	
    public void accept(StreamVisitorBase visitor) {
    	visitor.visit(this);
    }
}
