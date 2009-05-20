package org.mitre.giscore.events;

import org.mitre.giscore.IStreamVisitor;

import java.util.Date;

/**
 * Controls the behavior of files fetched by a NetworkLink. <p/>
 *
 * Notes/Limitations: <p/>
 *
 * Holder for NetworkLinkControl but doe not yet hold the Update contents
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: May 20, 2009 3:47:51 PM
 */
public class NetworkLinkControl implements IGISObject {

	private Double minRefreshPeriod;
	private Double maxSessionLength;
	private String cookie;
	private String message;
	private String linkName;
	private String linkDescription;
	private String linkSnippet;
	private Date expires;
	private String targetHref; // from Update element
	
	// TODO: add  Update details

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
	}

	public Double getMinRefreshPeriod() {
		return minRefreshPeriod;
	}

	public void setMinRefreshPeriod(Double minRefreshPeriod) {
		this.minRefreshPeriod = minRefreshPeriod;
	}

	public Double getMaxSessionLength() {
		return maxSessionLength;
	}

	public void setMaxSessionLength(Double maxSessionLength) {
		this.maxSessionLength = maxSessionLength;
	}

	public String getCookie() {
		return cookie;
	}

	public void setCookie(String cookie) {
		this.cookie = cookie;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getLinkName() {
		return linkName;
	}

	public void setLinkName(String linkName) {
		this.linkName = linkName;
	}

	public String getLinkDescription() {
		return linkDescription;
	}

	public void setLinkDescription(String linkDescription) {
		this.linkDescription = linkDescription;
	}

	public String getLinkSnippet() {
		return linkSnippet;
	}

	public void setLinkSnippet(String linkSnippet) {
		this.linkSnippet = linkSnippet;
	}

	public Date getExpires() {
		return expires;
	}

	public void setExpires(Date expires) {
		this.expires = expires;
	}

	public String getTargetHref() {
		return targetHref;
	}

	public void setTargetHref(String targetHref) {
		this.targetHref = targetHref;
	}

	public void accept(IStreamVisitor visitor) {
		visitor.visit(this);
	}
}
