package org.mitre.giscore.output.shapefile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.events.Comment;
import org.mitre.giscore.events.ContainerEnd;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.DocumentStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.GroundOverlay;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.NetworkLink;
import org.mitre.giscore.events.NetworkLinkControl;
import org.mitre.giscore.events.PhotoOverlay;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.ScreenOverlay;
import org.mitre.giscore.events.Style;
import org.mitre.giscore.events.StyleMap;
import org.mitre.giscore.geometry.Circle;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.GeometryBag;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.Model;
import org.mitre.giscore.geometry.MultiLine;
import org.mitre.giscore.geometry.MultiLinearRings;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.MultiPolygons;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.output.FeatureKey;
import org.mitre.giscore.output.FeatureSorter;
import org.mitre.giscore.output.IContainerNameStrategy;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.output.esri.BasicContainerNameStrategy;
import org.mitre.giscore.utils.ObjectBuffer;
import org.mitre.giscore.utils.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Output stream for shapefile creation. The basic output routines are lifted
 * from the transfusion mediate package.
 * 
 * @author DRAND
 *
 */
public class ShapefileOutputStream extends ShapefileBaseClass implements IGISOutputStream, IStreamVisitor {
	private static final Logger logger = LoggerFactory.getLogger(ShapefileOutputStream.class);
	
	/**
	 * The feature sorter takes care of the details of storing features for
	 * later retrieval by schema.
	 */
	private FeatureSorter sorter = new FeatureSorter();
	
	/**
	 * Tracks the path - useful for naming collections
	 */
	private Stack<String> path = new Stack<String>();
	
	/**
	 * The first time we find a particular feature key, we store away the 
	 * path and geometry type as a name. Not perfect, but at least it will
	 * be somewhat meaningful.
	 */
	private Map<FeatureKey, String> datasets = new HashMap<FeatureKey, String>();
	
	/**
	 * Style id to style map
	 */
	private Map<String, Style> styles = new HashMap<String, Style>();
	
	/**
	 * Style id to specific style. This info is inferred from style map
	 * elements.
	 */
	private Map<String, String> styleMappings = new HashMap<String, String>();

	/**
	 * Container naming strategy, never null after ctor
	 */
	private IContainerNameStrategy containerNameStrategy;

	/**
	 * Stream to hold output data
	 */
	private ZipOutputStream outputStream;

	/**
	 * Place to create the output files. The parent of this must exist.
	 */
	private File outputPath;

	/**
	 * Maps style icon references to esri shape ids
	 */
	private PointShapeMapper mapper = new PointShapeMapper();
	
    /**
     * Ctor
     *
     * @param stream                the output stream to write the resulting GDB into, never
     *                              <code>null</code>.
     * @param path                  the directory and file that should hold the file gdb, never
     *                              <code>null</code>.
     * @param containerNameStrategy a name strategy to override the default, may be
     *                              <code>null</code>.
     * @param mapper				point to shape mapper
     * @throws IOException if an IO error occurs
     */
    public ShapefileOutputStream(OutputStream stream, File path,
                           IContainerNameStrategy containerNameStrategy,
                           PointShapeMapper mapper) {
    	if (stream == null) {
			throw new IllegalArgumentException("stream should never be null");
		}
    	if (!(stream instanceof ZipOutputStream)) {
    		throw new IllegalArgumentException("stream must be a zip output stream");
    	}
        if (path == null || !path.getParentFile().exists()) {
            throw new IllegalArgumentException(
                    "path should never be null and parent must exist");
        }
        if (containerNameStrategy == null) {
            this.containerNameStrategy = new BasicContainerNameStrategy();
        } else {
            this.containerNameStrategy = containerNameStrategy;
        }    	
        
        outputStream = (ZipOutputStream) stream;
        outputPath = path;
        if (mapper != null) this.mapper = mapper;
    }
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.IGISOutputStream#write(org.mitre.giscore.events.IGISObject)
	 */
	public void write(IGISObject object) {
		object.accept(this);
	}

	@Override
	public void close() throws IOException {
		for(FeatureKey key : sorter.keys()) {
			ObjectBuffer buffer = sorter.getBuffer(key);
			String pathstr = key.getPath();
			String pieces[] = pathstr.split("_");
			List<String> path = Arrays.asList(pieces);
			try {
				String cname = containerNameStrategy.deriveContainerName(path, key);
				Style style = null;
				if (key.getStyleRef() != null) {
					String id = key.getStyleRef();
					if (styleMappings.get(id) != null) {
						id = styleMappings.get(id);
					}
					style = styles.get(id);
				}
				SingleShapefileOutputHandler soh = 
					new SingleShapefileOutputHandler(key.getSchema(), style, buffer, outputPath, cname, mapper);
				soh.process();
			} catch (Exception e) {
				logger.error("Problem reifying data from stream",e);
			}
		}
		sorter.cleanup();
		ZipUtils.outputZipComponents(outputPath.getName(), outputPath, (ZipOutputStream) outputStream);
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events.ContainerEnd)
	 */
	@Override
	public void visit(ContainerEnd containerEnd) {
		path.pop();
		
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events.ContainerStart)
	 */
	@Override
	public void visit(ContainerStart containerStart) {
		path.push(containerStart.getName());
	}

	@Override
	public void visit(Style style) {
		styles.put(style.getId(), style);
	}

    /*
     * (non-Javadoc)
     *
     * @see org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events.Schema
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
	 * .Feature)
	 */
	@Override
	public void visit(Feature feature) {
		// Skip non-geometry features
		if (feature.getGeometry() == null) return;
		String fullpath = path != null ? StringUtils.join(path, '_') : null;
		FeatureKey key = sorter.add(feature, fullpath);
		if (datasets.get(key) == null) {
			StringBuilder setname = new StringBuilder();
			setname.append(fullpath);
			if (key.getGeoclass() != null) {
				setname.append("_");
				setname.append(key.getGeoclass().getSimpleName());
			}
			String datasetname = setname.toString();
			datasetname = datasetname.replaceAll("\\s", "_");
			datasets.put(key, datasetname);
		}
	}

	@Override
	public void visit(StyleMap styleMap) {
		String id = styleMap.get("normal");
		if (id != null && id.startsWith("#")) {
			styleMappings.put(styleMap.getId(), id.substring(1));
		}
	}


}
