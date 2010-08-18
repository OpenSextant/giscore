/****************************************************************************************
 *  NetworkLink.java
 *
 *  Created: Feb 4, 2009
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

import edu.umd.cs.findbugs.annotations.Nullable;
import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

/**
 * Represents a remote resource
 * 
 * @author DRAND
 */
public class NetworkLink extends Feature {

	private static final long serialVersionUID = 1L;
	private boolean refreshVisibility = false;
	private boolean flyToView = false;
	private TaggedMap link;
	
	/* (non-Javadoc)
	 * @see org.mitre.giscore.events.Feature#getType()
	 */
	@Override
	public String getType() {
		return IKml.NETWORK_LINK;
	}

	/**
	 * @return the refreshVisibility
	 */
	public boolean isRefreshVisibility() {
		return refreshVisibility;
	}

	/**
	 * @param refreshVisibility the refreshVisibility to set
	 */
	public void setRefreshVisibility(boolean refreshVisibility) {
		this.refreshVisibility = refreshVisibility;
	}

	/**
	 * @return the flyToView
	 */
	public boolean isFlyToView() {
		return flyToView;
	}

	/**
	 * @param flyToView the flyToView to set
	 */
	public void setFlyToView(boolean flyToView) {
		this.flyToView = flyToView;
	}

	/**
     * Get Link property TaggedMap. TaggedMap includes the child elements
     * associated with the networkLink Link or Url tag which may contain
     * any of the following attributes: refreshMode, refreshInterval,
     *  viewRefreshMode, viewRefreshTime, viewBoundScale, viewFormat, httpQuery.
	 * @return the link
	 */
    @Nullable
	public TaggedMap getLink() {
		return link;
	}

	/**
	 * @param link the link to set
	 */
	public void setLink(TaggedMap link) {
		this.link = link;
	}
	
    public void accept(IStreamVisitor visitor) {
    	visitor.visit(this);
    }
	
	/* (non-Javadoc)
	 * @see org.mitre.giscore.events.Feature#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		super.readData(in);
		flyToView = in.readBoolean();
		refreshVisibility = in.readBoolean();
		link = (TaggedMap) in.readObject();
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.events.Feature#writeData(org.mitre.giscore.utils.SimpleObjectOutputStream)
	 */
	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		super.writeData(out);
		out.writeBoolean(flyToView);
		out.writeBoolean(refreshVisibility);
		out.writeObject(link);
	}
}
