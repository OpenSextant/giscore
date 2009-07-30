/****************************************************************************************
 *  PointOffsetVisitor.java
 *
 *  Created: Jul 30, 2009
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
package org.mitre.giscore.output.shapefile;

import java.util.ArrayList;
import java.util.List;

import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.input.shapefile.PolygonCountingVisitor;
import org.mitre.giscore.output.StreamVisitorBase;

/**
 * Figure out the point offsets for the components in the point array
 * and the total point count as well. 
 * 
 * @author DRAND
 *
 */
public class PointOffsetVisitor extends StreamVisitorBase {
	private PolygonCountingVisitor pv = new PolygonCountingVisitor();
	private List<Integer> offsets = new ArrayList<Integer>();
	private int total = 0;
	
	@Override
	public void visit(LinearRing ring) {
		offsets.add(total);
		ring.accept(pv);
		total += pv.getPointCount();
		pv.resetCount();
	}	

	@Override
	public void visit(Line line) {
		offsets.add(total);
		total += line.getNumPoints();
	}

	public int getTotal() {
		return total;
	}

	public List<Integer> getOffsets() {
		return offsets;
	}
}
