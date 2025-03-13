/*
 * Copyright (c) 2025, APT Group, Department of Computer Science,
 * The University of Manchester.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.manchester.tornado.unittests.fields;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.TornadoExecutionResult;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;
import uk.ac.manchester.tornado.api.enums.ProfilerMode;
import uk.ac.manchester.tornado.api.exceptions.TornadoExecutionPlanException;
import uk.ac.manchester.tornado.api.types.arrays.FloatArray;
import uk.ac.manchester.tornado.unittests.common.TornadoTestBase;

/**
 * <p>
 * How to test?
 * </p>
 * <code>
 * tornado-test -pbc -V --fast --jvm="-Dtornado.device.memory=2GB" uk.ac.manchester.tornado.unittests.fields.TestStressFields
 * </code>
 */
public class TestStressFields extends TornadoTestBase {

    FloatArray arrayA;
    FloatArray arrayB;
    FloatArray arrayC;

    private void testStressFields() {
        for (@Parallel int i = 0; i < arrayC.getSize(); i++) {
            arrayC.set(i, arrayA.get(i) * arrayB.get(i));
        }
    }

    private void testStressFields(FloatArray arrayA, FloatArray arrayB, FloatArray arrayC) {
        for (@Parallel int i = 0; i < arrayA.getSize(); i++) {
            arrayC.set(i, arrayA.get(i) * arrayB.get(i));
        }
    }

    private static void testStressFieldsStatic(FloatArray arrayA, FloatArray arrayB, FloatArray arrayC) {
        for (@Parallel int i = 0; i < arrayA.getSize(); i++) {
            arrayC.set(i, arrayA.get(i) * arrayB.get(i));
        }
    }

    private static void testStressFieldsStatic(DataPackage dataPackage) {
        for (@Parallel int i = 0; i < dataPackage.arrayA.getSize(); i++) {
            dataPackage.arrayC.set(i, dataPackage.arrayA.get(i) * dataPackage.arrayB.get(i));
        }
    }

    private void testStressFieldsCombo(DataPackage dataPackage, FloatArray arrayB) {
        for (@Parallel int i = 0; i < dataPackage.arrayA.getSize(); i++) {
            arrayC.set(i, dataPackage.arrayA.get(i) * arrayB.get(i));
        }
    }

    private static class DataPackage {
        FloatArray arrayA;
        FloatArray arrayB;
        FloatArray arrayC;

        public DataPackage() {
            arrayA = new FloatArray(SIZE_OF_512MB / 4);
            arrayB = new FloatArray(SIZE_OF_512MB / 4);
            arrayC = new FloatArray(SIZE_OF_512MB / 4);
            arrayA.init(100.1f);
            arrayB.init(0.2f);
            arrayC.init(0.0f);
        }

        private void testStressFields() {
            for (@Parallel int i = 0; i < arrayA.getSize(); i++) {
                arrayC.set(i, arrayA.get(i) * arrayB.get(i));
            }
        }
    }

    @Test
    public void testStressMemoryFieldsA() {
        arrayA = new FloatArray(SIZE_OF_512MB / 4);
        arrayB = new FloatArray(SIZE_OF_512MB / 4);
        arrayC = new FloatArray(SIZE_OF_512MB / 4);

        arrayA.init(100.1f);
        arrayB.init(200.2f);
        arrayC.init(0.0f);

        // If we do not pass transfer to device or transfer to host, then
        // the following task-graphs works.
        // Otherwise, not all data is allocated, and bytecodes in TornadoVM
        // are on the wrong place when sending data back and forth.
        TaskGraph taskGraph = new TaskGraph("testStressMemoryFields") //
                .transferToDevice(DataTransferMode.EVERY_EXECUTION, arrayA, arrayB) //
                .task("fields", this::testStressFields) //
                .transferToHost(DataTransferMode.EVERY_EXECUTION, arrayC); //

        try (TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(taskGraph.snapshot())) {
            for (int i = 0; i < 100; i++) {
                TornadoExecutionResult execute = executionPlan //
                        .withProfiler(ProfilerMode.SILENT) //
                        .execute();
                execute.transferToHost(arrayC);
                long totalDeviceMemoryUsage = execute.getProfilerResult().getTotalDeviceMemoryUsage();
                System.out.println("Total device memory usage: " + totalDeviceMemoryUsage);
                System.out.println("Kernel Time: " + execute.getProfilerResult().getDeviceKernelTime());

                for (int j = 0; j < arrayC.getSize(); j++) {
                    assertEquals(arrayA.get(j) * arrayB.get(j), arrayC.get(j), 0.001f);
                }

                //assertEquals(1610612808, totalDeviceMemoryUsage);
            }

            for (int i = 0; i < arrayC.getSize(); i++) {
                assertEquals(arrayA.get(i) * arrayB.get(i), arrayC.get(i), 0.001f);
            }

        } catch (TornadoExecutionPlanException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testStressMemoryFieldsB() {

        DataPackage dataPackage = new DataPackage();

        // This test works when there is no invocation to data transfers

        // If we have transfer to device and transfer to host with the object,
        // then, it works. 
        TaskGraph taskGraph = new TaskGraph("testStressMemoryFields") //
                .transferToDevice(DataTransferMode.EVERY_EXECUTION, dataPackage) //
                .task("fields", dataPackage::testStressFields) //
                .transferToHost(DataTransferMode.EVERY_EXECUTION, dataPackage); //

        try (TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(taskGraph.snapshot())) {
            for (int i = 0; i < 100; i++) {
                TornadoExecutionResult execute = executionPlan.withProfiler(ProfilerMode.SILENT).execute();

                //execute.transferToHost(dataPackage.arrayC);

                long totalDeviceMemoryUsage = execute.getProfilerResult().getTotalDeviceMemoryUsage();
                System.out.println("Total device memory usage: " + totalDeviceMemoryUsage);

                for (int j = 0; j < dataPackage.arrayC.getSize(); j++) {
                    assertEquals(dataPackage.arrayA.get(j) * dataPackage.arrayB.get(j), dataPackage.arrayC.get(j), 0.001f);
                }
                assertEquals(1610612848, totalDeviceMemoryUsage);
            }

        } catch (TornadoExecutionPlanException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testStressMemoryFields() {
        int size = SIZE_OF_512MB / 4;
        arrayA = new FloatArray(size);
        arrayB = new FloatArray(size);
        arrayC = new FloatArray(size);

        arrayA.init(0.1f);
        arrayB.init(0.2f);

        TaskGraph taskGraph = new TaskGraph("testStressMemoryFields") //
                .transferToDevice(DataTransferMode.EVERY_EXECUTION, arrayA, arrayB) //
                .task("fields", this::testStressFields, arrayA, arrayB, arrayC) //
                .transferToHost(DataTransferMode.EVERY_EXECUTION, arrayC); //

        try (TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(taskGraph.snapshot())) {
            for (int i = 0; i < 100; i++) {
                TornadoExecutionResult execute = executionPlan.withProfiler(ProfilerMode.SILENT).execute();

                for (int j = 0; j < arrayC.getSize(); j++) {
                    assertEquals(arrayA.get(j) * arrayB.get(j), arrayC.get(j), 0.001f);
                }

                long totalDeviceMemoryUsage = execute.getProfilerResult().getTotalDeviceMemoryUsage();
                System.out.println("Total device memory usage: " + totalDeviceMemoryUsage);
                assertEquals(1610612808, totalDeviceMemoryUsage);
            }

        } catch (TornadoExecutionPlanException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testStressMemoryLocalArrays() {
        int size = SIZE_OF_512MB / 4;
        arrayA = new FloatArray(size);
        arrayB = new FloatArray(size);
        arrayC = new FloatArray(size);
        FloatArray a = new FloatArray(size);
        FloatArray b = new FloatArray(size);
        FloatArray c = new FloatArray(size);

        a.init(0.1f);
        b.init(0.2f);
        c.init(0.0f);

        TaskGraph taskGraph = new TaskGraph("testStressMemoryLocalArrays") //
                .transferToDevice(DataTransferMode.FIRST_EXECUTION, a, b) //
                .task("local", TestStressFields::testStressFieldsStatic, a, b, c) //
                .transferToHost(DataTransferMode.EVERY_EXECUTION, c); //

        try (TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(taskGraph.snapshot())) {
            for (int i = 0; i < 100; i++) {
                TornadoExecutionResult execute = executionPlan.withProfiler(ProfilerMode.SILENT).execute();
                long totalDeviceMemoryUsage = execute.getProfilerResult().getTotalDeviceMemoryUsage();
                System.out.println("Total device memory usage: " + totalDeviceMemoryUsage);
                assertEquals(1610612808, totalDeviceMemoryUsage);
            }
            for (int i = 0; i < a.getSize(); i++) {
                assertEquals(a.get(i) * b.get(i), c.get(i), 0.001f);
            }

        } catch (TornadoExecutionPlanException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testStressMemoryLocalObjects() {

        // This test is supported in the current version of TornadoVM (v1.0.11).
        // It creates an object of type `DataPackage` and builds a task-graph
        // invoking a static-method with an argument of type `DataPackage`.
        // It is mandatory to express data to be transferred to/from the device.
        // Otherwise, TornadoVM can't transfer the right fields to the device.

        DataPackage dataPackage = new DataPackage();
        TaskGraph taskGraph = new TaskGraph("testStressMemoryLocalObjects") //
                .transferToDevice(DataTransferMode.FIRST_EXECUTION, dataPackage) //
                .task("objectLocal", TestStressFields::testStressFieldsStatic, dataPackage) //
                .transferToHost(DataTransferMode.EVERY_EXECUTION, dataPackage);

        try (TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(taskGraph.snapshot())) {
            for (int i = 0; i < 2; i++) {
                TornadoExecutionResult executeResult = executionPlan.withProfiler(ProfilerMode.SILENT).execute();
                long totalDeviceMemoryUsage = executeResult.getProfilerResult().getTotalDeviceMemoryUsage();
                System.out.println("Total device memory usage: " + totalDeviceMemoryUsage);

                // Check result
                for (int j = 0; j < dataPackage.arrayA.getSize(); j++) {
                    assertEquals(dataPackage.arrayA.get(j) * dataPackage.arrayB.get(j), dataPackage.arrayC.get(j), 0.001f);
                }

                // Check memory usage on the device
                assertEquals(1610612848, totalDeviceMemoryUsage);
            }

        } catch (TornadoExecutionPlanException e) {
            throw new RuntimeException(e);
        }

        // After the scope of the execution plan, the close is automatically invoked, and all the fields
        // within the dataPackage objects are freed. 
    }
}
