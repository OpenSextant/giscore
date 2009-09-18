package org.mitre.giscore.output.esri;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.output.FeatureKey;
import org.mitre.giscore.output.IContainerNameStrategy;

/**
 * Strategy that uses the current "container" along with the geometry to
 * derive a name.
 */
public class BasicContainerNameStrategy implements
        IContainerNameStrategy {

    /*
       * (non-Javadoc)
       *
       * @see
       * org.mitre.giscore.output.IContainerNameStrategy#deriveContainerName
       * (java.util.List, org.mitre.giscore.output.FeatureKey)
       */
    public String deriveContainerName(List<String> path, FeatureKey key) {
        StringBuilder setname = new StringBuilder();

        setname.append(StringUtils.join(path, '_'));
        if (key.getGeoclass() != null) {
            if (setname.length() > 0)
                setname.append("_");
            setname.append(key.getGeoclass().getSimpleName());
        }
        String datasetname = setname.toString();
        datasetname = datasetname.replaceAll("\\s+", "_");

        return datasetname;
    }
}