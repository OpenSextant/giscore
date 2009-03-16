package org.mitre.giscore.events;

import org.mitre.giscore.output.StreamVisitorBase;
import java.io.Serializable;

/**
 * Represents a Comment object. In XML would
 * be represented as <!-- text content -->.
 * 
 * @author Jason Mathews, MITRE Corp.
 * Created: Mar 10, 2009 9:03:48 AM
 */
public class Comment implements IGISObject, Serializable {

	private static final long serialVersionUID = 1L;

	private String text;

	/**
     * Default, no-args constructor for implementations to use if needed.
     */
    protected Comment() {}

	/**
	 * This creates the comment with the supplied text.
     *
     * @param text <code>String</code> content of comment.
	 */
	public Comment(String text) {
		setText(text);
	}

	/**
	 * This returns the textual data within the <code>Comment</code>.
     *
     * @return the text of this comment
     */
	public String getText() {
		return text;
	}

	/**
	 * This will set the value of the <code>Comment</code>.
     *
     * @param text <code>String</code> text for comment.
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Visit the object
	 *
	 * @param visitor the visitor to dispatch to, never <code>null</code>
	 */
	public void accept(StreamVisitorBase visitor) {
		visitor.visit(this);
	}

	/**
	 * This returns a <code>String</code> representation of the
	 * <code>Comment</code>, suitable for debugging.
	 *
	 * @return <code>String</code> - information about the
	 *         <code>Comment</code>
	 */
	public String toString() {
		return new StringBuilder()
			.append("[Comment: ")
			.append(text)
			.append(']')
			.toString();
	}

}
