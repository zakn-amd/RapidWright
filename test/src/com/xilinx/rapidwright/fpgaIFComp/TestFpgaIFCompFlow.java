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
import java.util.stream.Stream;
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
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TestFpgaIFCompFlow {
    // this should probably be in something like com.xilinx.rapidwright.util
    // generalized/simplified version of com.xilinx.rapidwright.ipi.BlockUpdater.runVivadoTasks
    private static List<String> runVivadoTask(String runDir, String tclScript, boolean verbose) {
        // all this method does is get vivado to run a TCL script
        final String vivadoCmd = "vivado -mode batch -source " + tclScript;
        System.out.println(vivadoCmd);

        // set up the vivado process
        Job j = new LocalJob();
        j.setCommand(vivadoCmd);
        j.setRunDir(runDir);
        j.launchJob();

        // run the vivado job
        while (!j.isFinished()) {
            if(verbose) {
                System.out.println("Vivado running");
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        String logFile = j.getLogFilename();
        List<String> log = new ArrayList<>();
        log = FileTools.getLinesFromTextFile(logFile);
        if(!log.isEmpty() && verbose) {
            for(String l : log) {
                System.out.println(l);
            }
        }

        return log;
    }

    private static int readVivadoLogForKeyPhrase(List<String> log, String key) {
        for(String l : log) {
            if(l.contains(key)) {
                return Integer.parseInt(l.replaceAll("[^\\d]", ""));
            }
        }
        return -1;
    }

    static Stream<RouteIF> routersToTest(){
        RouteIF R = new RouteIF();
        RouteIF rr = R.new rwRouter();
        //RouteIF cr = R.new classicRouter(); // really slow
        RouteIF nr = R.new nullRouter();
        return Stream.of(rr, nr);
    }

    // TODO: parameterize based on input dcp and expected unrouted/error'd nets
    @ParameterizedTest
    @MethodSource("com.xilinx.rapidwright.fpgaIFComp.TestFpgaIFCompFlow#routersToTest")
    public void testFlow(RouteIF competitionRouter, @TempDir Path tempDir) throws IOException {
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

        competitionRouter.route(outputIF+".netlist", outputIF+".phys");

        final String outputDCP = tempDir.resolve("rerouted_picoblaze_partial.dcp").toString();
        String checkArgs[] = {outputIF+"_routed.netlist", outputIF+"_routed.phys", constraintsFile, outputDCP};
        PhysicalNetlistToDcp.main(checkArgs);

        final String tclScript = tempDir.resolve("tclScript.tcl").toString();
        List<String> lines = new ArrayList<>();
        lines.add("open_checkpoint " + outputDCP);
        lines.add("report_route_status");
        lines.add("exit");
        FileTools.writeLinesToTextFile(lines, tclScript);

        List<String> log = runVivadoTask(tempDir.toString(), tclScript, true);
        int ur = readVivadoLogForKeyPhrase(log, "# of unrouted nets");
        int re = readVivadoLogForKeyPhrase(log, "# of nets with routing errors");

        Assertions.assertEquals(0, ur);
        Assertions.assertEquals(0, re);
    }
}
