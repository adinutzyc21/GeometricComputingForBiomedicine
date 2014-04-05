/*
 Adina Raluca Stoica
 CSE554: Geometric Computing for Biomedicine
 Final Project, Fall 2011
 
 Femur Head and Neck Angle Measurement
 Create a program that reads in a binary image file of a femur, fits circles through
 the femur head and femur neck, and automatically measures the angle between the 
 line connecting the center of the femur head circle and the center of the femur 
 neck circle, and the line that is vertical through the center of the femur head 
 circle when the bone lies horizontally with the head to the right.
 */
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.util.*;
import ij.plugin.filter.*;

//my stuff
import Jama.Matrix;
import java.text.*;

public class Femur_Angle implements PlugInFilter {
	ImagePlus imp;
	int magenta = -50000;
	int red = -120000;
	int black = 1;
	int teal = 100000;
	int blue = 255;
	int yellow = -500;
	int green = 20000;
	int lime = 1000000000;
	int pink = -1000000;
	int ocre = -20000000;
	int burgundy = -7000000;
	int gray = -5000000;
	int white = -1;

	//aliases for 0 and 1
	int zero = -1;//black?
	int one = -16777216;//white

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
		int[][] border=getBorder(ip);
		int[][] contour1=getContour1(border,ip);
		int[][] contour2=getContour2(border,ip);
		int[] centAndRad1=fitCircle(contour1);
		int[] centAndRad2=closestPoints(contour2);
		double angle = getAngle(centAndRad1[0], centAndRad1[1], centAndRad2[0], centAndRad2[1]);
		//draw
		drawContours(border,contour1,contour2,ip, black, white, white, red, magenta, magenta);//bordercolor, bonecolor, backgroundcolor, headcolor, neck1color, neck2color
		imp.updateAndDraw();
		//Pause for 4 seconds
	    try {
	        Thread.sleep(500);
	    } catch (InterruptedException e) {
	        //We've been interrupted: no more messages.
	        return;
	    }
		drawCircle(centAndRad1, ip, teal);
		imp.updateAndDraw();
		//Pause for 4 seconds
	    try {
	        Thread.sleep(500);
	    } catch (InterruptedException e) {
	        //We've been interrupted: no more messages.
	        return;
	    }
		drawCircle(centAndRad2, ip, blue);
		imp.updateAndDraw();
		//Pause for 4 seconds
	    try {
	        Thread.sleep(500);
	    } catch (InterruptedException e) {
	        //We've been interrupted: no more messages.
	        return;
	    }
		drawAngle(centAndRad1[0], centAndRad1[1], centAndRad2[0], centAndRad2[1],ip, yellow);//refresh image
		imp.updateAndDraw();
		//show dialog
		NumberFormat formatter = new DecimalFormat("#.##");
		GenericDialog gd = new GenericDialog("Angle Measurement");
		gd.addStringField("The angle has ", formatter.format(angle)+" radians",15);
		gd.addStringField("The angle has ", formatter.format(Math.toDegrees(angle))+" degrees",15);
		gd.showDialog();
	}
	
	//Gets the contours for the second circle
	private int[][] getContour2(int[][] border, ImageProcessor ip){
		int bsize = border.length;
		int csize=bsize/6;
		int[][] contour = new int[csize][2];
		int j=0;
		
		//get the contour
		for(int i = bsize/3; i < bsize/2 && j<csize; i++){
			contour[j][1]=border[i][1];
			contour[j][0]=border[i][0];
			j++;
		}
		//sort the contour
		contour=xySort(contour);
		
		//change the begining and end so that we have 
		//correspondences on both sides (desn't really matter, just easier)
		int s=0;//starting point
		int eq=0;//are they equal?
		while(eq==0)
			if(contour[s][1]!=contour[s+1][1])
				s+=1;
			else
				eq=1;
		eq=0;//are they equal?
		while(eq==0)
			if(contour[csize-2][1]!=contour[csize-1][1])
				csize-=1;
			else
				eq=1;

		//find the centroid coordinates
		int[] centroid = getCentroid(s,csize,contour);
		//img[centroid[1]*width+centroid[0]]=red;
		int xc=centroid[0];
		
		//remove multiples (places where the ys are the same
		// more than 2 times in a row and some of the xs are consecutive)
		int[][] temp = new int[csize][2];//for removing multiples
		j=0;
		for(int k=s; k<csize; k++){
			if(k+1<csize)
				if(contour[k][0]<xc && contour[k+1][0]<xc && contour[k][1]==contour[k+1][1])
					while(contour[k][0]<xc && contour[k+1][0]<xc && contour[k][1]==contour[k+1][1])
						k++;
			temp[j][0]=contour[k][0];
			temp[j][1]=contour[k][1];
			if(k+1<csize)
				if(contour[k][0]>=xc && contour[k+1][0]>=xc && contour[k][1]==contour[k+1][1])
					while(contour[k][0]>=xc && contour[k+1][0]>=xc && contour[k][1]==contour[k+1][1])
						k++;
			j++;
		}
		//make a new array with just the right size
		int usize = j;
		int[][] contour2 = new int[usize][2];
		for(int i=0; i<usize; i++){
			contour2[i][1]=temp[i][1];
			contour2[i][0]=temp[i][0];
		}

		return contour2;
	}
	
	//Gets the contour of the borders for the first circle
	private int[][] getContour1(int[][] border, ImageProcessor ip){
		int bsize = border.length;
		int len = bsize/3;
		int[][] cont1 = new int[len][2];
		for(int i=0; i < len; i++){
			cont1[i][1]=border[i][1];
			cont1[i][0]=border[i][0];
		}
		return cont1;
	}

	//Gets the image border out of the binary picture
	private int[][] getBorder(ImageProcessor ip){
		int[] img = (int[])ip.getPixels();
		int width = ip.getWidth();
		int height = ip.getHeight();
		int[][] border = new int[width*height/2][2];
		int k;

		//get the border
		k=0;
		for(int y=0; y<height; y++)
			for(int x=0; x<width; x++){
				if(x+1 < width){
					if(img[y*width+x] == zero && img[y*width+x+1] == one){
						border[k][0] = x+1;
						border[k][1] = y;
						k+=1;
					}
					if(img[y*width+x] == one && img[y*width+x+1] == zero){
						border[k][0] = x;
						border[k][1] = y;
						k+=1;	
					}
				}
				if(y+1 < height){
					if(img[y*width+x] == zero && img[(y+1)*width+x] == one){
						border[k][0] = x;
						border[k][1] = y+1;
						k+=1;
					}
					if(img[y*width+x] == one && img[(y+1)*width+x] == zero){
						border[k][0] = x;
						border[k][1] = y;
						k+=1;	
					}
				}
			}
		int bsize = k;
		
		//remove duplicates
		int[][] border2 = removeDups(border,bsize);

		return border2;//return the smaller, non-duplicated array
	}

	//Removes duplicates and returns smaller array
	private int[][] removeDups(int[][]border, int bsize){
		int[][] border_copy = new int[bsize][2];
		int k=0;
		int found;
		for(int i=0; i<bsize; i++){
			found=0;
			for(int j=i+1; j<bsize; j++)
				if(border[i][0]==border[j][0] && border[i][1]==border[j][1]){
					found=1;
					break; //if and/or when you find a duplicate, you don't need to search anymore
				}
			if(found==0){//do not save it if you found a duplicate
				border_copy[k][0]=border[i][0];
				border_copy[k][1]=border[i][1];
				k++;
			}
		}
		//get smaller array
		//==============================================
		bsize = k;
		int[][] border2 = new int[bsize][2];
		for(k=0; k<bsize; k++){
			border2[k][0]=border_copy[k][0];
			border2[k][1]=border_copy[k][1];
		}
		return border2;
	}
	
	//Sorts (inefficiently) an array such that ys are sorted in increasing order
	//and for the same y, xs are sorted in increasing order (bubble sort)
	private int[][] xySort(int[][] array){
		int len=array.length;
		int aux;
		for (int i=len-1; i>0; i--)
			for (int j=1; j<=i; j++)
				if (array[j-1][0] > array[j][0]){
					aux = array[j-1][0];
					array[j-1][0] = array[j][0];
					array[j][0] = aux;
					aux = array[j-1][1];
					array[j-1][1] = array[j][1];
					array[j][1] = aux;
				}
		for (int i=len-1; i>0; i--)
			for (int j=1; j<=i; j++)
				if (array[j-1][1] > array[j][1]){
					aux = array[j-1][0];
					array[j-1][0] = array[j][0];
					array[j][0] = aux;
					aux = array[j-1][1];
					array[j-1][1] = array[j][1];
					array[j][1] = aux;
				}
		return array;
	}

	//Gets the coordinates of the centroid
	private int[] getCentroid(int s, int csize, int[][] contour){
		int[] centroid=new int[2];//x and y coordinates
		int sumx=0, sumy=0;
		for(int k=s; k<csize; k++){
			sumx+=contour[k][0];
			sumy+=contour[k][1];
		}
		centroid[0]=sumx/(csize-s);
		centroid[1]=sumy/(csize-s);
		return centroid;
	}

	//Find the circle fit through contours using Kasa fit
	private int[] fitCircle(int[][] points) {//the xy-coords of the border
		int len = points.length;
		//the Kasa method implies matrix operations which can be found 
		//in the Jama Matrix package
		
		double p1[][] = new double[len][3];//the third column is all 1s
		double p2[][] = new double[len][1];//x^2+y^2; Jama needs double[][]
		for(int i=0; i<len; i++){
			p1[i][0]=(double)points[i][0];
			p1[i][1]=(double)points[i][1];
			p1[i][2]=1;
			p2[i][0]=p1[i][0]*p1[i][0]+p1[i][1]*p1[i][1];
		}
		
		//transform p1 into a Matrix
		Matrix P1 = new Matrix(p1);
		Matrix P2 = new Matrix(p2);
		
		//we need to find the solution p
		//to A1.P = P2, that is p = inv(P1).P2 
		Matrix P = P1.inverse().times(P2);
		
		//get the centers and the radii
		double t1 = P.get(0,0);
		double t2 = P.get(1,0);
		double R = Math.sqrt((t1*t1+t2*t2)/4 + P.get(2,0));
		double xc = t1/2;
		double yc = t2/2;
		
		// centAndRad returns the x-coord of the center,
		// the y-coord of the center
		// and the radius of the algebraically fit circle
		int[] centAndRad = new int[3]; 
		centAndRad[0]=(int)xc;
		centAndRad[1]=(int)yc;
		centAndRad[2]=(int)R;
		
		return centAndRad;
	    }
	
	//Gets the closest points on each side of the border in the 2nd region
	// the center is the centroid of these points and the radius is half their distance 
	private int[] closestPoints(int[][] contour2){
		int size = contour2.length;
		int[][]left = new int[size/2][2];//left side
		int[][]right = new int[size/2][2];//right side
		int j=0;
		for(int i=0; i<size; i+=2){
			left[j][0]=contour2[i][0];
			left[j][1]=contour2[i][1];
			right[j][0]=contour2[i+1][0];
			right[j][1]=contour2[i+1][1];
			j++;
		}
		double mindist=999999;
		double dist;
		int p1x=0, p2x=0, p1y=0, p2y=0;//x and y for the minimum 
		for(int i=0;i<size/2; i++)
			for(j=0;j<size/2; j++){
				dist = getD((double)left[i][0],(double)left[i][1],(double)right[j][0],(double)right[j][1]);
				if (dist <= mindist){
					mindist = dist;
					p1x=left[i][0];//x for left
					p1y=left[i][1];//y for left
					p2x=right[i][0];//x for right
					p2y=right[i][1];//y for right
				}
			}
		double xc = (double)(p2x+p1x)/2;
		double yc = (double)(p2y+p2x)/2;
		double R = getD((double)p1x,(double)p1y,xc,yc);

		// centAndRad returns the x-coord of the center,
		// the y-coord of the center
		// and the radius 
		int[] centAndRad = new int[3]; 
		
		centAndRad[0]=(int)xc;
		centAndRad[1]=(int)yc;
		centAndRad[2]=(int)R;
		
		return centAndRad;
	}
	
	//Gets thedistance between two points
	private double getD(double x1, double y1, double x2, double y2){
		return Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
	}
	
	//Draws the contours we have
	//bordercolor, bonecolor, backgroundcolor, headcolor, neck1color, neck2color
	private void drawContours(int[][] border, int[][] contour1, int[][] contour2, ImageProcessor ip, int bdcolor, int bonecolor, int bgcolor, int hcolor, int n1color, int n2color){
		int[] img = (int[])ip.getPixels();
		int width = ip.getWidth();
		int height = ip.getHeight();
		int i;
		
		//set the borders to black
		for(i=0; i<border.length; i++)
			img[border[i][1]*width+border[i][0]] = bdcolor;

		//set bgcolor background and bonecolor bone
		for(int y=0; y<height; y++)
			for(int x=0; x<width; x++){
				if(img[y * width + x]==one)
					img[y * width + x]=bgcolor;
				if(img[y * width + x]==zero)
					img[y * width + x]=bonecolor;
			}

		//first contour
		for(i=0; i < contour1.length; i++){
			img[contour1[i][1] * width + contour1[i][0]] = hcolor;
		}
		//second contour
		for(i=0; i<contour2.length; i++){
			img[contour2[i][1] * width + contour2[i][0]] = n1color; //y * width + x
			i++;
			img[contour2[i][1] * width + contour2[i][0]] = n2color; //y * width + x
		}

	}

	//Draws the circle
	private void drawCircle(int[] centAndRad, ImageProcessor ip, int color){
		int[] img = (int[])ip.getPixels();
		int width = ip.getWidth();
		int height = ip.getHeight();
		
		//extract the circle center and radius, for readability
		int xc=centAndRad[0];
		int yc=centAndRad[1];
		int R=centAndRad[2];
		//draw the circle
		// we need points that fit the circle equation
		// (x-xc)^2+(y-yc)^2 = R
		double eq;
		for(int y=0; y<height; y++)
			for(int x=0; x<width; x++){
				eq=Math.sqrt((double)((x-xc)*(x-xc)+(y-yc)*(y-yc)));
				if( eq < (double)R+1 && eq >= R)
					img[y * width + x] = color;
			}
		img[yc * width + xc] = black;
	}

	private void drawAngle(int x1, int y1, int x2, int y2, ImageProcessor ip, int color){
		int[] img = (int[])ip.getPixels();
		int width = ip.getWidth();
		int height = ip.getHeight();
		
		double m=(y2-y1)/(x2-x1);
		double b=y2-m*x2;
		double x_;//x_ = (y-b)/m
		for(int y=0; y<height; y++)
			for(int x=0; x<width; x++){
				x_ = (y-b)/m;
				if((y==y1 && x<=x1) || (x < x_+1 && x >= x_ && y > y1))
					img[y * width + x] = color;
			}
	}

	//Gets the angle
	private double getAngle(int x1, int y1, int x2, int y2){
		double angle=0;
		double t;
		t=(double)(y2-y1)/(double)(x2-x1);
		angle=Math.atan(t);
		return Math.PI-angle;
	}
}