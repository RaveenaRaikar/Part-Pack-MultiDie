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
  <li>The partitioning flow is incorporated in the packing flow. The first step is to partition the input netlist (BLIF format) based on the number of die in the system. The next step is packing using AApack (VTR8).</li>
</ul>
</li>


</ul>

Usage
---------------

Some parts of this toolflow require external packages. 
Partitioning: hMETIS.
Packing: VPR (from VTR package).

Progam arguments:
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

If you encounter bugs, want to use the FPGA CAD Framework but need support or want to tell us about your results, please contact us. We can be reached at yun.zhou[at]ugent.be

Referencing the FPGA Placement Framework
---------------
If you use the FPGA CAD Framework in your work, please reference the following papers in your publications: <br>

Packing:
<b>How preserving circuit design hierarchy during FPGA packing leads to better performance <br>
Dries Vercruyce, Elias Vansteenkiste and Dirk Stroobandt</b> <br>
<i> IEEE Transactions on Computer-Aided Design of Integrated Circuits and Systems}, 37(3), pp. 629-642.</i>

Placement:
<b>Liquid: High quality scalable placement for large heterogeneous FPGAs<br>
Dries Vercruyce, Elias Vansteenkiste and Dirk Stroobandt</b> <br>
<i> Field Programmable Technology (ICFPT), 2017 17th International Conference on. IEEE, 2017</i>

Routing:
<b>CRoute: A fast high-quality timing-driven connection-based FPGA router<br>
Dries Vercruyce, Elias Vansteenkiste and Dirk Stroobandt</b> <br>
<i> accepted for publication</i>

Contributors
---------------
Active Contributors
<ul>
  <li>Yun Zhou - <a href="mailto:yun.zhou@ugent.be">yun.zhou@ugent.be</a></li>
</ul>

Past Contributors
<ul>
  <li>Dries Vercruyce - <a href="mailto:dries.vercruyce@ugent.be">dries.vercruyce@ugent.be</a></li>
  <li>Elias Vansteenkiste - <a href="mailto:Elias.Vansteenkiste@gmail.com">Elias.Vansteenkiste@gmail.com</a></li>
  <li>Arno Messiaen - <a href="mailto:Arno.Messiaen@gmail.com">Arno.Messiaen@gmail.com</a></li>
  <li>Seppe Lenders - <a href="mailto:Seppe.Lenders@gmail.com"> Seppe.Lenders@gmail.com</a></li>
</ul>

Development
---------------
The FPGA CAD Framework is a work in progress, input is welcome.

