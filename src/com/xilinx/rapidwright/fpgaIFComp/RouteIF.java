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
import com.xilinx.rapidwright.edif.EDIFNetlist;
import com.xilinx.rapidwright.interchange.LogNetlistReader;
import com.xilinx.rapidwright.interchange.LogNetlistWriter;
import com.xilinx.rapidwright.interchange.PhysNetlistReader;
import com.xilinx.rapidwright.interchange.PhysNetlistWriter;
import com.xilinx.rapidwright.router.Router;
import com.xilinx.rapidwright.rwroute.PartialRouter;


/**
 * read in an unrouted FPGA interchange formatted design, route it, and spit
 * out a routed FPGA interchange format design. 
 * @author zakn
 *
 */
public class RouteIF {

    private EDIFNetlist n;
    private Design d;

    private void readUnroutedDesign(String logNlistFileName, String physNlistFileName) throws IOException {
        n = LogNetlistReader.readLogNetlist(logNlistFileName);
        d = PhysNetlistReader.readPhysNetlist(physNlistFileName, n);
    }

    private void writeRoutedDesign(String logNlistFileName, String physNlistFileName) throws IOException {
        LogNetlistWriter.writeLogNetlist(d.getNetlist(), logNlistFileName.replace(".netlist", "_routed.netlist"));
        PhysNetlistWriter.writePhysNetlist(d, physNlistFileName.replace(".phys", "_routed.phys"));
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println(
                    "USAGE: <input>.netlist <input>.phys");
            System.exit(1);
        }
        String logNlistFileName = args[0];
        String physNlistFileName = args[1];

        RouteIF R = new RouteIF();
        rwRouter r = R.new rwRouter();
        r.route(logNlistFileName, physNlistFileName);
    }

    public void route(String logNlistFileName, String physNlsitFileName) throws IOException{
        throw new java.io.IOException();
    }

    public class rwRouter extends RouteIF {
        @Override
        public void route(String logNlistFileName, String physNlistFileName) throws IOException {
            readUnroutedDesign(logNlistFileName, physNlistFileName);

            PartialRouter.routeDesignPartialNonTimingDriven(d, null, true);

            writeRoutedDesign(logNlistFileName, physNlistFileName);
        }
    }

    public class classicRouter extends RouteIF{
        @Override
        public void route(String logNlistFileName, String physNlistFileName) throws IOException {
            readUnroutedDesign(logNlistFileName, physNlistFileName);

            Router r = new Router(d);
            r.routeDesign();

            writeRoutedDesign(logNlistFileName, physNlistFileName);
        }
    }

    public class nullRouter extends RouteIF{
        @Override
        public void route(String logNlistFileName, String physNlistFileName) throws IOException {
            readUnroutedDesign(logNlistFileName, physNlistFileName);

            // do nothing

            writeRoutedDesign(logNlistFileName, physNlistFileName);
        }
    }
}
