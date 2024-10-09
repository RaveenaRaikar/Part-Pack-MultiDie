The FPGA CAD Framework for 2.5D Multi-Die architectures
==============================

An FPGA CAD framework focused on optimizing CAD algorithms for multi-die architectures.
The framework is implemented in Java.


What can I do with this tool flow?
---------------

<ul>

<li>
Partitioning and Packing:
<ul>
  <li>The partitioning step is incorporated in the packing flow. The first step is to partition the input netlist (BLIF format) based on the number of die in the system. The next step is packing using AApack (VTR8).</li>
</ul>
</li>


</ul>

Usage
---------------

Some parts of this toolflow require external packages.  
Partitioning: hMETIS.  
Packing: VPR (from VTR package).  

Program arguments:  
--result_folder /path/to/result/folder  
--architecture  /Arch_file_name  
--circuit /circuit_name  
--vpr_folder /path/to/VTR/root/folder  
--hmetis_folder /path/to/hmetis/folder  

License
---------------
see license file

Contact us
---------------
The FPGA Placement Framework is released by Ghent University, ELIS department, Hardware and Embedded Systems (HES) group (http://hes.elis.ugent.be).

If you encounter bugs, want to use the FPGA CAD Framework but need support or want to tell us about your results, please contact us. We can be reached at raveenaramanand.raikar[at]ugent.be

