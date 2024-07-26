package uk.ac.manchester.tornado.unittests.tasks;

import org.junit.Test;

import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.common.TornadoDevice;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;
import uk.ac.manchester.tornado.api.exceptions.TornadoExecutionPlanException;
import uk.ac.manchester.tornado.api.types.collections.VectorFloat8;
import uk.ac.manchester.tornado.api.types.vectors.Float8;
import uk.ac.manchester.tornado.unittests.common.TornadoTestBase;

/**
 * How to test?
 * <code>
 * tornado-test -V uk.ac.manchester.tornado.unittests.tasks.TestMultiTasks
 * </code>
 */
public class TestMultiTasks extends TornadoTestBase {

    public static void parallelInitialization(VectorFloat8 data) {
        for (@Parallel int i = 0; i < data.size(); i++) {
            int j = i * 8;
            data.set(i, new Float8(j, j + 1, j + 2, j + 3, j + 4, j + 5, j + 6, j + 7));
        }
    }

    public static void computeSquare(VectorFloat8 data) {
        for (@Parallel int i = 0; i < data.size(); i++) {
            Float8 item = data.get(i);
            Float8 result = Float8.mult(item, item);
            data.set(i, result);
        }
    }

    @Test
    public void test() throws TornadoExecutionPlanException {
        VectorFloat8 array = new VectorFloat8(1024 * 8);
        TaskGraph taskGraph = new TaskGraph("s0") //
                .transferToDevice(DataTransferMode.EVERY_EXECUTION, array) //
                .task("t0", TestMultiTasks::parallelInitialization, array) //
                .task("t1", TestMultiTasks::computeSquare, array) //
                .transferToHost(DataTransferMode.EVERY_EXECUTION, array);
        try (TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(taskGraph.snapshot())) {

            TornadoDevice device = TornadoExecutionPlan.getDevice(0, 0);
            executionPlan.withDevice(device);

            executionPlan.execute();
        }
    }
}
