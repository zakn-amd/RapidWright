/*
 * Copyright (c) 2023, Advanced Micro Devices, Inc.
 * All rights reserved.
 *
 * Author: Zak Nafziger, Xilinx Research Labs.
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

package com.xilinx.rapidwright.fpgaIFComp;

import java.io.IOException;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.interchange.LogNetlistWriter;
import com.xilinx.rapidwright.interchange.PhysNetlistWriter;


/**
 *  Unroute a placed and routed DCP and write the result out as in FPGA
 *  Interchange File. Based on com.xilinx.examples.UpdateRoutingUsingSATRouter
 *  and com.xilinx.rapidwright.interchange.PyhsicalNetlistExample
 *  @author zakn
 *  
 */
public class PrepRoutedBenchmark {
    public static void main(String[] args) throws IOException {
        // Check args
        if (args.length != 2) {
            System.out.println("USAGE: java " + PrepRoutedBenchmark.class.getCanonicalName() + " "
                    + "<placed_routed_dcp> <output_unrouted_if>");
            return;
        }
        
        Design d = Design.readCheckpoint(args[0]);
        
        for (Net n : d.getNets()) {
            if  ( !(n.isClockNet() || n.isStaticNet()) ) {
                n.unroute();
            }
        }
        
        String logNlistFileName = args[1] + ".netlist";
        String physNlistFileName = args[1] + ".phys";
        LogNetlistWriter.writeLogNetlist(d.getNetlist(), logNlistFileName);
        PhysNetlistWriter.writePhysNetlist(d, physNlistFileName);
        
    }
}