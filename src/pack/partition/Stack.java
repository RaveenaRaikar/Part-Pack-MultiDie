package pack.partition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import pack.netlist.Netlist;
//import pack.netlist.PartitionNetlist;
//import pack.util.Output;
import pack.util.Output;

public class Stack {
	private HashMap<Integer,ArrayList<Netlist>> work;
	private int numElements = 0;
	
	public Stack(){
		this.work = new HashMap<Integer,ArrayList<Netlist>>();
		this.numElements = 0;
		}
			
	public void pushNetlist(Netlist netlist){
		this.numElements += 1;
		int numBlocks = netlist.atom_count();
		if(!this.work.containsKey(numBlocks)){
			this.work.put(numBlocks, new ArrayList<Netlist>());
		}
		this.work.get(numBlocks).add(netlist); //Adding the last block of the netlist as the previous condition is satisfied then 
											   //last block will not be added
	}

	public Netlist pullNetlist(){
		this.numElements -= 1;
		//TODO
		Netlist netlist = this.work.get(this.getMaxSize()).remove(0);
		this.removeEmptyRows();
		return netlist;
	}

	public int size(){
		return this.numElements;
		
	}
	public boolean isEmpty(){
		return (this.numElements == 0);
	}
	private int getMaxSize(){
		int maxSize = 0;
		for(int row:this.work.keySet()){
			if(row > maxSize){
				maxSize = row;
			}
		}
		return maxSize;
	}
	private void removeEmptyRows(){
		Set<Integer> emptyRows = new HashSet<Integer>();
		for(Integer row:this.work.keySet()){
			if(this.work.get(row).isEmpty()){
				emptyRows.add(row);
			}
		}
		for(Integer emptyRow:emptyRows){
			this.work.remove(emptyRow);
		}
	}
}
