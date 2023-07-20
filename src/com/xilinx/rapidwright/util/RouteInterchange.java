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

package com.xilinx.rapidwright.util;

import java.io.IOException;
import java.util.Arrays;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.edif.EDIFNetlist;
import com.xilinx.rapidwright.interchange.LogNetlistReader;
import com.xilinx.rapidwright.interchange.LogNetlistWriter;
import com.xilinx.rapidwright.interchange.PhysNetlistReader;
import com.xilinx.rapidwright.interchange.PhysNetlistWriter;
import com.xilinx.rapidwright.rwroute.PartialRouter;
import com.xilinx.rapidwright.rwroute.RWRoute;

public class RouteInterchange {

    private static Design readUnroutedDesign(String logNetlistFileName,
            String physNetlistFileName) throws IOException {
        EDIFNetlist n = LogNetlistReader.readLogNetlist(logNetlistFileName);
        Design d = PhysNetlistReader.readPhysNetlist(physNetlistFileName, n);
        return d;
    }

    private static void writeRoutedDesign(Design d, String logNetlistFileName,
            String physNetlistFileName)
            throws IOException {
        LogNetlistWriter.writeLogNetlist(d.getNetlist(), logNetlistFileName);
        PhysNetlistWriter.writePhysNetlist(d, physNetlistFileName);
    }

    public static void main(String[] args) throws IOException {

        if ((args.length != 4) && (args.length != 5) && (args.length != 6)) {
            System.out.println("Usage: <input.netlist> <input.phys> <output.netlist> <output.phys> [--nonTimingDriven] [--partial]");
        }

        boolean ntd = false;
        boolean part = false;
        if (args.length >= 5) {
            String[] options = Arrays.copyOfRange(args, 4, args.length);
            ntd = Arrays.stream(options).anyMatch("--nonTimingDriven"::equals);
            part = Arrays.stream(options).anyMatch("--partial"::equals);
        }

        Design d = readUnroutedDesign(args[0], args[1]);

        if (part) {
            if (ntd) {
                PartialRouter.routeDesignPartialNonTimingDriven(d, null, false);
                ;
            } else {
                PartialRouter.routeDesignPartialTimingDriven(d, null, false);
            }
        } else {
            if (ntd) {
                RWRoute.routeDesignFullNonTimingDriven(d);
            }
            else {
                RWRoute.routeDesignFullTimingDriven(d);
            }
        }

        writeRoutedDesign(d, args[2], args[3]);
    }
}