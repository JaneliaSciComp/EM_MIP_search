//************************************************
// 
// Written by Hideo Otsuna (HHMI Janelia inst.)
// Oct 2019
// 
//**************************************************

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.plugin.PlugIn;
import ij.plugin.frame.*; 
import ij.plugin.filter.*;
//import ij.plugin.Macro_Runner.*;
import ij.gui.GenericDialog.*;
import ij.macro.*;
import ij.measure.Calibration;
import ij.plugin.CanvasResizer;
import ij.plugin.Resizer;
import ij.util.Tools;
import ij.io.FileInfo;
import ij.io.TiffEncoder;
import ij.plugin.filter.GaussianBlur;

import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.ImageIO;

import java.io.*;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;
import java.io.IOException;
import java.io.File;
import java.nio.*;
import java.util.*;
import java.util.Iterator;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;


public class Gradient_conv_multithread implements PlugIn {
	
	int thread_num_=0, maxradius=0; 
	
	public void run(String arg) {
		
		
		thread_num_=(int)Prefs.get("thread_num_.int",4);
		maxradius=(int)Prefs.get("maxradius.int",20);
		
		GenericDialog gd = new GenericDialog("Gradient tiff save multi");
		
		gd.addNumericField("CPU number", thread_num_, 0);
		gd.addNumericField("Max radius", maxradius, 0);
		
		
		gd.showDialog();
		if(gd.wasCanceled()){
			return;
		}
		
		thread_num_ = (int)gd.getNextNumber();
		
		if(thread_num_ <= 0) thread_num_ = 1;
		Prefs.set("thread_num.int", thread_num_);
		
		Prefs.set("maxradius.int", maxradius);
		
		DirectoryChooser dirO = new DirectoryChooser("serial tiff for open directory");
		String Odirectory = dirO.getDirectory();
		
		DirectoryChooser dirs = new DirectoryChooser("Save directory");
		String Sdirectory = dirs.getDirectory();
		
		IJ.log("Odirectory; "+Odirectory+"   Sdirectory; "+Sdirectory);
		
		File OdirectoryFile = new File(Odirectory);
		final File names[] = OdirectoryFile.listFiles(); 
		Arrays.sort(names);
		
		IJ.log("names length;"+names.length);
		
		
		long timestart = System.currentTimeMillis();
		Threfunction(Odirectory,names,Sdirectory,thread_num_,maxradius);
		
		long timeend = System.currentTimeMillis();
		
		long gapS = (timeend-timestart)/1000;
		
		IJ.log("Done "+gapS+" second");
		
	} //public void run(String arg) {
	
	public void Threfunction (final String FOdirectory, File names[],final String SdirectoryF, final int thread_num_F, final int maxradiusF){
		
		final AtomicInteger ai = new AtomicInteger(0);
		final Thread[] threads = newThreadArray();
		
		
		IJ.log("thread_num_F; "+thread_num_F+"  FOdirectory; "+FOdirectory+"  SdirectoryF; "+SdirectoryF);
		
		
		for (int ithread = 0; ithread < threads.length; ithread++) {
			// Concurrently run in as many threads as CPUs
			threads[ithread] = new Thread() {
				
				{ setPriority(Thread.NORM_PRIORITY); }
				
				public void run() {
					
					for(int ii=ai.getAndIncrement(); ii<names.length; ii = ai.getAndIncrement()){
						
						
						//IJ.showProgress((double)iMIP/(double) names.length);
						IJ.showStatus(String.valueOf(ii));
						
						ImagePlus imp =null;// new ImagePlus();
						ImageProcessor ip=null;
						
						int tifposi = names[ii].getName().lastIndexOf("tif");
						
						if(tifposi>0){
							while(imp==null){
								imp = IJ.openImage(FOdirectory+names[ii].getName());
							}
							
					//		IJ.log("image depth1; "+imp.getType());
							
							ImageConverter ic = new ImageConverter(imp);
							ic.convertToGray8();
							
					//		IJ.log("image depth2; "+imp.getType());
							
						
							
							int [] info= imp.getDimensions();
							int WW = info[0];
							int HH = info[1];
							final int sumpx= WW*HH;
							
							ImageProcessor EightIMG = imp.getProcessor();
							
							for(int ix=WW-270; ix<WW; ix++){
								for(int iy=0; iy<100; iy++){
									
									EightIMG.set(ix,iy,-16777216);
								}
							}
							
							for(int ix=0; ix<330; ix++){
								for(int iy=0; iy<105; iy++){
									
									EightIMG.set(ix,iy,-16777216);
								}
							}
							
							
							IJ.run(imp,"Maximum...", "radius="+maxradiusF+"");
							
							ic = new ImageConverter(imp);
							ic.convertToGray16();
							EightIMG = imp.getProcessor();
						
							
							for(int ipix=0; ipix<sumpx; ipix++){// 255 binary mask creation, posi signal become 0 INV
								if(EightIMG.get(ipix)<1)
								EightIMG.set(ipix, 65535);
								else 
								EightIMG.set(ipix, 0);
								
							}
							
							
							EightIMG=gradientslice(EightIMG); // make gradient mask, EightIMG is 0 center & gradient mask of original mask 
							
							String namewrite = names[ii].getName();
							int dotindex= namewrite.lastIndexOf(".");
							namewrite= namewrite.substring(0, dotindex);
							
							
							try{ // Your output file or stream
								
								writeImage(imp, SdirectoryF+namewrite+".png");
							} catch (Exception e) {
								e.printStackTrace();
							}
							
							imp.unlock();
							imp.close();
							
							
						}//	if(tifposi>0){
						
					}//for(int ii=ai.getAndIncrement(); ii<FXYtotalbrick; ii = ai.getAndIncrement()){
			}};
		}//	for (int ithread = 0; ithread < threads.length; ithread++) {
		startAndJoin(threads);
		
	}
	
	public void writeImage (ImagePlus imp, String path) throws Exception{
		write16gs(imp, path);
		
	}
	
	private Thread[] newThreadArray() {
		int n_cpus = Runtime.getRuntime().availableProcessors();
		if (n_cpus > thread_num_) n_cpus = thread_num_;
		if (n_cpus <= 0) n_cpus = 1;
		return new Thread[n_cpus];
	}
	
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
	
	ImageProcessor gradientslice (ImageProcessor ipgra){
		
		int nextmin=0;	int Stop=0; int Pout=0;
		
		ImagePlus impfunc = new ImagePlus ("funIMP",ipgra);
		
		int Fmaxvalue=255;
		if(impfunc.getType()==0)
		Fmaxvalue=255;	
		else if(impfunc.getType()==1)
		Fmaxvalue=65535;	
		
	//	IJ.log("image depth; "+impfunc.getType()+"  ; "+Fmaxvalue);// 0=8bit, 1=16bit, 4=24bit
		
		int [] infof= impfunc.getDimensions();
		int width = infof[0];
		int height = infof[1];
		
		while(Stop==0){
			
			Stop=1;
			Pout=Pout+1; 
			
			
			//	IJ.log("run; "+ String.valueOf(try2)+"Fmaxvalue; "+String.valueOf(Fmaxvalue)+"  nextminF; "+String.valueOf(nextminF));
			
			for(int ix=0; ix<width; ix++){// x pixel shift
				//	IJ.log("ix; "+String.valueOf(ix));
				for(int iy=0; iy<height; iy++){
					
					int pix0=-1; int pix1=-1; int pix2=-1; int pix3=-1;
					
					int pix=ipgra.get(ix,iy);
					
					if(pix==Fmaxvalue){
						
						if(ix>0)
						pix0=ipgra.get(ix-1,iy);
						
						if(ix<width-1)
						pix1=ipgra.get(ix+1,iy);
						
						if(iy>0)
						pix2=ipgra.get(ix,iy-1);
						
						if(iy<height-1)
						pix3=ipgra.get(ix,iy+1);
						
						if(pix0==nextmin || pix1==nextmin || pix2==nextmin || pix3==nextmin){//|| edgepositive==1
							
							ipgra.set(ix,iy,Pout);
							Stop=0;
						}
					}//if(pix==Fmaxvalue){
					
				}//	for(int iy=0; iy<height; iy++){
			}//	for(int ix=0; ix<width; ix++){// x pixel shift
			nextmin=nextmin+1;
		}//	while(Stop==0){
		impfunc.unlock();
		impfunc.close();
		return ipgra;
	}
	void write16gs(ImagePlus imp, String path) throws Exception {
		ShortProcessor sp = (ShortProcessor)imp.getProcessor();
		BufferedImage bi = sp.get16BitBufferedImage();
		File f = new File(path);
		ImageIO.write(bi, "png", f);
	}
}




