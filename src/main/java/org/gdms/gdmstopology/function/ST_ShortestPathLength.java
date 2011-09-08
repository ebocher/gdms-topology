package org.gdms.gdmstopology.function;

import org.gdms.data.DataSourceFactory;
import org.gdms.data.SQLDataSourceFactory;
import org.gdms.data.schema.DefaultMetadata;
import org.gdms.data.schema.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DriverException;
import org.gdms.driver.DataSet;
import org.gdms.gdmstopology.model.DWMultigraphDataSource;
import org.gdms.gdmstopology.model.GraphEdge;
import org.gdms.gdmstopology.model.GraphSchema;
import org.gdms.gdmstopology.model.WMultigraphDataSource;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;
import org.gdms.sql.function.table.AbstractTableFunction;
import org.gdms.sql.function.table.TableArgument;
import org.gdms.sql.function.table.TableDefinition;
import org.gdms.sql.function.table.TableFunctionSignature;
import org.orbisgis.progress.ProgressMonitor;
import org.jgrapht.traverse.ClosestFirstIterator;
import org.jgrapht.graph.EdgeReversedGraph;

/**
 *
 * @author ebocher
 */
public class ST_ShortestPathLength extends AbstractTableFunction {

        private DiskBufferDriver diskBufferDriver;

        @Override
        public DataSet evaluate(SQLDataSourceFactory dsf, DataSet[] tables, Value[] values, ProgressMonitor pm) throws FunctionException {
                int source = values[0].getAsInt();
                try {
                        DataSet sdsEdges = tables[0];
                        diskBufferDriver = new DiskBufferDriver(dsf, getMetadata(null));

                        if (values.length == 3) {
                                if (values[1].getAsBoolean()) {
                                        return computeWMPath(dsf, sdsEdges, source, pm);
                                } else {
                                        return computeDWMPath(dsf, sdsEdges, source, values[2].getAsBoolean(), pm);
                                }

                        } else {
                                return computeDWMPath(dsf, sdsEdges, source, false, pm);
                        }

                } catch (DriverException ex) {
                        throw new FunctionException("Cannot compute the shortest path length", ex);
                }


        }

        @Override
        public void workFinished() throws DriverException {
                if (diskBufferDriver != null) {
                        diskBufferDriver.stop();
                }
        }

        @Override
        public String getName() {
                return "ST_ShortestPathLength";
        }

        @Override
        public String getDescription() {
                return "Return the shortest path length beetwen one vertex to all other based on a directed graph. True if the path is computed using an undirected graph.";
        }

        @Override
        public String getSqlOrder() {
                return "SELECT * from ST_ShortestPathLength(table, 12[,true]) );";
        }

        @Override
        public Metadata getMetadata(Metadata[] tables) throws DriverException {
                Metadata md = new DefaultMetadata(
                        new Type[]{TypeFactory.createType(Type.INT), TypeFactory.createType(Type.INT), TypeFactory.createType(Type.INT), TypeFactory.createType(Type.DOUBLE)},
                        new String[]{GraphSchema.ID, GraphSchema.START_NODE, GraphSchema.END_NODE, GraphSchema.WEIGTH});
                return md;
        }

        @Override
        public FunctionSignature[] getFunctionSignatures() {
                return new FunctionSignature[]{
                                new TableFunctionSignature(TableDefinition.GEOMETRY, new TableArgument(TableDefinition.GEOMETRY), ScalarArgument.INT),
                                new TableFunctionSignature(TableDefinition.GEOMETRY, new TableArgument(TableDefinition.GEOMETRY), ScalarArgument.INT, ScalarArgument.BOOLEAN, ScalarArgument.BOOLEAN)
                        };
        }

        private DiskBufferDriver computeWMPath(DataSourceFactory dsf, DataSet sds, int source, ProgressMonitor pm) throws DriverException {
                WMultigraphDataSource wMultigraphDataSource = new WMultigraphDataSource(dsf, sds, pm);
                ClosestFirstIterator<Integer, GraphEdge> cl = new ClosestFirstIterator<Integer, GraphEdge>(
                        wMultigraphDataSource, source);
                //First point added
                diskBufferDriver.addValues(new Value[]{ValueFactory.createValue(source), ValueFactory.createValue(source), ValueFactory.createValue(source), ValueFactory.createValue(0)});

                int previous = source;
                while (cl.hasNext()) {
                        Integer node = cl.next();
                        if (node != source) {
                                double length = cl.getShortestPathLength(node);
                                diskBufferDriver.addValues(new Value[]{ValueFactory.createValue(source), ValueFactory.createValue(previous), ValueFactory.createValue(node), ValueFactory.createValue(length)});
                                previous = node;
                        }
                }
                diskBufferDriver.writingFinished();
                diskBufferDriver.start();
                return diskBufferDriver;


        }

        private DiskBufferDriver computeDWMPath(DataSourceFactory dsf, DataSet sds, int source, Boolean reverseGraph, ProgressMonitor pm) throws DriverException {
                DWMultigraphDataSource dwMultigraphDataSource = new DWMultigraphDataSource(dsf, sds, pm);
                ClosestFirstIterator<Integer, GraphEdge> cl;
                if (reverseGraph) {
                        EdgeReversedGraph edgeReversedGraph = new EdgeReversedGraph(dwMultigraphDataSource);
                        cl = new ClosestFirstIterator<Integer, GraphEdge>(
                                edgeReversedGraph, source);
                } else {
                        cl = new ClosestFirstIterator<Integer, GraphEdge>(
                                dwMultigraphDataSource, source);
                }
                //First point added
                diskBufferDriver.addValues(new Value[]{ValueFactory.createValue(source), ValueFactory.createValue(source), ValueFactory.createValue(source), ValueFactory.createValue(0)});

                int previous = source;
                while (cl.hasNext()) {
                        Integer node = cl.next();
                        if (node != source) {
                                double length = cl.getShortestPathLength(node);
                                diskBufferDriver.addValues(new Value[]{ValueFactory.createValue(source), ValueFactory.createValue(previous), ValueFactory.createValue(node), ValueFactory.createValue(length)});
                                previous = node;
                        }
                }
                diskBufferDriver.writingFinished();
                diskBufferDriver.start();
                return diskBufferDriver;
        }
}
