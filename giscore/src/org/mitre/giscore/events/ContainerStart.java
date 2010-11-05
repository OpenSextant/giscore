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

import java.io.IOException;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.apache.commons.lang.StringUtils;

/**
 * We've seen the start of a container. A container can hold zero or more features
 * and zero or more sub containers.
 * 
 * @author DRAND
 */
public class ContainerStart extends Common implements IContainerType {
	private static final long serialVersionUID = 1L;
	
	private String type;

    private boolean open;

    /**
	 * Empty ctor for data IO.  Constructor must be followed by call to {@code readData()}
     * to initialize the object instance otherwise object is invalid.
	 */
	public ContainerStart() {
		// 
	}
	
    /**
     * Constructs a container that can hold zero or more features or other containers.
     * <code>ContainerStart</code> should be followed by a matching <code>ContainerEnd</code> element. 
     * @param type Type of container
     * @throws IllegalArgumentException if type is null or empty 
     */
	public ContainerStart(String type) {
		setType(type);
	}

	/**
	 * @return the type, never null
	 */
    @NonNull
	public String getType() {
		return type;
	}

	/**
	 * Set type of container (e.g. Folder, Document)
	 * @param type the type to set
	 * @throws IllegalArgumentException if type is null or empty
	 */
	public void setType(String type) {
		if (StringUtils.isBlank(type)) {
			throw new IllegalArgumentException("type should never be null or empty");
		}
		this.type = type;
	}

	/**
	 * Specifies whether a Container (Document or Folder) appears closed or open when first loaded into the Places panel.
	 * false=collapsed (the default), true=expanded. This element applies only to Document, Folder, and NetworkLink. 
	 * @return whether the Container appears open or closed
	 */
    public boolean isOpen() {
        return open;
    }

	/**
	 * Set open flag.
	 * @param open True if container appears open when first loaded
	 */
    public void setOpen(boolean open) {
        this.open = open;
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
        StringBuilder b = new StringBuilder(super.toString());
        if (b.length() != 0 && b.charAt(b.length() - 1) != '\n') {
            b.append('\n');
        }
        b.append("  type=").append(type);
        return b.toString(); 
    }
	
    public void accept(IStreamVisitor visitor) {
    	visitor.visit(this);
    }

	/* (non-Javadoc)
	 * @see org.mitre.giscore.events.BaseStart#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		super.readData(in);
		type = in.readString();
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.events.BaseStart#writeData(org.mitre.giscore.utils.SimpleObjectOutputStream)
	 */
	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {	
		super.writeData(out);
		out.writeString(type);
	}
    
}
