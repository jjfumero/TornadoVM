/*
 * Copyright (c) 2024, 2025, APT Group, Department of Computer Science,
 * School of Engineering, The University of Manchester. All rights reserved.
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package uk.ac.manchester.tornado.drivers.opencl.graal.nodes;

import org.graalvm.compiler.core.common.LIRKind;
import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.lir.gen.LIRGeneratorTool;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.FixedWithNextNode;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.memory.address.AddressNode;
import org.graalvm.compiler.nodes.spi.LIRLowerable;
import org.graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.meta.Value;
import uk.ac.manchester.tornado.drivers.opencl.graal.HalfFloatStamp;
import uk.ac.manchester.tornado.drivers.opencl.graal.OCLArchitecture;
import uk.ac.manchester.tornado.drivers.opencl.graal.lir.OCLKind;
import uk.ac.manchester.tornado.drivers.opencl.graal.lir.OCLLIRStmt;
import uk.ac.manchester.tornado.drivers.opencl.graal.lir.OCLUnary;

@NodeInfo
public class WriteHalfFloatNode extends FixedWithNextNode implements LIRLowerable {

    public static final NodeClass<WriteHalfFloatNode> TYPE = NodeClass.create(WriteHalfFloatNode.class);

    @Input
    private AddressNode addressNode;

    @Input
    private ValueNode valueNode;

    public WriteHalfFloatNode(AddressNode addressNode, ValueNode valueNode) {
        super(TYPE, new HalfFloatStamp());
        this.addressNode = addressNode;
        this.valueNode = valueNode;
    }

    public void generate(NodeLIRBuilderTool generator) {
        LIRGeneratorTool tool = generator.getLIRGeneratorTool();
        Value addressValue = generator.operand(addressNode);
        OCLArchitecture.OCLMemoryBase base = ((OCLUnary.MemoryAccess) addressValue).getBase();
        OCLUnary.OCLAddressCast cast = new OCLUnary.OCLAddressCast(base, LIRKind.value(OCLKind.HALF));
        Value input = generator.operand(valueNode);
        tool.append(new OCLLIRStmt.StoreStmt(cast, (OCLUnary.MemoryAccess) addressValue, input));
    }
}
