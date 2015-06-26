GeometricComputingForBiomedicine
=====================================

You can find the final project I made for the Geometric Computing for Biomedicine class at Washington University in St. Louis in Fall 2011 here.

As the final project for the class I implemented a plug-in for ImageJ that reads in a binary image file of a femur, fits circles through the femur head and femur neck, and automatically measures the femur angle.

The project is described on my website here: http://research.engineering.wustl.edu/~adina.stoica/projects_wustl_geomcomp.html

You can also read the final project writeup here: http://research.engineering.wustl.edu/~adina.stoica/extras/proj/geometric_computing/femurhead.pdf

To get it to work:
1. Download ImageJ from http://imagej.nih.gov/ij/download.html
2. Download the Jama .jar file from http://math.nist.gov/javanumerics/jama/
3. Go to the ImageJ folder and copy jama.jar into /plugins/jars
4. Copy ActionBar into the /plugins subdirectory of ImageJ
5. Start ImageJ. Go to Plugins-> Compile and Run and select ActionBar/Femurangle.java
6. After the compilation is complete go to Plugins->ActionBar->FemurAngle.

The first icon opens an image file (example files in test_data) and the second one runs the algorithm on the file and calculates the femur angle.
You can zoom in and out on the image, as well as rotate it 90 degrees clockwise or counterclockwise. 
The last icon toggles the original ImageJ interface on and off.


Please note that the code for all the other items in the ActionBar Plugin is not mine. I am solely providing the whole code for ease of use. Original author: http://rsb.info.nih.gov/ij/plugins/action-bar.html
