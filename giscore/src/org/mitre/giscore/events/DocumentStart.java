/****************************************************************************************
 *  DocumentStart.java
 *
 *  Created: Jan 28, 2009
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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.Namespace;

/**
 * This tags the document with the source information of what format it came
 * from, useful for back end processors to decide on the meaning of various 
 * values.
 * 
 * @author DRAND
 *
 */
public class DocumentStart implements IGISObject {

    private static final long serialVersionUID = 1L;

	private DocumentType type;
	private final List<Namespace> namespaces = new ArrayList<Namespace>(); 

	/**
	 * Ctor
	 * @param type
	 */
	public DocumentStart(DocumentType type) {
		setType(type);
	}

	/**
	 * @return the type
	 */
	public DocumentType getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(DocumentType type) {
		this.type = type;
	}
	
    /**
	 * @return the namespaces
	 */
	public List<Namespace> getNamespaces() {
		return namespaces;
	}

	public void accept(IStreamVisitor visitor) {
    	visitor.visit(this);
    }
    
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
}
