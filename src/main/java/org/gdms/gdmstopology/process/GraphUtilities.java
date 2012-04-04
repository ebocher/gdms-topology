/**
 * GDMS-Topology  is a library dedicated to graph analysis. It's based on the JGraphT available at
 * http://www.jgrapht.org/. It ables to compute and process large graphs using spatial
 * and alphanumeric indexes.
 * This version is developed at French IRSTV institut as part of the
 * EvalPDU project, funded by the French Agence Nationale de la Recherche
 * (ANR) under contract ANR-08-VILL-0005-01 and GEBD project
 * funded by the French Ministery of Ecology and Sustainable Development .
 *
 * GDMS-Topology  is distributed under GPL 3 license. It is produced by the "Atelier SIG" team of
 * the IRSTV Institute <http://www.irstv.cnrs.fr/> CNRS FR 2488.
 *
 * GDMS-Topology is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * GDMS-Topology is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GDMS-Topology. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://trac.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.gdms.gdmstopology.process;

import com.vividsolutions.jts.geom.Geometry;
import java.util.Iterator;
import org.gdms.data.SQLDataSourceFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DataSet;
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DriverException;
import org.gdms.gdmstopology.model.DWMultigraphDataSource;
import org.gdms.gdmstopology.model.EdgeReversedGraphDataSource;
import org.gdms.gdmstopology.model.GDMSValueGraph;
import org.gdms.gdmstopology.model.GraphEdge;
import org.gdms.gdmstopology.model.GraphException;
import org.gdms.gdmstopology.model.GraphMetadataFactory;
import org.gdms.gdmstopology.model.GraphSchema;
import org.gdms.gdmstopology.model.WMultigraphDataSource;
import org.jgrapht.traverse.ClosestFirstIterator;
import org.orbisgis.progress.ProgressMonitor;

/**
 *
 * @author ebocher IRSTV FR CNRS 2488
 */
public class GraphUtilities extends GraphAnalysis {

        /**
         * Return all reachable edges from a node.
         * A radius can be used to limit the area.
         * 
         * @param dsf
         * @param dataSet
         * @param source
         * @param costField
         * @param radius
         * @param graphType
         * @param pm
         * @return
         * @throws GraphException
         * @throws DriverException 
         */
        public static DiskBufferDriver getReachableEdges(SQLDataSourceFactory dsf, DataSet dataSet, int source, String costField, double radius, int graphType, ProgressMonitor pm) throws GraphException, DriverException {
                if (graphType == GraphSchema.DIRECT) {
                        DWMultigraphDataSource dwMultigraphDataSource = new DWMultigraphDataSource(dsf, dataSet, pm);
                        dwMultigraphDataSource.setWeigthFieldIndex(costField);
                        return findReachableEdges(dsf, dwMultigraphDataSource, source, Double.POSITIVE_INFINITY, pm);
                } else if (graphType == GraphSchema.DIRECT_REVERSED) {
                        DWMultigraphDataSource dwMultigraphDataSource = new DWMultigraphDataSource(dsf, dataSet, pm);
                        dwMultigraphDataSource.setWeigthFieldIndex(costField);
                        EdgeReversedGraphDataSource edgeReversedGraph = new EdgeReversedGraphDataSource(dwMultigraphDataSource);
                        return findReachableEdges(dsf, edgeReversedGraph, source, Double.POSITIVE_INFINITY, pm);
                } else if (graphType == GraphSchema.UNDIRECT) {
                        WMultigraphDataSource wMultigraphDataSource = new WMultigraphDataSource(dsf, dataSet, pm);
                        wMultigraphDataSource.setWeigthFieldIndex(costField);
                        return findReachableEdges(dsf, wMultigraphDataSource, source, Double.POSITIVE_INFINITY, pm);
                } else {
                        throw new GraphException("Only 3 type of graphs are allowed."
                                + "1 if the path is computing using a directed graph.\n"
                                + "2 if the path is computing using a directed graph and edges are reversed\n"
                                + "3 if the path is computing using a undirected.");
                }
        }

        /**
         * Return all reachable edges from a node.
         * A radius can be used to limit the area.
         * @param dsf
         * @param graph
         * @param source
         * @param radius 
         * @param pm 
         * @return
         * @throws DriverException 
         */
        public static DiskBufferDriver findReachableEdges(SQLDataSourceFactory dsf, GDMSValueGraph<Integer, GraphEdge> graph,
                Integer source, double radius, ProgressMonitor pm) throws DriverException, GraphException {

                if (!graph.containsVertex(source)) {
                        throw new GraphException(
                                "The graph must contain the source vertex");
                }

                DiskBufferDriver diskBufferDriver = new DiskBufferDriver(dsf, GraphMetadataFactory.createReachablesEdgesMetadata());
                ClosestFirstIterator<Integer, GraphEdge> cl = new ClosestFirstIterator<Integer, GraphEdge>(
                        graph, source);
                int count = 0;
                pm.startTask("Find reachable edges", 100);

                while (cl.hasNext()) {
                        if (count >= 100 && count % 100 == 0) {
                                if (pm.isCancelled()) {
                                        break;
                                }
                        }
                        count++;
                        Integer node = cl.next();
                        if (node != source) {
                                double length = cl.getShortestPathLength(node);
                                GraphEdge edge = cl.getSpanningTreeEdge(node);
                                Geometry geom = graph.getGeometry(edge);
                                diskBufferDriver.addValues(new Value[]{ValueFactory.createValue(geom),
                                                ValueFactory.createValue(edge.getRowId()),
                                                ValueFactory.createValue(edge.getWeight()), ValueFactory.createValue(length)});

                        }
                }
                diskBufferDriver.writingFinished();
                diskBufferDriver.start();
                pm.endTask();
                return diskBufferDriver;

        }

        /**
         * Return all reachable edges from several nodes.
         * A radius can be used to limit the area.
         * 
         * @param dsf
         * @param dataSet
         * @param nodes
         * @param costField
         * @param radius
         * @param graphType
         * @param pm
         * @return
         * @throws GraphException
         * @throws DriverException 
         */
        public static DiskBufferDriver getMReachableEdges(SQLDataSourceFactory dsf, DataSet dataSet, DataSet nodes, String costField, double radius, int graphType, ProgressMonitor pm) throws GraphException, DriverException {
                if (graphType == GraphSchema.DIRECT) {
                        DWMultigraphDataSource dwMultigraphDataSource = new DWMultigraphDataSource(dsf, dataSet, pm);
                        dwMultigraphDataSource.setWeigthFieldIndex(costField);
                        return findMReachableEdges(dsf, dwMultigraphDataSource, nodes, Double.POSITIVE_INFINITY, pm);
                } else if (graphType == GraphSchema.DIRECT_REVERSED) {
                        DWMultigraphDataSource dwMultigraphDataSource = new DWMultigraphDataSource(dsf, dataSet, pm);
                        dwMultigraphDataSource.setWeigthFieldIndex(costField);
                        EdgeReversedGraphDataSource edgeReversedGraph = new EdgeReversedGraphDataSource(dwMultigraphDataSource);
                        return findMReachableEdges(dsf, edgeReversedGraph, nodes, Double.POSITIVE_INFINITY, pm);
                } else if (graphType == GraphSchema.UNDIRECT) {
                        WMultigraphDataSource wMultigraphDataSource = new WMultigraphDataSource(dsf, dataSet, pm);
                        wMultigraphDataSource.setWeigthFieldIndex(costField);
                        return findMReachableEdges(dsf, wMultigraphDataSource, nodes, Double.POSITIVE_INFINITY, pm);
                } else {
                        throw new GraphException("Only 3 type of graphs are allowed."
                                + "1 if the path is computing using a directed graph.\n"
                                + "2 if the path is computing using a directed graph and edges are reversed\n"
                                + "3 if the path is computing using a undirected.");
                }
        }

        /**
         * Return all reachable edges from several nodes.
         * A radius can be used to limit the area.
         * @param dsf
         * @param graph
         * @param nodes
         * @param radius 
         * @param pm 
         * @return
         * @throws DriverException 
         */
        public static DiskBufferDriver findMReachableEdges(SQLDataSourceFactory dsf, GDMSValueGraph<Integer, GraphEdge> graph,
                DataSet nodes, double radius, ProgressMonitor pm) throws DriverException, GraphException {

                if (checkSourceColumn(nodes)) {
                        Iterator<Value[]> it = nodes.iterator();
                        DiskBufferDriver diskBufferDriver = new DiskBufferDriver(dsf, GraphMetadataFactory.createMReachablesEdgesMetadata());

                        while (it.hasNext()) {
                                Value[] values = it.next();
                                int source = values[SOURCE_FIELD_INDEX].getAsInt();
                                if (!graph.containsVertex(source)) {
                                        throw new GraphException(
                                                "The graph must contain the source vertex");
                                }
                                ClosestFirstIterator<Integer, GraphEdge> cl = new ClosestFirstIterator<Integer, GraphEdge>(
                                        graph, source);
                                int count = 0;
                                pm.startTask("Find reachable edges", 100);

                                while (cl.hasNext()) {
                                        if (count >= 100 && count % 100 == 0) {
                                                if (pm.isCancelled()) {
                                                        break;
                                                }
                                        }
                                        count++;
                                        Integer node = cl.next();
                                        if (node != source) {
                                                double length = cl.getShortestPathLength(node);
                                                GraphEdge edge = cl.getSpanningTreeEdge(node);
                                                Geometry geom = graph.getGeometry(edge);
                                                diskBufferDriver.addValues(new Value[]{ValueFactory.createValue(geom),
                                                                ValueFactory.createValue(edge.getRowId()), ValueFactory.createValue(source),
                                                                ValueFactory.createValue(edge.getWeight()), ValueFactory.createValue(length)});

                                        }
                                }
                        }
                        diskBufferDriver.writingFinished();
                        diskBufferDriver.start();
                        pm.endTask();
                        return diskBufferDriver;
                } else {
                        throw new GraphException("The table nodes must contains the column source");
                }
        }
}
