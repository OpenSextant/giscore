package org.mitre.giscore.events;

import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.giscore.utils.IDataSerializable;

import java.io.IOException;

/**
 * In reading a GISStream sometimes elements are sometimes out of order and a
 * straight-forward processing could encounter errors so such elements that
 * require special handling are wrapped in a <code>WrappedObject</code>.
 * Without explicit handling these are treated as <code>Comment</code>
 * objects and generally ignored or passed around without any processing.
 *  
 * @author Jason Mathews, MITRE Corp.
 * Date: Nov 4, 2009 8:54:09 AM
 */
public class WrappedObject extends Comment implements IDataSerializable {

	private static final long serialVersionUID = 1L;

	private IGISObject wrappedObject;

	/**
	 * Empty ctor for data IO
	 */
	public WrappedObject() {
	}

	public WrappedObject(IGISObject obj) {
		wrappedObject = obj;
	}

	/**
	 * This returns the textual data within the <code>Comment</code>.
     *
     * @return the text of this comment
     */
	public String getText() {
		StringBuilder sb = new StringBuilder()
				.append("[Comment: ");
		if (wrappedObject != null)
			sb.append('\n')
					.append(ToStringBuilder.reflectionToString(wrappedObject, ToStringStyle.SHORT_PREFIX_STYLE));
		return sb.append(']').toString();
	}

	/**
	 * Get wrapped IGISObject instance that this element includes
	 * @return wrapped object
	 */
	public IGISObject getObject() {
		return wrappedObject;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.mitre.giscore.events.BaseStart#readData(org.mitre.giscore.utils.
	 * SimpleObjectInputStream)
	 */
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		wrappedObject = (IGISObject) in.readObject();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.mitre.giscore.events.BaseStart#writeData(java.io.DataOutputStream)
	 */
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		if (wrappedObject instanceof IDataSerializable)
			out.writeObject((IDataSerializable)wrappedObject);
	}

}
