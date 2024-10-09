package pack.main;

import java.util.ArrayList;

import java.util.List;
import java.util.concurrent.ExecutionException;


import pack.architecture.Architecture;
import pack.netlist.DieLevelNetlist;

import pack.netlist.Netlist;
//import pack.netlist.PartitionNetlist;
import pack.netlist.PathWeight;
import pack.partition.Partition;
import pack.util.Info;
import pack.util.Output;
import pack.util.Timing;

public class Main {
	public static void main(String[] args) throws InterruptedException, ExecutionException{
	
//MULTI-DIE MODIFICATIONS////
		ArrayList<String> SLLPartedges = new ArrayList<String>();
		Simulation simulation = new Simulation();
		simulation.parseArgs(args);

		Info.enabled(simulation.getBooleanValue("print_stats_to_file"));

		Output.path(simulation);//Set path of output
		Output.println(simulation.toValueString());
		Output.newLine();

		//NETLIST
		Netlist netlist = new Netlist(simulation);
		//////// PACKING TIMER ////////
		Timing multiPartTimer = new Timing();
		multiPartTimer.start();
		///////////////////////////////

		//ARCHITECTURE
		Timing architecturetimer = new Timing();
		architecturetimer.start();

		//generate die level architecture
		Architecture archdie= new Architecture(simulation);
		archdie.generate_die_architecture(netlist.get_models().keySet());
		
		
	    Architecture archLight = new Architecture(simulation);
		archLight.initialize();
		  
		architecturetimer.stop(); 
		Output.println("Architecture functionality took " + architecturetimer.toString()); 
		Output.newLine();
  
		//netlist.floating_blocks();
		  
		//Timing edges 
		PathWeight path = new PathWeight(netlist, archLight,simulation); 
		path.assign_net_weight();
		
		//Prepacking step is required to assign the hardblock groups
    	netlist.pre_pack_dsp_ram(archLight);
		netlist.pre_pack_carry();
		//PATITIONING USING HMETIS
		  
	    //////DIE LEVEL PARTITIONING USING HMETIS//////// 
		
		//Since die level partitioning does not use pre-pack step, the hard blocks are not created at this point.

	    Partition partition = new Partition(netlist, archLight, simulation, path.get_max_arr_time(),true);
	    Timing partitioningTimer = new Timing();
	    partitioningTimer.start();
	    Output.println("\tStarting Die level Partitioning ");
	    partition.diepartition(); 
	    //GET THE SLL NETS
	    SLLPartedges = partition.SLLPartedges;

	    partitioningTimer.stop();
	    Output.println("\tDie level Partitioning took " + partitioningTimer.toString());
	    Output.newLine();
	  
	    Info.finish(simulation);
	    //Change the netlist file
	    DieLevelNetlist net = new DieLevelNetlist(netlist,partition,simulation);
	    net.GenerateDieNetlist();
	   //Partitioned netlist checker; to ensure that the SLL are inputs/outputs in the respective blif file.

	    int dieCount = simulation.getIntValue("Number_of_die");
	    //Timing DiepartitioningTimer = new Timing();
	    Timing seedBasedPackingTimer = new Timing();
	    seedBasedPackingTimer.start();

	    List<Thread> threads = new ArrayList<>();
	    for(int i = 0; i < dieCount;i++) {
	    	MultiThreading parallelThread = new MultiThreading(i, simulation, netlist, SLLPartedges);
	    	Thread dieThread = new Thread(parallelThread);
	    	dieThread.start();
	    	threads.add(dieThread);
	    		    	
	    	
	    }
	    for (Thread t : threads) {
		    t.join();
		}

	    seedBasedPackingTimer.stop();	

	    multiPartTimer.stop();
	    Output.println("\t Total multidie took " + multiPartTimer);
	    
	}
	
}
