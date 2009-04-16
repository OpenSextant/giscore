/****************************************************************************************
 *  SortingOutputStream.java
 *
 *  Created: Apr 16, 2009
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
package org.mitre.giscore.output;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.ICategoryNameExtractor;
import org.mitre.giscore.events.Comment;
import org.mitre.giscore.events.ContainerEnd;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.DocumentStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.Style;
import org.mitre.giscore.events.StyleMap;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an output stream that takes information in the Rows or Row subclasses
 * handed to it in order to place them in different containers. It will reorder
 * the data by using a feature sorter to hold data for each collection. On close
 * it will call the actual output stream and create the resulting file.
 * 
 * @author DRAND
 * 
 */
public class SortingOutputStream extends StreamVisitorBase implements
		IGISOutputStream {
	private static final Logger logger = LoggerFactory.getLogger(SortingOutputStream.class);
	
	/**
	 * The feature sorter
	 */
	private FeatureSorter sorter = new FeatureSorter();

	/**
	 * The gis output stream, assigned in the ctor and never changed afterward.
	 * This is never <code>null</code>.
	 */
	private IGISOutputStream stream = null;

	/**
	 * The name strategy to use for creating containers, assigned in the ctor
	 * and never changed afterward. This is never <code>null</code>.
	 */
	private IContainerNameStrategy strategy = null;

	/**
	 * Tracks the path - useful for naming collections
	 */
	private List<String> path = new ArrayList<String>();
	
	/**
	 * Outer name, pick up from the first container
	 */
	private String outer = null;

	/**
	 * The extractor that will determine the name of a category based on the
	 * actual data in the row or row subclass.
	 */
	private ICategoryNameExtractor extractor = null;
	
	/**
	 * Ctor
	 * 
	 * @param innerstream
	 * @param strategy
	 */
	public SortingOutputStream(IGISOutputStream innerstream,
			IContainerNameStrategy strategy, ICategoryNameExtractor extractor) {
		if (innerstream == null) {
			throw new IllegalArgumentException(
					"innerstream should never be null");
		}
		if (strategy == null) {
			throw new IllegalArgumentException("strategy should never be null");
		}
		if (extractor == null) {
			throw new IllegalArgumentException("extractor should never be null");
		}
		this.stream = innerstream;
		this.strategy = strategy;
		this.extractor = extractor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.IGISOutputStream#write(org.mitre.giscore.events
	 * .IGISObject)
	 */
	@Override
	public void write(IGISObject object) {
		object.accept(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .Comment)
	 */
	@Override
	public void visit(Comment comment) {
		stream.write(comment);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .ContainerEnd)
	 */
	@Override
	public void visit(ContainerEnd containerEnd) {
		if (path.size() > 0) {
			path.remove(path.size() - 1);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .ContainerStart)
	 */
	@Override
	public void visit(ContainerStart containerStart) {
		if (outer == null) {
			outer = containerStart.getName();
		} else {
			path.add(containerStart.getName());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .DocumentStart)
	 */
	@Override
	public void visit(DocumentStart documentStart) {
		stream.write(documentStart);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .Feature)
	 */
	@Override
	public void visit(Feature feature) {
		visit((Row) feature);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .Row)
	 */
	@Override
	public void visit(Row row) {
		String category = extractor.extractCategoryName(row);
		String fullpath = null;
		if (path.size() > 0) {
			fullpath = StringUtils.join(path, '_') + "_" + category;
		} else {
			fullpath = category;
		}
		sorter.add(row, fullpath);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .Schema)
	 */
	@Override
	public void visit(Schema schema) {
		sorter.add(schema);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .Style)
	 */
	@Override
	public void visit(Style style) {
		stream.write(style);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .StyleMap)
	 */
	@Override
	public void visit(StyleMap styleMap) {
		stream.write(styleMap);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		Collection<FeatureKey> keys = sorter.keys();
		stream.write(new DocumentStart(DocumentType.KML));
		ContainerStart outercontainer = new ContainerStart("Document");
		if (StringUtils.isNotBlank(outer)) outercontainer.setName(outer);
		stream.write(outercontainer);
		for(FeatureKey key : keys) {
			File file = sorter.getFeatureFile(key);
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(file);
				SimpleObjectInputStream sois = new SimpleObjectInputStream(fis);
				ContainerStart cs = new ContainerStart("Folder");
				cs.setName(key.getPath());
				stream.write(cs);
				IGISObject obj = (IGISObject) sois.readObject();
				do {
					stream.write(obj);
					obj = (IGISObject) sois.readObject();
				} while(obj != null);
				ContainerEnd ce = new ContainerEnd();
				stream.write(ce);
			} catch (Exception e) {
				logger.error("Problem reifying data from stream",e);
			} finally {
				IOUtils.closeQuietly(fis);
			}
		}
		sorter.cleanup();
		stream.close();
	}

}
