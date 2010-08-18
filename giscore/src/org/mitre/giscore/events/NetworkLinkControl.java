package org.mitre.giscore.events;

import edu.umd.cs.findbugs.annotations.Nullable;
import org.mitre.giscore.IStreamVisitor;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.Date;

/**
 * Controls the behavior of files fetched by a NetworkLink. <p/>
 *
 * <h4>Notes/Limitations:</h4>
 *
 * TODO: Holder for NetworkLinkControl but does not yet hold the Update contents
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: May 20, 2009 3:47:51 PM
 */
public class NetworkLinkControl implements IGISObject {

    private static final long serialVersionUID = 1L;

	private Double minRefreshPeriod;
	private Double maxSessionLength;
	private String cookie;
	private String message;
	private String linkName;
	private String linkDescription;
	private String linkSnippet;
	private Date expires;
	private String targetHref; // from Update element
	private String updateType;
	private TaggedMap viewGroup; // Camera or LookAt

	// TODO: add Update details: need list of Update objects. replace single updateType value with list. 

	/*
	  <element name="NetworkLinkControl" type="kml:NetworkLinkControlType"/>
	  <complexType name="NetworkLinkControlType" final="#all">
		<sequence>
		  <element ref="kml:minRefreshPeriod" minOccurs="0"/>
		  <element ref="kml:maxSessionLength" minOccurs="0"/>
		  <element ref="kml:cookie" minOccurs="0"/>
		  <element ref="kml:message" minOccurs="0"/>
		  <element ref="kml:linkName" minOccurs="0"/>
		  <element ref="kml:linkDescription" minOccurs="0"/>
		  <element ref="kml:linkSnippet" minOccurs="0"/>
		  <element ref="kml:expires" minOccurs="0"/>
		  <element ref="kml:Update" minOccurs="0"/>
		  <element ref="kml:AbstractViewGroup" minOccurs="0"/>
		  <element ref="kml:NetworkLinkControlSimpleExtensionGroup" minOccurs="0"
			maxOccurs="unbounded"/>
		  <element ref="kml:NetworkLinkControlObjectExtensionGroup" minOccurs="0"
			maxOccurs="unbounded"/>
		</sequence>
	  </complexType>
	 */

	public NetworkLinkControl() {
        // empty constructor
	}

    @Nullable
	public Double getMinRefreshPeriod() {
		return minRefreshPeriod;
	}

	public void setMinRefreshPeriod(Double minRefreshPeriod) {
		this.minRefreshPeriod = minRefreshPeriod;
	}

    @Nullable
	public Double getMaxSessionLength() {
		return maxSessionLength;
	}

	public void setMaxSessionLength(Double maxSessionLength) {
		this.maxSessionLength = maxSessionLength;
	}

    @Nullable
	public String getCookie() {
		return cookie;
	}

	public void setCookie(String cookie) {
		this.cookie = cookie;
	}

    @Nullable
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

    @Nullable
	public String getLinkName() {
		return linkName;
	}

	public void setLinkName(String linkName) {
		this.linkName = linkName;
	}

    @Nullable
	public String getLinkDescription() {
		return linkDescription;
	}

	public void setLinkDescription(String linkDescription) {
		this.linkDescription = linkDescription;
	}

    @Nullable
	public String getLinkSnippet() {
		return linkSnippet;
	}

	public void setLinkSnippet(String linkSnippet) {
		this.linkSnippet = linkSnippet;
	}

    @Nullable
	public Date getExpires() {
		// Note: this exposes internal representation by returning reference to mutable object
		return expires;
	}

	public void setExpires(Date expires) {
        this.expires = expires == null ? null : (Date)expires.clone();
	}

    @Nullable
	public String getTargetHref() {
		return targetHref;
	}

	public void setTargetHref(String targetHref) {
		this.targetHref = targetHref;
	}

    @Nullable
	public String getUpdateType() {
		return updateType;
	}

	public void setUpdateType(String updateType) {
		this.updateType = updateType;
	}

    @Nullable
	public TaggedMap getViewGroup() {
		return viewGroup;
	}

	public void setViewGroup(TaggedMap viewGroup) {
		this.viewGroup = viewGroup;
	}

	public void accept(IStreamVisitor visitor) {
		visitor.visit(this);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.MULTI_LINE_STYLE);
	}

}
