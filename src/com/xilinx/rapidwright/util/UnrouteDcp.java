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

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Net;

public class UnrouteDcp {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("USAGE: UnrouteDcp <routed.dcp> <unrouted.dcp>");
            return;
        }

        Design d = Design.readCheckpoint(args[0]);

        for (Net n : d.getNets()) {
            if (!(n.isClockNet() || n.isStaticNet())) {
                n.unroute();
            }
        }

        d.writeCheckpoint(args[1]);
    }
}