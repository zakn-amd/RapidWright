/*
 * Copyright (c) 2021-2022, Xilinx, Inc.
 * Copyright (c) 2022-2023, Advanced Micro Devices, Inc.
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
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import com.xilinx.rapidwright.design.ConstraintGroup;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.interchange.PhysicalNetlistToDcp;
import com.xilinx.rapidwright.ipi.XDCParser;
import com.xilinx.rapidwright.support.RapidWrightDCP;
import com.xilinx.rapidwright.util.FileTools;
import com.xilinx.rapidwright.util.Job;
import com.xilinx.rapidwright.util.LocalJob;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TestFpgaIFCompFlow {
    // this should probably be in something like com.xilinx.rapidwright.util
    // generalized/simplified version of com.xilinx.rapidwright.ipi.BlockUpdater.runVivadoTasks
    private static boolean runVivadoTask(String runDir, String tclScript, boolean verbose) {
        // all this method does is get vivado to run a TCL script
        final String vivadoCmd = "vivado -mode batch -source " + tclScript;
        System.out.println(vivadoCmd);

        // set up the vivado process
        Job j = new LocalJob();
        j.setCommand(vivadoCmd);
        j.setRunDir(runDir);
        j.launchJob();
        Optional<List<String>> log = Optional.empty();

        // helper class to print the output of vivado
        class PrintLog{
            private static void print(Optional<List<String>> log, boolean verbose) {
                if(!log.isEmpty() && verbose) {
                    for(String l : log.get()) {
                        System.out.println(l);
                    }
                }
            }
        }

        // run the vivado job
        while (!j.isFinished()) {
            log = j.getLastLogLines();
            PrintLog.print(log, verbose);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
         log = j.getLastLogLines();
        PrintLog.print(log, verbose);

        return j.jobWasSuccessful();
    }

    @Test
    public void testFlow(@TempDir Path tempDir) throws IOException {
        Assumptions.assumeTrue(FileTools.isVivadoOnPath());

        final String inputDCP = RapidWrightDCP.getString("picoblaze_partial.dcp");
        final String outputIF = tempDir.resolve("picoblaze_partial").toString();

        //TODO: cargo-cult programming; what is the correct way to extract the xdc?
        final String constraintsFile = tempDir.resolve("constraints.xdc").toString();
        List<String> constraints = Design.readCheckpoint(inputDCP).getXDCConstraints(ConstraintGroup.NORMAL);
        FileOutputStream f = new FileOutputStream(constraintsFile);
        XDCParser.writeXDC(constraints, f);

        String prepArgs[] = {inputDCP, outputIF};
        PrepRoutedBenchmark.main(prepArgs);

        String routeArgs[] = {outputIF+".netlist", outputIF+".phys"};
        RouteIF.main(routeArgs);

        final String outputDCP = tempDir.resolve("rerouted_picoblaze_partial.dcp").toString();
        String checkArgs[] = {outputIF+"_routed.netlist", outputIF+"_routed.phys", constraintsFile, outputDCP};
        PhysicalNetlistToDcp.main(checkArgs);

        final String tclScript = tempDir.resolve("tclScript.tcl").toString();
        List<String> lines = new ArrayList<>();
        lines.add("open_checkpoint " + outputDCP);
        lines.add("report_route_status");
        lines.add("exit");
        FileTools.writeLinesToTextFile(lines, tclScript);

        boolean r = runVivadoTask(tempDir.toString(), tclScript, true);

        Assertions.assertTrue(r);
    }
}
