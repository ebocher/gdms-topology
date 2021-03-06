/**
 * GDMS-Topology  is a library dedicated to graph analysis. It is based on the JGraphT
 * library available at <http://www.jgrapht.org/>. It enables computing and processing
 * large graphs using spatial and alphanumeric indexes.
 *
 * This version is developed at French IRSTV institut as part of the
 * EvalPDU project, funded by the French Agence Nationale de la Recherche
 * (ANR) under contract ANR-08-VILL-0005-01 and GEBD project
 * funded by the French Ministery of Ecology and Sustainable Development.
 *
 * GDMS-Topology  is distributed under GPL 3 license. It is produced by the "Atelier SIG"
 * team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.
 *
 * Copyright (C) 2009-2012 IRSTV (FR CNRS 2488)
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
 * For more information, please consult: <http://wwwc.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.gdms.gdmstopology.function;

import org.gdms.data.DataSourceFactory;
import org.gdms.data.schema.Metadata;
import org.gdms.data.values.Value;
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DriverException;
import org.gdms.driver.DataSet;
import org.gdms.gdmstopology.model.GraphMetadataFactory;
import org.gdms.gdmstopology.model.GraphSchema;
import org.gdms.gdmstopology.process.GraphPath;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;
import org.gdms.sql.function.table.AbstractTableFunction;
import org.gdms.sql.function.table.TableArgument;
import org.gdms.sql.function.table.TableDefinition;
import org.gdms.sql.function.table.TableFunctionSignature;
import org.orbisgis.progress.ProgressMonitor;

/**
 *
 * @author Erwan Bocher
 */
public class ST_ShortestPath extends AbstractTableFunction {

        @Override
        public DataSet evaluate(DataSourceFactory dsf, DataSet[] tables, Value[] values, ProgressMonitor pm) throws FunctionException {
                try {
                        int source = values[0].getAsInt();
                        int target = values[1].getAsInt();
                        String costField = values[2].getAsString();
                        if (values.length == 4) {
                                DiskBufferDriver diskBufferDriver = GraphPath.getShortestPath(dsf, tables[0], source, target, costField, values[3].getAsInt(), pm);
                                diskBufferDriver.open();
                                return diskBufferDriver;

                        } else {
                                DiskBufferDriver diskBufferDriver = GraphPath.getShortestPath(dsf, tables[0], source, target, costField, GraphSchema.DIRECT, pm);
                                diskBufferDriver.open();
                                return diskBufferDriver;
                        }

                } catch (Exception ex) {
                        throw new FunctionException("Cannot compute the shortest path", ex);
                }
        }

        @Override
        public String getName() {
                return "ST_ShortestPath";
        }

        @Override
        public String getDescription() {
                return "Return the shortest path beetwen two vertexes using the Dijkstra algorithm.\n"
                        + "Optional argument : \n"
                        + "1 if the graph is directed ."
                        + "2 if the graph is directed and edges reversed\n"
                        + "3 if the graph is undirected\n";
        }

        @Override
        public String getSqlOrder() {
                return "SELECT * from  ST_ShortestPath(table,12, 10, costField [,1]);";
        }

        @Override
        public Metadata getMetadata(Metadata[] tables) throws DriverException {
                return GraphMetadataFactory.createEdgeMetadataShortestPath();
        }

        @Override
        public FunctionSignature[] getFunctionSignatures() {
                return new FunctionSignature[]{
                                new TableFunctionSignature(TableDefinition.GEOMETRY, new TableArgument(TableDefinition.GEOMETRY), ScalarArgument.INT, ScalarArgument.INT, ScalarArgument.STRING),
                                new TableFunctionSignature(TableDefinition.GEOMETRY, new TableArgument(TableDefinition.GEOMETRY), ScalarArgument.INT,
                                ScalarArgument.INT, ScalarArgument.STRING, ScalarArgument.INT)
                        };
        }
}
