/*
 * Copyright (c) 2023, Advanced Micro Devices, Inc.
 * All rights reserved.
 *
 * Author: Zak Nafziger, Advanced Micro Devices, Inc.
 *
 * This file is part of RapidWright.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.xilinx.rapidwright.interchange;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.xilinx.rapidwright.design.ConstraintGroup;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.ipi.XDCParser;
import com.xilinx.rapidwright.util.FileTools;

public class DcpToInterchange {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("USAGE: <input.dcp>");
            return;
        }

        Design design = Design.readCheckpoint(args[0]);
        String baseName = Paths.get(args[0]).getFileName().toString();
        baseName = FileTools.removeFileExtension(baseName);
        String logNlistName = baseName + ".netlist";
        String physNlistName = baseName + ".phys";
        String xdcName = baseName + ".xdc";

        LogNetlistWriter.writeLogNetlist(design.getNetlist(), logNlistName);
        PhysNetlistWriter.writePhysNetlist(design, physNlistName);

        List<String> constraints = design.getXDCConstraints(ConstraintGroup.NORMAL);
        try (FileOutputStream f = new FileOutputStream(xdcName)) {
            XDCParser.writeXDC(constraints, f);
        }
    }
}