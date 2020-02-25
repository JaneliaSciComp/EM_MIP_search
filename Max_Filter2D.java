import ij.*;
import ij.plugin.filter.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;
import ij.macro.*;
import ij.gui.GenericDialog.*;
import ij.io.*;

import java.io.File;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;


public class Max_Filter2D implements PlugInFilter{
	
	int thread_num_ = (int)Prefs.get("thread_num.int",4);
	ImagePlus imp;
	ImageProcessor ip1;
	
	public int setup(String arg, ImagePlus imp)
	{
		IJ.register (Max_Filter2D.class);
		if (IJ.versionLessThan("1.49d")){
			IJ.showMessage("Error", "Please Update ImageJ.");
			return 0;
		}
		
		this.imp = imp;
		
		return DOES_ALL;
	}
	
	
	public void run(ImageProcessor ip1) {
		
		imp = WindowManager.getCurrentImage();
		
		
		int Size = (int)Prefs.get("Size.int", 0);
		//	boolean OverOri = (boolean)Prefs.get("OverOri.boolean",false);
		int Scale = (int)Prefs.get("Scale.int", 1);
		//int	growd = (int)Prefs.get("growd.int",0);
		
		GenericDialog gd = new GenericDialog("MaxFilter2D");
		gd.addNumericField("Expansion size", Size, 0);
		gd.addNumericField("CPU threads", thread_num_, 0);
		gd.addSlider("XYScaling factor", 1, 4, Scale);
		
	//	String []	CalMST2 = {"XY", "YZ"};
	//	gd.addRadioButtonGroup("Growing direction", CalMST2, 0, 2, CalMST2[growd]);
		
		//gd.addCheckbox("OverWrite original images", OverOri);
		
		gd.showDialog();
		if (gd.wasCanceled()) return;
		
		Size = (int)gd.getNextNumber();
		thread_num_ = (int)gd.getNextNumber();
		Scale = (int)gd.getNextNumber();
		//		String STgrowd = (String)gd.getNextRadioButton();
		
		//	if(STgrowd=="XY")
		//		growd=0;
		//		else
		//	growd=1;
		
		//	OverOri=gd.getNextBoolean();//boolean Returns the state of the next checkbox.
		
		IJ.log("Expansion size; "+String.valueOf(Size)+"  Scale factor; "+String.valueOf(Scale));
		
		//	Prefs.set("OverOri.Boolean",OverOri);
		Prefs.set("Size.int", Size);
		Prefs.set("thread_num_.int", thread_num_);
	//	Prefs.set("growd.int",growd);
		
		
		if(thread_num_ <= 0) thread_num_ = 1;
		Prefs.set("thread_num.int", thread_num_);
		
		Prefs.set("Scale.int", Scale);
		
		long a=System.currentTimeMillis();
		
		imp=MaxF(imp,Size,Scale);
		imp.updateAndDraw();
		long b=System.currentTimeMillis();
		
		long gap=b-a;
		IJ.log("time; "+String.valueOf(gap/1000)+"  sec");
		//IJ.log("Conversion done");
	}
	
	
	ImagePlus MaxF(ImagePlus channels, int Size2, int Scale2){
		
		ImagePlus imp2 = new ImagePlus();
		//	ImagePlus[] imp2 = new ImagePlus[1];
		
		final int SizeF = Size2;
		final int Scale2F = Scale2;
		
		int slicenumber = channels.getStackSize();
		ImageProcessor ip2 = channels.getProcessor();
		int sumpx = ip2.getPixelCount();
		final int width = channels.getWidth();
		final int height = channels.getHeight();
		
		ImageStack st1 = channels.getStack();
		
		//int newwidth2=Math.round(width/2);
		//int newheight2=Math.round(height/2);
		
		//ImageStack st2 = new ImageStack (newwidth2, newheight2, slicenumber);
		
		final AtomicInteger ai = new AtomicInteger(1);
		final Thread[] threads = newThreadArray();
		final int sumpxF=sumpx;
		
		for (int ithread = 0; ithread < threads.length; ithread++) {
			threads[ithread] = new Thread() {
				
				{ setPriority(Thread.NORM_PRIORITY); }
				
				public void run() {
					
					//				if(STgrowd=="XY"){
					IJ.showProgress(0.0);
					for(int islice=ai.getAndIncrement(); islice<=slicenumber; islice = ai.getAndIncrement()){
						IJ.showProgress(islice/slicenumber);
						if(IJ.escapePressed())
						return;
						
						ImageProcessor ip3 = st1.getProcessor(islice);// data
						
						int newwidth=Math.round(width/Scale2F);
						int newheight=Math.round(height/Scale2F);
						int SizeF2=Math.round(SizeF/Scale2F);
						
						ip3 = ip3.resize(newwidth, newheight, false);			
						
						int sumpxF2 = ip3.getPixelCount();
						
						//		IJ.log("newwidth; "+String.valueOf(newwidth)+"  newheight"+String.valueOf(newheight)+"  SizeF2; "+String.valueOf(SizeF2));
						
						int finalpix=1;
						int inivalue=255;
						
						for(int isize=0; isize<SizeF2; isize++){
							for(int ix=1; ix<newwidth-1; ix++){
								for(int iy=1; iy<newheight-1; iy++){
									
									double pix1=ip3.get(ix, iy);
									
									if(pix1==0){
										double pix2=ip3.get(ix, iy-1);
										double pix3=ip3.get(ix, iy+1);
										
										double pix4=ip3.get(ix-1, iy);
										double pix5=ip3.get(ix-1, iy-1);
										double pix6=ip3.get(ix-1, iy+1);
										
										double pix7=ip3.get(ix+1, iy);
										double pix8=ip3.get(ix+1, iy-1);
										double pix9=ip3.get(ix+1, iy+1);
										
										if(pix2==inivalue)
										ip3.set(ix, iy,finalpix);
										else if(pix3==inivalue)
										ip3.set(ix, iy,finalpix);
										else if(pix4==inivalue)
										ip3.set(ix, iy,finalpix);
										else if(pix7==inivalue)
										ip3.set(ix, iy,finalpix);
										else if(pix5==inivalue)
										ip3.set(ix, iy,finalpix);
										else if(pix6==inivalue)
										ip3.set(ix, iy,finalpix);
										else if(pix8==inivalue)
										ip3.set(ix, iy,finalpix);
										else if(pix9==inivalue)
										ip3.set(ix, iy,finalpix);
									}
								}
							}//for(int ix=1; ix<width-1; ix++){
							
							int ix=0;
							for(int iy=1; iy<newheight-1; iy++){
								
								double pix1=ip3.get(ix, iy);
								
								if(pix1==0){
									double pix2=ip3.get(ix, iy-1);
									double pix3=ip3.get(ix, iy+1);
									
									double pix7=ip3.get(ix+1, iy);
									double pix8=ip3.get(ix+1, iy-1);
									double pix9=ip3.get(ix+1, iy+1);
									
									if(pix2==inivalue)
									ip3.set(ix, iy,finalpix);
									else if(pix3==inivalue)
									ip3.set(ix, iy,finalpix);
									else if(pix7==inivalue)
									ip3.set(ix, iy,finalpix);
									else if(pix8==inivalue)
									ip3.set(ix, iy,finalpix);
									else if(pix9==inivalue)
									ip3.set(ix, iy,finalpix);
								}
							}
							
							ix=newwidth-1;
							for(int iy=1; iy<newheight-1; iy++){
								
								double pix1=ip3.get(ix, iy);
								
								if(pix1==0){
									double pix2=ip3.get(ix, iy-1);
									double pix3=ip3.get(ix, iy+1);
									
									double pix4=ip3.get(ix-1, iy);
									double pix5=ip3.get(ix-1, iy-1);
									double pix6=ip3.get(ix-1, iy+1);
									
									if(pix2==inivalue)
									ip3.set(ix, iy,finalpix);
									else if(pix3==inivalue)
									ip3.set(ix, iy,finalpix);
									else if(pix4==inivalue)
									ip3.set(ix, iy,finalpix);
									else if(pix5==inivalue)
									ip3.set(ix, iy,finalpix);
									else if(pix6==inivalue)
									ip3.set(ix, iy,finalpix);
								}
							}//	for(int iy=1; iy<newheight-1; iy++){
							
							int iy=0;
							for(ix=1; ix<newwidth-1; ix++){
								
								double pix1=ip3.get(ix, iy);
								
								if(pix1==0){
									double pix3=ip3.get(ix, iy+1);
									double pix4=ip3.get(ix-1, iy);
									double pix6=ip3.get(ix-1, iy+1);
									double pix7=ip3.get(ix+1, iy);
									double pix9=ip3.get(ix+1, iy+1);
									
									if(pix3==inivalue)
									ip3.set(ix, iy,finalpix);
									else if(pix4==inivalue)
									ip3.set(ix, iy,finalpix);
									else if(pix7==inivalue)
									ip3.set(ix, iy,finalpix);
									else if(pix6==inivalue)
									ip3.set(ix, iy,finalpix);
									else if(pix9==inivalue)
									ip3.set(ix, iy,finalpix);
								}
							}//for(int ix=1; ix<width-1; ix++){
							
							iy=newheight-1;
							for(ix=1; ix<newwidth-1; ix++){
								
								double pix1=ip3.get(ix, iy);
								
								if(pix1==0){
									double pix2=ip3.get(ix, iy-1);
									double pix4=ip3.get(ix-1, iy);
									double pix5=ip3.get(ix-1, iy-1);
									double pix7=ip3.get(ix+1, iy);
									double pix8=ip3.get(ix+1, iy-1);
									
									
									if(pix2==inivalue)
									ip3.set(ix, iy,finalpix);
									else if(pix4==inivalue)
									ip3.set(ix, iy,finalpix);
									else if(pix7==inivalue)
									ip3.set(ix, iy,finalpix);
									else if(pix5==inivalue)
									ip3.set(ix, iy,finalpix);
									else if(pix8==inivalue)
									ip3.set(ix, iy,finalpix);
									
								}
							}//for(int ix=1; ix<newwidth-1; ix++){
							
							
							inivalue=finalpix;
							finalpix=finalpix+1;
							
						}//for(int isize=0; isize<SizeF; isize++){
						
						ip3 = ip3.resize(width, height, false);
						
						for(int ipix=0; ipix<sumpxF; ipix++){
							double pixC=ip3.get(ipix);
							if(pixC!=0)
							ip3.set(ipix,255);
						}
						
						st1.setProcessor(ip3, islice);
						//			System.gc();
					}//	for (int i = ai.getAndIncrement(); i < names.length; i = ai.getAndIncrement()) {
					//		}else{//if(STgrowd=="XY"){
					
			}};//threads[ithread] = new Thread() {
		}//	for (int ithread = 0; ithread < threads.length; ithread++) {
		startAndJoin(threads);
		//	channels.show();
		return channels;
		
		
	}//	ImagePlus MaxF(ImagePlus channels, int Size2, Boolean OverOri2){
	
	/** Create a Thread[] array as large as the number of processors available.
		* From Stephan Preibisch's Multithreading.java class. See:
		* http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD
		*/
	private Thread[] newThreadArray() {
		int n_cpus = Runtime.getRuntime().availableProcessors();
		if (n_cpus > thread_num_) n_cpus = thread_num_;
		if (n_cpus <= 0) n_cpus = 1;
		return new Thread[n_cpus];
	}
	
	/** Start all given threads and wait on each of them until all are done.
		* From Stephan Preibisch's Multithreading.java class. See:
		* http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD
		*/
	public static void startAndJoin(Thread[] threads)
	{
		for (int ithread = 0; ithread < threads.length; ++ithread)
		{
			threads[ithread].setPriority(Thread.NORM_PRIORITY);
			threads[ithread].start();
		}
		
		try
		{   
			for (int ithread = 0; ithread < threads.length; ++ithread)
			threads[ithread].join();
		} catch (InterruptedException ie)
		{
			throw new RuntimeException(ie);
		}
	}
	
	//	imp.show();
}







