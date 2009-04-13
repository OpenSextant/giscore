package org.mitre.giscore.output;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.mitre.giscore.events.Common;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.geometry.Geometry;

/**
 * A key used to look up a specific temp file. 
 * 
 * @author DRAND
 */
public class FeatureKey {
	private Schema schema;
	private Class<? extends Geometry> geoclass;
	private Class<? extends Common> featureClass;
	
	public FeatureKey(Schema schema, Class<? extends Geometry> geoclass, 
			Class<? extends Common> featureClass) {
		if (schema == null) {
			throw new IllegalArgumentException(
					"schema should never be null");
		}
		if (featureClass == null) {
			throw new IllegalArgumentException(
					"featureClass should never be null");
		}
		this.featureClass = featureClass;
		this.schema = schema;
		this.geoclass = geoclass;
	}

	/**
	 * @return the schema
	 */
	public Schema getSchema() {
		return schema;
	}

	/**
	 * @return the geoclass
	 */
	public Class<? extends Geometry> getGeoclass() {
		return geoclass;
	}
	
	/**
	 * @return the featureClass
	 */
	public Class<? extends Common> getFeatureClass() {
		return featureClass;
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