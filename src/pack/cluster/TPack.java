package pack.cluster;

import java.nio.file.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pack.architecture.Architecture;
import pack.main.Simulation;
import pack.netlist.B;
import pack.netlist.N;
import pack.netlist.Netlist;
import pack.netlist.P;
import pack.netlist.T;
import pack.partition.Partition;
import pack.util.ErrorLog;
import pack.util.Output;
import pack.util.ThreadPool;
import pack.util.Util;

public class TPack {
	private Netlist root;
	private Netlist Orig;
	private Partition partition;

	private Simulation simulation;

	private String vpr_folder;
	private int DieNum;

	private Set<String> netlistInputs;

	private Set<String> netlistOutputs;
	private ArrayList<String> SLLPartedges;

	private List<Netlist> subcircuits;
	private List<LogicBlock> logicBlocks;

	private ThreadPool threadPool;
	private List<VPRThread> packPool;

	public TPack(Netlist Orig, Netlist root, Partition partition, Architecture architecture, Simulation simulation, int DieNum,ArrayList<String> SLLPartedges){
		this.root = root;
		this.Orig = Orig;
		this.partition = partition;

		this.simulation = simulation;

		this.vpr_folder = simulation.getStringValue("vpr_folder");

		this.logicBlocks = new ArrayList<>();
		this.DieNum = DieNum;
		this.SLLPartedges = SLLPartedges;
		this.findNetlistInputsAndOutputTerminalNames(SLLPartedges);

	}
	public TPack(Netlist Orig, Netlist root, Architecture architecture, Simulation simulation, int DieNum,ArrayList<String> SLLPartedges){
		this.root = root;
		this.Orig = Orig;

		this.simulation = simulation;

		this.vpr_folder = simulation.getStringValue("vpr_folder");

		this.logicBlocks = new ArrayList<>();
		this.DieNum = DieNum;
		this.SLLPartedges = SLLPartedges;
		this.findNetlistInputsAndOutputTerminalNames(SLLPartedges);

	}
	private void findNetlistInputsAndOutputTerminalNames(ArrayList<String> SLLPartedges){
		this.netlistInputs = new HashSet<>();

		
 		for(N inputNet:this.root.get_input_nets()){
 			boolean inputTerminalFound = false;
 			if(this.Orig.get_Orig_input_nets().contains(inputNet.get_name())) {
 				
 				for(P terminalPin:inputNet.get_terminal_pins()){
					T t = terminalPin.get_terminal();
					if(t.is_input_type()){
						this.netlistInputs.add(t.get_name());
						inputTerminalFound = true;
					}
				}
				if(!inputTerminalFound){
					ErrorLog.print("Input net " + inputNet.toString() + " has no input terminal");
				}
 			}
 		}
 		
 		this.netlistOutputs = new HashSet<>();
		ArrayList<N> outputNets = new ArrayList<>();
 		outputNets.addAll(this.root.get_output_nets());
		if(this.root.has_floating_blocks()){
			for(B floatingBlock:this.root.get_floating_blocks()){
				for(N n:floatingBlock.get_output_nets()){
					if(n.has_terminals()){
						if(!outputNets.contains(n)){
							outputNets.add(n);
						}
					}
				}
			}
		}
 		for(N outputNet:outputNets){
 			boolean outputTerminalFound = false;
 			if(this.Orig.get_Orig_output_nets().contains(outputNet.get_name())) {
				for(P terminalPin:outputNet.get_terminal_pins()){
					T t = terminalPin.get_terminal();
					if(t.is_output_type()){
						this.netlistOutputs.add("out:" + t.get_name());
						outputTerminalFound = true;
					}
				}
				if(!outputTerminalFound){
					ErrorLog.print("Output net " + outputNet.toString() + " has no output terminal");
				}
 			}

 		}
	}
	public void seedBasedPacking(){
		Output.println("PHASE 2: SEED BASED PACKING");
		Output.newLine();
		
		this.subcircuits = this.root.get_leaf_nodes();
		
		//Analyze the leaf nodes
		for(Netlist leafNode:this.subcircuits){
			if(leafNode.has_children()){
				ErrorLog.print("A leaf node should not have children!");
			}
		}

		//Analyze the hierarchy recursively and give each netlist a hierarchy identifier
		this.root.setRecursiveHierarchyIdentifier("");

		double unpackTime = 0.0;
		for(Netlist subcircuit:this.subcircuits){
			unpackTime += subcircuit.unpack_all_molecules();
		}
		Output.println("\tUnpack molecules took " + Util.round(unpackTime, 2) + " sec");
		Output.newLine();

		Output.println("\tLeaf nodes: " + this.subcircuits.size());
		Output.print("\t\t");
		int no = 0;
		for(Netlist nl:this.subcircuits){
			if(no == 10){
				Output.newLine();
				Output.print("\t\t");
				no = 0;
			}
			System.out.print(nl.atom_count() + " ");
			no += 1;
		}
		Output.newLine();
		Output.newLine();

		int poolSize = this.simulation.getIntValue("num_threads");
		this.threadPool = new ThreadPool(poolSize);
		this.packPool = new ArrayList<>();
		Output.println("\tPool size: " + poolSize);
		Output.newLine();

		if(this.root.has_floating_blocks()){
			
			this.startTPack(this.root.get_floating_blocks());
		}

		while(!this.subcircuits.isEmpty() || !this.packPool.isEmpty()){
			
			this.startTPack();
			this.finishTPack();
		}
		Output.newLine();

		Output.println("\t" + "A maximum of " + this.threadPool.maxUsage() + " threads is used during seed based packing");
		Output.newLine();

		this.removeCutTerminalsFromIOBlocks();
		this.updateEnabledLogicBlocks();
		for(Netlist netlist: this.root.get_leaf_nodes()){
			netlist.updateEnabledLogicBlocks();
		}
	}
	public void seedBasedPackingVPR(){
		Output.println("PHASE 2: SEED BASED PACKING");
		Output.newLine();
		this.packPool = new ArrayList<>();

		this.startAAPack();
		this.finishAAPack();

	
		Output.newLine();


		this.removeCutTerminalsFromIOBlocks();
		this.updateEnabledLogicBlocks();
		for(Netlist netlist: this.root.get_leaf_nodes()){
			netlist.updateEnabledLogicBlocks();
		}
	}
	private void updateEnabledLogicBlocks(){
		List<LogicBlock> temp = this.logicBlocks;
		this.logicBlocks = new ArrayList<>();
		for(LogicBlock block:temp){
			if(block.enabled()){
				this.logicBlocks.add(block);
			}
		}
	}

	public List<LogicBlock> getLogicBlocks(){
		return this.logicBlocks;
	}
	

	private void startTPack(Set<B> floatingBlocks){
		boolean dielevel = false;
		int thread = this.threadPool.getThread();
		Netlist.write_blif(this.vpr_folder + "vpr/files/", thread, floatingBlocks, this.root.get_blif(), this.root.get_models(), this.simulation.getSimulationID(), dielevel);
		VPRThread vpr = new VPRThread(thread, this.simulation, null,this.DieNum);
		vpr.run(floatingBlocks.size());
		this.packPool.add(vpr);
	}
	public void startTPack(){
		boolean dielevel =false;
		while(!this.subcircuits.isEmpty() && !this.threadPool.isEmpty()){
			Netlist leafNode = this.subcircuits.remove(0);
			int thread = this.threadPool.getThread();
			leafNode.writeSDC(this.vpr_folder + "vpr/files/", thread, this.partition, this.simulation.getSimulationID());
			leafNode.writeBlif(this.vpr_folder + "vpr/files/", thread, this.partition, this.simulation.getSimulationID(),dielevel);
			VPRThread vpr = new VPRThread(thread, this.simulation, leafNode,this.DieNum);
			
			vpr.run(leafNode.atom_count());
			this.packPool.add(vpr);
		}
	}	
	public void startAAPack(){
		Netlist leafNode = this.root;
		VPRThread vpr = new VPRThread(this.simulation, leafNode,this.DieNum);
			
		vpr.runaapack(leafNode.atom_count());
		this.packPool.add(vpr);
	}
	
	public void finishTPack(){
		for(int i=0; i<this.packPool.size(); i++){
			if(!this.packPool.get(i).isRunning(i)){
				VPRThread vpr = this.packPool.remove(i);
				int thread = vpr.getThread();
				this.threadPool.addThread(thread);
				/* files generated in Eclipse source directory, move the process files to VPR/files folder
				 * using files.move. --> This leads to additional runtime hence skipped.
				 */
				String CurrentDirectory = System.getProperty("user.dir");
				String file = CurrentDirectory +"/" + this.root.get_blif() + "_" + this.simulation.getSimulationID() + "_" + thread;

				if(!Util.fileExists(file + ".net")){
		 			Output.println("Netfile " + file + ".net" + " " + "not available");
		 			Output.println("**** VPR Line ****");
		 			Output.println(vpr.getCommand());
		 			Output.newLine();
		 			ErrorLog.print("Netfile " + file + ".net" + " " + "not available");
		 		}

		 		this.processNetlistFile(file, vpr.getNetlist());
				this.startTPack();
				i--;
			}
		}
	}
	public void finishAAPack(){

		for(int i=0; i<this.packPool.size(); i++){
			while(true) {
				if(this.packPool.get(i).isRunning(i)){
				}
				else {
					VPRThread vpr = this.packPool.remove(i);
					String netfile = simulation.getStringValue("circuit") + "_" + simulation.getSimulationID() +"_die_" + this.DieNum;
					String CurrentDirectory = System.getProperty("user.dir");
					String file = CurrentDirectory +"/" + netfile;
	
					if(!Util.fileExists(file + ".net")){
			 			Output.println("Netfile " + file + ".net" + " " + "not available");
			 			Output.println("**** VPR Line ****");
			 			Output.println(vpr.getCommand());
			 			Output.newLine();
			 			ErrorLog.print("Netfile " + file + ".net" + " " + "not available");
			 		}
	
			 		this.processNetlistFile(file, vpr.getNetlist());
					i--;
					break;
				}
			}	
		}
	}
	private String[] getLines(String file){
		ArrayList<String> lines = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file + ".net"));
			String line = br.readLine();
			while (line != null) {
				lines.add(line);
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			System.err.println("Problem in reading the BLIFfile: " + e.getMessage());
			e.printStackTrace();
		}	
 		String[] result = new String[lines.size()];
 		return lines.toArray(result);
	}
	private void processNetlistFile(String file, Netlist netlist){
 		if(!Util.fileExists(file + ".net")){
 			ErrorLog.print("Netfile " + file + ".net" + " " + "not available");
 		}else{
 			String[] lines = this.getLines(file);
 			LogicBlock[] currentBlock = new LogicBlock[20];
 			int pbLevel = -1;
 			for(int i=0; i<lines.length; i++){
 				String line = lines[i];
 				if(line.contains("<block")){
 					pbLevel += 1;
 				}
 				if(line.contains("<block") && pbLevel > 0){
 					boolean floating = (netlist == null);
 					currentBlock[pbLevel] = new LogicBlock(getName(line), getInstance(line), getInstanceNumber(line), getMode(line), pbLevel, floating);
 					if(line.contains("/>")){
 	 					pbLevel -= 1;
 	 					if(pbLevel == 0){
 							LogicBlock lb = currentBlock[pbLevel+1];
 							this.logicBlocks.add(lb);
 	 					}else{
 	 						currentBlock[pbLevel].addChildBlock(currentBlock[pbLevel+1]);
 	 					}
 					}
 				}else if(line.contains("<inputs>") && pbLevel > 0){
 					i += 1;
 					line = parseLine(lines[i]);
 					while(line.contains("<port name=")){
 						String port = getName(line);
						String pinLine = line;
 	 					while(!line.contains("</port>")){
 	 						i += 1;
 	 						line = parseLine(lines[i]);
 	 						pinLine += " " + line;
 	 					}
 	 					String[] pins = this.getPins(pinLine, port);
 						currentBlock[pbLevel].addInput(port,pins);
 						i += 1;
 						line = parseLine(lines[i]);
 					}
 					i -= 1;
 				}else if(line.contains("<outputs>") && pbLevel > 0){
 					i += 1;
 					line = parseLine(lines[i]);
 					while(line.contains("<port name=")){
 						String port = getName(line);
						String pinLine = line;
 	 					while(!line.contains("</port>")){
 	 						i += 1;
 	 						line = parseLine(lines[i]);
 	 						pinLine += " " + line;
 	 					}
 	 					String[] pins = this.getPins(pinLine, port);
 						currentBlock[pbLevel].addOutput(port,pins);
 						i += 1;
 						line = parseLine(lines[i]);
 					}
 					i -= 1;
 				}else if(line.contains("<clocks>") && pbLevel > 0){
 					i += 1;
 					line = parseLine(lines[i]);
 					while(line.contains("<port name=")){
 						String port = getName(line);
						String pinLine = line;
 	 					while(!line.contains("</port>")){
 	 						i += 1;
 	 						line = parseLine(lines[i]);
 	 						pinLine += " " + line;
 	 					}
 	 					String[] pins = this.getPins(pinLine, port);
 						currentBlock[pbLevel].addClock(port,pins);
 						i += 1;
 						line = parseLine(lines[i]);
 					}
 					i -= 1;
 				}else if(line.contains("</block>") && pbLevel > 0){
					pbLevel -= 1;
					if(pbLevel == 0){
						LogicBlock lb = currentBlock[pbLevel+1];
						if(!lb.isFloating()) netlist.addLogicBlock(lb);//Floating blocks have no netlist
						this.logicBlocks.add(lb);
					}else{
						currentBlock[pbLevel].addChildBlock(currentBlock[pbLevel+1]);
					}
 				}
 			}
 
 		}
	}
 	private static String parseLine(String line){
		while(line.charAt(line.length()-1) == ' ') line = line.substring(0, line.length()-1);
		while(line.charAt(0) == ' ') line = line.substring(1, line.length());
		return line;
 	}
 	private String getName(String line){
		int namePos = line.indexOf("name=\"");
		if(namePos >= 0){
			int openPos = line.indexOf("\"", namePos+1);
			int closePos = line.indexOf("\"", openPos+1);
			String name = line.substring(openPos+1,closePos);
			return name;
		}else{
			return null;
		}
 	}
 	private String getInstance(String line){
		int instancePos = line.indexOf("instance=\"");
		if(instancePos >= 0){
			int openPos = line.indexOf("\"",instancePos+1);
			int closePos = line.indexOf("[", openPos+1);
			return line.substring(openPos+1,closePos);
		}else{
			return null;
		}
 	}
 	private int getInstanceNumber(String line){
		int instancePos = line.indexOf("instance=\"");
		if(instancePos >= 0){
			int openPos = line.indexOf("[",instancePos+1);
			int closePos = line.indexOf("]", openPos+1);
			return Integer.parseInt(line.substring(openPos+1,closePos));
		}else{
			return -1;
		}
 	}
 	private String getMode(String line){
		int modePos = line.indexOf("mode=\"");
		if(modePos >= 0){
			int openPos = line.indexOf("\"", modePos+1);
			int closePos = line.indexOf("\"", openPos+1);
			return line.substring(openPos+1,closePos);
		}else{
			return null;
		}
 	}
 	private String[] getPins(String line, String port){
 		if(line.contains("\">") && line.contains("</")){
 			line = line.substring(line.indexOf("\">")+2,line.indexOf("</"));
 		}else{
 			line = line.replace("<port name=\"" + port + "\">","");
 			line = line.replace("</port>","");
 			System.out.println("\"bug\" found in line: " + line);
 		}
 		if(line.contains("\t")) line = line.replace("\t", "");
		while(line.charAt(line.length()-1) == ' ') line = line.substring(0, line.length()-1);
		while(line.charAt(0) == ' ') line = line.substring(1, line.length());
		while(line.contains("  ")) line = line.replace("  ", " ");
		return line.split(" ");
 	}
 	private void removeCutTerminalsFromIOBlocks(){
 		boolean newLine = false;
 		for(LogicBlock parent:this.logicBlocks){
 			if(parent.getMode().equals("io")){
 				for(LogicBlock child:parent.getNonEmptyChildBlocks()){
 					if(child.hasMode()){
 	 					if(child.getMode().equals("inpad")){
 	 						String name = child.getName();
 	 						if(!this.netlistInputs.contains(name)){
 	 							if(parent.numberOfNonEmptyChildBlocks()==1){
 	 								parent.disable();//This io block is a cut terminal
 	 							}else{
 	 								parent.removeInpadBlock();

 	 								//If the removed input pad that results from a cut net is an input to io_cells then this connections should be restored by adding an input to the io block
 	 								for(LogicBlock ioCell:parent.getNonEmptyChildBlocks()){
 	 									if(ioCell.getInstance().equals("io_cell")){
 	 										int ioCellInputNum = 0;
 	 										for(String input:ioCell.getInputs("data_in")){
 	 											if(input.contains("pad[0].recieve_off_chip[0]")){
 	 												Output.println("\t\tInput \"" + name + "\" on block \"" + parent.getName() + "\" is removed, add removed input pad as an input of the io block");
 	 												newLine = true;
 	 												//Search for input on parent
 	 												int parentCoreInInputNum = 0;
 	 	 											for(String parentCoreInInput:parent.getInputs("core_in")){
 	 	 												if(parentCoreInInput.equals("open")){
 	 	 													ioCell.setInput("data_in", ioCellInputNum, "io.core_in[" + parentCoreInInputNum + "]->io_cell_inputs");
 	 	 													parent.setInput("core_in", parentCoreInInputNum, name);
 	 	 													break;
 	 	 												}else if(parentCoreInInput.equals(name)){
 	 	 													ioCell.setInput("data_in", ioCellInputNum, "io.core_in[" + parentCoreInInputNum + "]->io_cell_inputs");
 	 	 													break;
 	 	 												}
 	 	 												parentCoreInInputNum += 1;
 	 	 											}
 	 											}
 	 											ioCellInputNum += 1;
 	 										}
 	 									}
 	 								}
 	 							}
 	 						}else{
 	 							this.netlistInputs.remove(name);
 	 						}
 	 					}else if(child.getMode().equals("outpad")){
 	 						String name = child.getName();
 	 						if(!this.netlistOutputs.contains(name)){
 	 							if(parent.numberOfNonEmptyChildBlocks()==1){
 	 								parent.disable();//This io block is a cut terminal
 	 							}else{					
 	 								parent.removeOutpadBlock();
 	 							}
 	 						}else{
 	 							this.netlistOutputs.remove(name);
 	 						}
 	 					}
 					}
 				}
 			}else if(parent.getMode().equals("inpad")){ ////////////////////
 				String name = parent.getName();
 				
 				if(!this.netlistInputs.contains(name)) {
 					if(parent.numberOfNonEmptyChildBlocks()==1){
							parent.disable();//This io block is a cut terminal
						}else{					
							parent.removeInpadBlock();
						}
 				}else {
 					this.netlistInputs.remove(name);
 				}
 			}else if(parent.getMode().equals("outpad")) { 
 				String name = parent.getName();
 				if(!this.netlistOutputs.contains(name)) {
 					if(parent.numberOfNonEmptyChildBlocks()==1){
						parent.disable();//This io block is a cut terminal
					}else{					
						parent.removeOutpadBlock();
					}
 				}else {
 					this.netlistOutputs.remove(name);
 				}
 			}/////////
 		}
 		if(newLine)Output.newLine();
 	}
 	private void deleteExistingFiles(){
		//Delete all existing files of this netlist in the vpr files folder
		File folder = new File(this.vpr_folder + "vpr/files/");
		File[] listOfFiles = folder.listFiles();
		for(int i = 0; i < listOfFiles.length; i++){
			File file = listOfFiles[i];
			if(file.isFile()){
				if(file.getName().contains(this.root.get_blif() + this.simulation.getSimulationID())){
					file.delete();
				}
			}
		}
 	}
}
