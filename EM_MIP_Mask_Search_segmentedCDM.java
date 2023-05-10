import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ListIterator; 
import java.util.Iterator; 

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.awt.*;

import java.awt.image.*;
import javax.imageio.*;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

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
import java.nio.*;
import java.util.*;
import java.nio.charset.Charset;

import javax.imageio.ImageIO; 
import javax.imageio.ImageReader; 
import javax.imageio.stream.ImageInputStream; 
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.ImageTypeSpecifier;

import ij.*;
import ij.gui.*;
import ij.io.*;

import ij.plugin.filter.*;
import ij.process.*;
import ij.process.ImageProcessor;
//import ij.plugin.frame.RoiManager;

public class EM_MIP_Mask_Search_segmentedCDM implements PlugInFilter
{
	ImagePlus imp, imp2;
	ImageProcessor ipEMslice, nip1, ip2, ipLMsegSlice, ip4, ip5, ip6, ip33;
	int pix1=0, CheckPost,UniqueLineName=0,IsPosi,threadNumE=0,FLpositive=0;
	int pix3=0,Check=0,arrayPosition=0,dupdel=1,FinalAdded=1,enddup=0;
	ImagePlus newimp, newimpOri;
	String linename,LineNo, LineNo2,preLineNo="A",FullName,LineName,arrayName,PostName;
	String args [] = new String[10],PreFullLineName,ScorePre,TopShortLinename;
	String negativeradiusEM="10";
	ExecutorService m_executor;
	
	boolean DUPlogonE,EMhemibrain;
	
	public class SearchResult{
		String m_name;
		int m_sid;
		long m_offset;
		int m_slice_offset;
		int m_strip;
		int[] m_strip_offsets;
		int[] m_strip_lengths;
		byte[] m_pixels;
		byte[] m_colocs;
		ImageProcessor m_iporg;
		ImageProcessor m_ipcol;
		SearchResult(String name, int sid, long offset, int slice_offset, int strip, int[] strip_offsets, int[] strip_lengths, byte[] pxs, byte[] coloc, ImageProcessor iporg, ImageProcessor ipcol){
			m_name = name;
			m_sid = sid;
			m_offset = offset;
			m_slice_offset = slice_offset;
			m_strip = strip;
			m_strip_offsets = strip_offsets;
			m_strip_lengths = strip_lengths;
			m_pixels = pxs;
			m_colocs = coloc;
			m_iporg = iporg;
			m_ipcol = ipcol;
		}
	}
	
	class ByteVector {
		public byte[] data;
		private int size;
		
		public ByteVector() {
			data = new byte[10];
			size = 0;
		}
		
		public ByteVector(int initialSize) {
			data = new byte[initialSize];
			size = 0;
		}
		
		public ByteVector(byte[] byteBuffer) {
			data = byteBuffer;
			size = 0;
		}
		
		public void add(byte x) {
			if (size>=data.length) {
				doubleCapacity();
				add(x);
			} else
			data[size++] = x;
		}
		
		public int size() {
			return size;
		}
		
		public void add(byte[] array) {
			int length = array.length;
			while (data.length-size<length)
			doubleCapacity();
			System.arraycopy(array, 0, data, size, length);
			size += length;
		}
		
		void doubleCapacity() {
			byte[] tmp = new byte[data.length*2 + 1];
			System.arraycopy(data, 0, tmp, 0, data.length);
			data = tmp;
		}
		
		public void clear() {
			size = 0;
		}
		
		public byte[] toByteArray() {
			byte[] bytes = new byte[size];
			System.arraycopy(data, 0, bytes, 0, size);
			return bytes;
		}
	}
	
	public int packBitsUncompress(byte[] input, byte[] output, int offset, int expected) {
		if (expected==0) expected = Integer.MAX_VALUE;
		int index = 0;
		int pos = offset;
		while (pos < expected && pos < output.length && index < input.length) {
			byte n = input[index++];
			if (n>=0) { // 0 <= n <= 127
				byte[] b = new byte[n+1];
				for (int i=0; i<n+1; i++)
				b[i] = input[index++];
				System.arraycopy(b, 0, output, pos, b.length);
				pos += (int)b.length;
				b = null;
			} else if (n != -128) { // -127 <= n <= -1
				int len = -n + 1;
				byte inp = input[index++];
				for (int i=0; i<len; i++) output[pos++] = inp;
			}
		}
		return pos;
	}
	
	public int setup(String arg, ImagePlus imp)
	{
		IJ.register (EM_MIP_Mask_Search_segmentedCDM.class);
		if (IJ.versionLessThan("1.32c")){
			IJ.showMessage("Error", "Please Update ImageJ.");
			return 0;
		}
		
		int wList [] = WindowManager.getIDList();
		if (wList==null || wList.length<2) {
			IJ.showMessage("There should be at least two windows open; open the stack for search and a mask");
			return 0;
		}
		//	IJ.log(" wList;"+String.valueOf(wList));
		
		this.imp = imp;
		if(imp.getType()!=ImagePlus.COLOR_RGB){
			IJ.showMessage("Error", "Plugin requires RGB image");
			return 0;
		}
		return DOES_RGB;
		
		//	IJ.log(" noisemethod;"+String.valueOf(ff));
	}
	
	public String getZeroFilledNumString(int num, int digit) {
		String stri = Integer.toString(num);
		if (stri.length() < digit) {
			String zeros = "";
			for (int i = digit - stri.length(); i > 0; i--)
			zeros += "0";
			stri = zeros + stri;
		}
		return stri;
	}
	
	public String getZeroFilledNumString(double num, int decimal_len, int fraction_len) {
		String strd = String.format("%."+fraction_len+"f", num);
		int decdigit = strd.length()-(fraction_len+1);
		if (decdigit < decimal_len) {
			String zeros = "";
			for (int i = decimal_len - decdigit; i > 0; i--)
			zeros += "0";
			strd = zeros + strd;
		}
		return strd;
	}
	
	public void run(ImageProcessor ip){
		
		int wList [] = WindowManager.getIDList();
		if (wList==null || wList.length<2) {
			IJ.showMessage("There should be at least two windows open");
			return;
		}
		int imageno = 0; int SingleSliceMIPnum=0; int MultiSliceStack=0;
		String titles [] = new String[wList.length];
		int slices [] = new int[wList.length];
		
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			if (imp!=null){
				titles[i] = imp.getTitle();//Mask.tif and Data.tif
				slices[i] = imp.getStackSize();
				
				if(slices[i]>1){
					titles[i] = titles[i]+"  ("+slices[i]+") slices";
					MultiSliceStack = i;
				}else{
					titles[i] = titles[i]+"  ("+slices[i]+") slice";
					SingleSliceMIPnum = i;
				}
				imageno = imageno +1;
			}else
			titles[i] = "";
		}
		
		String[] negtitles = new String[titles.length+1];
		negtitles[0] = "none";
		System.arraycopy(titles, 0, negtitles, 1, titles.length);
		
		/////Dialog//////////////////////////////////////////////		
		int MaskE=(int)Prefs.get("MaskE.int",0);
		boolean mirror_maskE=(boolean)Prefs.get("mirror_maskE.boolean",false);
		int NegMaskE=(int)Prefs.get("NegMaskE.int",0);
		boolean mirror_negmaskE=(boolean)Prefs.get("mirror_negmaskE.boolean",false);
		int LMsegStack=(int)Prefs.get("LMsegStack.int",1);
		int ThresE=(int)Prefs.get("ThresE.int",100);
		double pixThresE=(double)Prefs.get("pixThresE.double",1);
		int duplineE=(int)Prefs.get("duplineE.int",1);
		int colormethodE=(int)Prefs.get("colormethodE.int",1);
		double pixfluE=(double)Prefs.get("pixfluE.double",1);
		int xyshiftE=(int)Prefs.get("xyshiftE.int",0);
		boolean logonE=(boolean)Prefs.get("logonE.boolean",false);
		int ThresmE=(int)Prefs.get("ThresmE.int",50);
		int NegThresmE=(int)Prefs.get("NegThresmE.int",50);
		boolean logNanE=(boolean)Prefs.get("logNanE.boolean",false);
		int labelmethodE=(int)Prefs.get("labelmethodE.int",0);
		boolean DUPlogonE=(boolean)Prefs.get("DUPlogonE.boolean",false);
		boolean GCONE=(boolean)Prefs.get("GCONE.boolean",false);
		boolean ShowCoE=(boolean)Prefs.get("ShowCoE.boolean",false);
		int NumberSTintE=(int)Prefs.get("NumberSTintE.int",0);
		threadNumE=(int)Prefs.get("threadNumE.int",8);
		String gradientDIR_=(String)Prefs.get("gradientDIR_.String","");
		boolean GradientOnTheFly_ = (boolean)Prefs.get("GradientOnTheFly_.boolean", false);
		int maxnumber=(int)Prefs.get("maxnumber.int",100);
		boolean shownormal=(boolean)Prefs.get("shownormal.boolean",false);
		String showFlip=(String)Prefs.get("showFlip.String","None");
		boolean maskiscom=(boolean)Prefs.get("maskiscom.boolean",false);
		negativeradiusEM = (String)Prefs.get("negativeradiusEM.String","10");
		EMhemibrain=(boolean)Prefs.get("EMhemibrain.boolean",false);
		
		int OriginalCDM=(int)Prefs.get("OriginalCDM.int",1);
		
		if(LMsegStack >= imageno){
			int singleslice=0; int Maxsingleslice=0; int MaxStack=0;
			
			for(int isliceSearch=0; isliceSearch<wList.length; isliceSearch++){
				singleslice=slices[isliceSearch];
				
				if(singleslice>Maxsingleslice){
					Maxsingleslice=singleslice;
					MaxStack=isliceSearch;
				}
			}
			LMsegStack=MaxStack;
		}
		
		if(NegMaskE >= imageno+1)
		NegMaskE = 0;
			
	if(MaskE>= imageno)
		MaskE=1;
		
		ImagePlus impMask = WindowManager.getImage(wList[MaskE]);
		int MaskSliceNum = impMask.getStackSize();
		
		ImagePlus impData = WindowManager.getImage(wList[LMsegStack]);
		int DataSliceNum = impData.getStackSize();
		
		//	IJ.log("mask; "+String.valueOf(MaskE)+"LMsegStack; "+String.valueOf(LMsegStack)+"imageno; "+String.valueOf(imageno)+"wList.length; "+String.valueOf(wList.length));
		if(labelmethodE>1)
		labelmethodE=1;
		
		GenericDialog gd = new GenericDialog("EM_MIP_Mask search segmented CDM");
		gd.addChoice("EM_stack or slice", titles, titles[MaskE]); //MaskE
		//	gd.addSlider("1.Threshold for EM_stack", 0, 255, ThresmE);
		gd.setInsets(0, 310, 0);
		//	gd.addCheckbox("1.Add mirror search", mirror_maskE);
		
		//	String []	com = {"ShowFlip hits on a same side (Not for commissure)", "ShowComissure matching (Bothside commissure)","None"};
		//	gd.setInsets(0, 180, 0);
		//		gd.addRadioButtonGroup("Show special matching: ", com, 1, 3, showFlip);
		
		gd.setInsets(20, 0, 0);
		//		gd.addChoice("Negative Mask", negtitles, negtitles[NegMaskE]); //Negative MaskE
		//		gd.addSlider("2.Threshold for negative mask", 0, 255, NegThresmE);
		//	gd.setInsets(0, 310, 0);
		//	gd.addCheckbox("2.Add mirror search", mirror_negmaskE);
		
		gd.setInsets(20, 0, 0);
		gd.addChoice("LM_stack for the search", titles, titles[LMsegStack]); //Data
		
		gd.setInsets(20, 0, 0);
		gd.addChoice("Original_CDM_result", titles, titles[OriginalCDM]); //Data
		
		//	gd.setInsets(20, 310, 0);// top, left, bottom
		//	gd.addCheckbox("This is EM_hemibrain", EMhemibrain);
		
		String []	com2 = {"10","5"};//"ShowComissure matching (Bothside commissure)"
		//	gd.setInsets(0, 305, 0);
		//	gd.addRadioButtonGroup("Negative score region radius: px ", com2, 1, 2, negativeradiusEM);
		
		gd.setInsets(20, 0, 0);
		gd.addNumericField("Positive PX % Threshold: EM matching is 0.5-1.5%", pixThresE, 4);
		gd.addSlider("Pix Color Fluctuation, +- Z slice", 0, 10, pixfluE);
		
		gd.setInsets(20, 220, 0);// top, left, bottom
		gd.addCheckbox("Show Normal_MIP_search_result before the shape matching", shownormal);
		//		gd.addCheckbox("GradientOnTheFly; (slower with ON)",GradientOnTheFly_);
		//		gd.setInsets(20, 0, 0);
		//	gd.addNumericField("Max number of the hits", maxnumber, 0);
		
		gd.setInsets(20, 0, 0);
		gd.addNumericField("Thread", threadNumE, 0);
		
		gd.setInsets(0, 362, 5);
		String []	shitstr = {"0px    ", "2px    ", "4px    "};
		gd.addRadioButtonGroup("XY Shift: ", shitstr, 1, 3, shitstr[xyshiftE/2]);
		
		//	gd.setInsets(0, 362, 5);
		//	String []	NumberST = {"%", "absolute value"};
		//	gd.addRadioButtonGroup("Scoring method; ", NumberST, 1, 2, NumberST[NumberSTintE]);
		
		gd.setInsets(20, 372, 0);
		gd.addCheckbox("ShowLog",logonE);
		
		gd.setInsets(20, 372, 0);
		gd.addCheckbox("Clear memory before search. Slow at beginning but fast search",GCONE);
		
		//	gd.setInsets(20, 372, 0);
		//	gd.addCheckbox("Co-localized stack shown (more memory needs)",ShowCoE);
		
		gd.showDialog();
		if(gd.wasCanceled()){
			return;
		}
		
		MaskE = gd.getNextChoiceIndex(); //MaskE
		LMsegStack = gd.getNextChoiceIndex(); //Color MIP
		OriginalCDM= gd.getNextChoiceIndex(); //Color MIP
		
		negativeradiusEM = "10";
		pixThresE=(double)gd.getNextNumber();
		pixfluE=(double)gd.getNextNumber();
		GradientOnTheFly_ = true;//gd.getNextBoolean();
		shownormal = gd.getNextBoolean();
		maxnumber=400;
		duplineE=0;
		DUPlogonE = true;
		threadNumE = (int)gd.getNextNumber();
		xyshiftE=Integer.parseInt( ((String)gd.getNextRadioButton()).substring(0,1) );
		
		String thremethodSTR="Two windows";
		String labelmethodSTR="overlap value + line name";
		String ScoringM="%";//(String)gd.getNextRadioButton();
		logonE = gd.getNextBoolean();
		GCONE = gd.getNextBoolean();
		ShowCoE = false;
		boolean EMsearch = true;
		
		IJ.log("threadNumE; "+threadNumE);
		
		Prefs.set("GradientOnTheFly_.boolean", GradientOnTheFly_);
		
		if(GCONE==true){
			//		XX:+UseG1GC;
			System.gc();
			System.gc();
			//		-XX:+UnlockExperimentalVMOptions;
			//		-XX:InitiatingHeapOccupancyPercent=5;
		}
		
		if(logonE==true){
			GenericDialog gd2 = new GenericDialog("log option");
			gd2.addCheckbox("ShowNaN",logNanE);
			
			gd2.showDialog();
			if(gd2.wasCanceled()){
				return;
			}
			String logmethodSTR=(String)gd2.getNextRadioButton();
			logNanE = gd2.getNextBoolean();
			Prefs.set("logNanE.boolean",logNanE);
		}//if(logonE==true){
		
		colormethodE=1;
		if(thremethodSTR=="Combine")
		colormethodE=0;
		
		
		if(labelmethodSTR=="overlap value")
		labelmethodE=0;//on top
		if(labelmethodSTR=="overlap value + line name")
		labelmethodE=1;
		
		if(ScoringM=="%")
		NumberSTintE=0;
		else
		NumberSTintE=1;
		
		Prefs.set("MaskE.int", MaskE);
		Prefs.set("mirror_maskE.boolean", mirror_maskE);
		Prefs.set("mirror_negmaskE.boolean", mirror_negmaskE);
		Prefs.set("ThresmE.int", ThresmE);
		Prefs.set("NegMaskE.int", NegMaskE);
		Prefs.set("NegThresmE.int", NegThresmE);
		Prefs.set("pixThresE.double", pixThresE);
		Prefs.set("ThresE.int", ThresE);
		Prefs.set("LMsegStack.int",LMsegStack);
		Prefs.set("colormethodE.int",colormethodE);
		Prefs.set("pixfluE.double", pixfluE);
		Prefs.set("xyshiftE.int",xyshiftE);
		Prefs.set("logonE.boolean",logonE);
		Prefs.set("labelmethodE.int",labelmethodE);
		Prefs.set("DUPlogonE.boolean",DUPlogonE);
		Prefs.set("duplineE.int",duplineE);
		Prefs.set("GCONE.boolean",GCONE);
		Prefs.set("ShowCoE.boolean",ShowCoE);
		Prefs.set("NumberSTintE.int",NumberSTintE);
		Prefs.set("threadNumE.int",threadNumE);
		Prefs.set("maxnumber.int",maxnumber);
		Prefs.set("shownormal.boolean",shownormal);
		Prefs.set("showFlip.String",showFlip);
		Prefs.set("negativeradiusEM.String",negativeradiusEM);
		Prefs.set("EMhemibrain.boolean", EMhemibrain);
		Prefs.set("OriginalCDM.int",OriginalCDM);
		
		
		double pixfludub=pixfluE/100;
		//	IJ.log(" pixfludub;"+String.valueOf(pixfludub));
		
		final double pixThresdub = pixThresE/100;///10000
		//	IJ.log(" pixThresdub;"+String.valueOf(pixThresdub));
		///////		
		ImagePlus impEMstack = WindowManager.getImage(wList[MaskE]); //MaskE
		ImagePlus inegmask = NegMaskE > 0 ? WindowManager.getImage(wList[NegMaskE-1]) : null; //Negative MaskE
		titles[MaskE] = impEMstack.getTitle();
		if (inegmask != null) negtitles[NegMaskE] = inegmask.getTitle();
		ImagePlus impLMsegstack = WindowManager.getImage(wList[LMsegStack]); //Data
		ImagePlus impOriginalCDM = WindowManager.getImage(wList[OriginalCDM]); //Data
		
		final ImageStack stEMstack = impEMstack.getStack();
		
		nip1 = NegMaskE > 0 ? inegmask.getProcessor() : null; //Negative MaskE
		int slicenumber = impLMsegstack.getStackSize();
		
		int width = impEMstack.getWidth();
		int height = impEMstack.getHeight();
		int slicenum = impEMstack.getStackSize();
		
		int widthD = impLMsegstack.getWidth();
		int heightD = impLMsegstack.getHeight();
		
		if(width!=widthD){
			IJ.showMessage ("Image size is different between the mask and data!  mask width; "+width+" px   data width; "+widthD+" px");
			IJ.log("Image size is different between the mask and data!");
			return;
		}
		
		if(height!=heightD){
			IJ.showMessage ("Image size is different between the mask and data!  mask height; "+height+" px   data height; "+heightD+" px");
			IJ.log("Image size is different between the mask and data!");
			return;
		}
		
		if(IJ.escapePressed())
		return;
		
		
		IJ.showProgress(0.0);
		
		//	IJ.log("maxvalue; "+maxvalue2+"	 gap;	"+gap);
		
		
		final ImageStack stLMsegstack = impLMsegstack.getStack();
		int posislice = 0;
		
		double posipersent2 = 0;
		double pixThresdub2 = 0;
		
		m_executor = Executors.newFixedThreadPool(threadNumE);
		
		long start, end;
		start = System.currentTimeMillis();
		
		IJ.log("  stLMsegstack.isVirtual(); "+stLMsegstack.isVirtual());
		String fileformat="";
		
		ArrayList<String> srlabels = new ArrayList<String>();
		ArrayList<String> finallbs = new ArrayList<String>();
		HashMap<String, SearchResult> srdict = new HashMap<String, SearchResult>(1000);
		
		final int fslicenum = slicenumber;
		final int fthreadnum = threadNumE;
		final boolean fShowCo = ShowCoE;
		final boolean flogon = logonE;
		final boolean flogNan = logNanE;
		final int fNumberSTint = NumberSTintE;
		final int flabelmethod = labelmethodE;
		final boolean isPackbits = fileformat.equals("tif PackBits");
		final int ThresmEF=ThresmE;
		final double pixfludubF=pixfludub;
		final int xyshiftEF=xyshiftE;
		
		int CDMrun=0;
		if(slicenum==1)
		CDMrun=1;
		
		if(CDMrun==1){
			
			ipEMslice = impEMstack.getProcessor();
			
			for(int ix=width-270; ix<width; ix++){// deleting color scale from mask
				for(int iy=0; iy<90; iy++){
					ipEMslice.set(ix,iy,-16777216);
				}
			}
			
			if(width<height){// VNC
				for(int ix=0; ix<291; ix++){// deleting color scale from mask
					for(int iy=0; iy<90; iy++){
						ipEMslice.set(ix,iy,-16777216);
					}
				}
			}
			
			//final ColorMIPMaskCompare cc = new ColorMIPMaskCompare (ipEMslice, ThresmE, mirror_maskE, nip1, NegThresmE, mirror_negmaskE, ThresE, pixfludub, xyshiftE);
			final ColorMIPMaskCompare cc = new ColorMIPMaskCompare (ipEMslice, ThresmEF, false, null, 10, false, 20, pixfludubF, xyshiftEF);
			
			m_executor = Executors.newFixedThreadPool(threadNumE);
			
			IJ.log("getProcessor run");		
			final List<Callable<ArrayList<SearchResult>>> tasks = new ArrayList<Callable<ArrayList<SearchResult>>>();
			for (int ithread = 0; ithread < threadNumE; ithread++) {
				final int ftid = ithread;
				final int f_th_snum = fslicenum/fthreadnum;
				tasks.add(new Callable<ArrayList<SearchResult>>() {
						public ArrayList<SearchResult> call() {
							ArrayList<SearchResult> out = new ArrayList<SearchResult>();
							for (int slice = fslicenum/fthreadnum*ftid+1, count = 0; slice <= fslicenum && count < f_th_snum; slice++, count++) {
								if( IJ.escapePressed() )
								break;
								if (ftid == 0)
								IJ.showProgress((double)slice/(double)f_th_snum);
								
								//	ipEMslice = stEMstack.getProcessor(slice); //MaskE
								//	String EMlinename = stEMstack.getSliceLabel(slice);
								//	ColorMIPMaskCompare cc = new ColorMIPMaskCompare (ipEMslice, ThresmEF, false, null, 10, false, 20, pixfludubF, xyshiftEF);
								
								ColorProcessor ipnew = null;
								if (fShowCo) ipnew = new ColorProcessor(width, height);
								
								ImageProcessor ipLMsegSlice = stLMsegstack.getProcessor(slice);
								String linename = stLMsegstack.getSliceLabel(slice);
								
								int FLindex= linename.lastIndexOf("_FL");
								if(FLindex>0)
								linename=linename.substring(0,FLindex);
								
								ColorMIPMaskCompare.Output res = cc.runSearch(ipLMsegSlice, ipnew);
								
								int posi = res.matchingPixNum;
								double posipersent = res.matchingPct;
								
								if(posipersent<=pixThresdub){
									if (flogon==true && flogNan==true)
									IJ.log("NaN");
								}else if(posipersent>pixThresdub){
									double posipersent3=posipersent*100;
									double pixThresdub3=pixThresdub*100;
									
									posipersent3 = posipersent3*100;
									posipersent3 = Math.round(posipersent3);
									double posipersent2 = posipersent3 /100;
									
									pixThresdub3 = pixThresdub3*100;
									pixThresdub3 = Math.round(pixThresdub3);
									
									if(flogon==true && flogNan==true)// sort by name
									IJ.log("Positive linename; 	"+linename+" 	"+String.valueOf(posipersent2));
									
									String title="";
									if(fNumberSTint==0){
										String numstr = getZeroFilledNumString(posipersent2, 3, 2);
										title = (flabelmethod==0 || flabelmethod==1) ? numstr+"_"+linename : linename+"_"+numstr;
									}
									else if(fNumberSTint==1) {
										String posiST=getZeroFilledNumString(posi, 4);
										title = (flabelmethod==0 || flabelmethod==1) ? posiST+"_"+linename : linename+"_"+posiST;
									}
									out.add(new SearchResult(title, slice, 0L, 0, 0, null, null, null, null, ipLMsegSlice, ipnew));
									if (ftid == 0)
									IJ.showStatus("Number of Hits (estimated): "+out.size()*fthreadnum);
								}
							}
							return out;
						}
				});
			}
			try {
				List<Future<ArrayList<SearchResult>>> taskResults = m_executor.invokeAll(tasks);
				for (Future<ArrayList<SearchResult>> future : taskResults) {
					for (SearchResult r : future.get()) {
						srlabels.add(r.m_name);
						srdict.put(r.m_name, r);
						
						int FLindex = r.m_name.lastIndexOf("_FL");
						
						//IJ.log("r.m_name; "+r.m_name);
						
						if(FLindex!=-1){
							FLpositive=FLpositive+1;
							
						}
						posislice=posislice+1;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
			long mid= System.currentTimeMillis();
			long gapmid=(mid-start)/1000;
			
			IJ.showStatus("Number of Hits: "+String.valueOf(posislice));
			IJ.log(" positive slice No.;"+String.valueOf(posislice)+" FL positive; "+FLpositive+"  sec; "+gapmid);
			
			int PositiveSlices=posislice;
			
			String OverlapValueLineArray [] = new String [posislice];
			for(int format=0; format<posislice; format++)
			OverlapValueLineArray [format]="0";
			
			String LineNameArray [] = new String[posislice];
			
			if(posislice>0){// if result is exist
				int posislice2=posislice;
				
				if(posislice==1)
				duplineE=0;
				String linenameTmpo;
				
				for(int CreateLineArray=0; CreateLineArray<posislice; CreateLineArray++){
					linenameTmpo = srlabels.get(CreateLineArray);
					
					int DotPosi=(linenameTmpo.indexOf("."));
					
					LineNo=linenameTmpo.substring(0, DotPosi);
					
					
					String posipersent2ST;
					if(labelmethodE==0 || labelmethodE==1){// on top score
						int UnderS0=(linenameTmpo.indexOf("_"));
						posipersent2ST = linenameTmpo.substring(0, UnderS0);// VT00002
					}else{
						int UnderS0=(linenameTmpo.lastIndexOf("_"));
						
						posipersent2ST = linenameTmpo.substring(UnderS0, linenameTmpo.length());// VT00002
						
					}
					//posipersent2= Double.parseDouble(posipersent2ST);
					
					LineNameArray[CreateLineArray]=LineNo+","+posipersent2ST+","+linenameTmpo;
					
					//		IJ.log("linenameTmpo;dcstack "+linenameTmpo);
				}
				Arrays.sort(LineNameArray, Collections.reverseOrder());
				
				//// duplication check and create top n score line list ////////////////////////////////////
				if(duplineE!=0){
					
					//	LineNameArray[posislice]="Z,0,Z";
					
					
					//// scan complete line name list and copy the list to new positive list //////////////
					String[] FinalPosi = new String [posislice];
					int duplicatedLine=0;
					
					for(int LineInt=0; LineInt<posislice; LineInt++)
					
					if(DUPlogonE==true)
					//		IJ.log(LineInt+"  "+LineNameArray[LineInt]);
					
					for(int Fposi=0; Fposi<=posislice; Fposi++){
						
						//////// pre line name ///////////////////////////
						if(Fposi>0){
							String arrayNamePre=LineNameArray[Fposi-1];
							
							arrayPosition=0;
							for (String retval: arrayNamePre.split(",")){
								args[arrayPosition]=retval;
								arrayPosition=arrayPosition+1;
							}
							
							preLineNo = args[0];// LineNo
						}
						
						///// current line name //////////////////////////
						if(Fposi<posislice){
							String arrayName=LineNameArray[Fposi];
							//		IJ.log("Original array; "+arrayName);
							
							arrayPosition=0;
							for (String retval: arrayName.split(",")){
								args[arrayPosition]=retval;
								arrayPosition=arrayPosition+1;
							}
							LineNo2 = args[0];// LineNo
							linename = args[2];//linename
						}
						
						if(Fposi==0)
						preLineNo=LineNo2;
						
						if(Fposi==posislice)
						LineNo2="End";
						
						//		IJ.log("LineNo 662; 	"+LineNo2+"   preLineNo; "+preLineNo+ "   duplicatedLine; "+duplicatedLine); // Sort OK!
						
						Check=-1;
						Check=(LineNo2.indexOf(preLineNo));
						
						//		IJ.log("Check; "+Check);
						
						if(Check!=-1 && Fposi!=0){
							duplicatedLine=duplicatedLine+1;
							//		IJ.log("Duplicated");
						}
						
						if(Check==-1 && duplicatedLine>duplineE-1){// end of duplication, at next new file name
							if(DUPlogonE==true){
								IJ.log("");
								IJ.log("Line Duplication; "+String.valueOf(duplicatedLine+1));
							}
							String [] Battle_Values = new String [duplicatedLine+1];
							
							for(int dupcheck=1; dupcheck<=duplicatedLine+1; dupcheck++){
								arrayName=LineNameArray[Fposi-dupcheck];
								
								arrayPosition=0;
								for (String retval: arrayName.split(",")){
									args[arrayPosition]=retval;
									arrayPosition=arrayPosition+1;
								}
								
								Battle_Values [dupcheck-1] = args[1]+","+args[2];//score + fullname
								
								if(DUPlogonE==true)
								IJ.log("Overlap_Values; "+Battle_Values [dupcheck-1]);
								
							}//for(int dupcheck=1; dupcheck<=duplicatedLine+1; dupcheck++){
							
							//		Collections.reverse(Ints.asList(Battle_Values));
							Arrays.sort(Battle_Values, Collections.reverseOrder());
							
							for(int Endvalue=0; Endvalue<duplineE; Endvalue++){// scan from top value to the border
								
								for(int dupcheck=1; dupcheck<=duplicatedLine+1; dupcheck++){
									
									arrayPosition=0;
									for (String retval: LineNameArray[Fposi-dupcheck].split(",")){
										args[arrayPosition]=retval;
										arrayPosition=arrayPosition+1;
									}
									
									String OverValue = args[1];//posipersent2
									LineName = args[0];//posipersent2
									FullName = args[2];//posipersent2
									
									
									arrayPosition=0;
									for (String retval: Battle_Values[Endvalue].split(",")){
										args[arrayPosition]=retval;
										arrayPosition=arrayPosition+1;
									}
									
									String TopValue = args[0];//posipersent2
									String FullNameBattle = args[1];//posipersent2
									
									int BattleCheck=(FullNameBattle.indexOf(FullName));
									
									//	IJ.log("OverValue; "+OverValue+"  TopValue; "+TopValue);
									//	IJ.log("FullNameBattle; "+FullNameBattle+"  FullName; "+FullName);
									
									double OverValueDB = Double.parseDouble(OverValue);
									double OverValueBV = Double.parseDouble(TopValue);
									
									if(OverValueBV==OverValueDB && BattleCheck!=-1){
										
										//				IJ.log("OverValueSlice1; "+LineNameArray[Fposi-dupcheck]);
										
										if(labelmethodE==0)// Sort by value
										LineNameArray[Fposi-dupcheck]="10000000,10000000,100000000";//delete positive overlap array for negative overlap list
										
										if(labelmethodE==1){// Sort by value and line
											if(Endvalue==0){// highest value
												if(DUPlogonE==true)
												IJ.log(1+UniqueLineName+"  UniqueLineName; "+FullName);
												OverlapValueLineArray[UniqueLineName]=FullName+","+LineName;
												UniqueLineName=UniqueLineName+1;
											}
										}
										//				IJ.log("OverValueSlice2; "+LineNameArray[Fposi-dupcheck]);
										
										//		PositiveSlices=PositiveSlices-1;
									}
								}//for(int dupcheck=1; dupcheck<=duplicatedLine+1; dupcheck++){
							}//	for(int Endvalue=0; Endvalue<duplineE; Endvalue++){// scan from top value to the border
							
							duplicatedLine=0; Check=2; 
						}//if(preLineNo!=LineNo && duplicatedLine>duplineE-1){// end of duplication, at next new file name
						
						int initialNo=Fposi-duplicatedLine-1;
						if(initialNo>0 && Check==-1 && duplicatedLine<=duplineE-1){//&& CheckPost==-1
							
							for(int dupwithinLimit=1; dupwithinLimit<=duplicatedLine+1; dupwithinLimit++){
								
								if(DUPlogonE==true){
									IJ.log("");
									IJ.log("dupwithinLimit; "+LineNameArray[Fposi-dupwithinLimit]+"  dupwithinLimit; "+dupwithinLimit+"  duplicatedLine; "+duplicatedLine);
								}
								
								if(labelmethodE==0)// Sort by value
								LineNameArray[Fposi-dupwithinLimit]="10000000,10000000,100000000";// delete positive files within duplication limit from negative list
								
								else if (labelmethodE==1){// Sort by value and line
									
									arrayName=LineNameArray[Fposi-dupwithinLimit];
									
									arrayPosition=0;
									for (String retval: arrayName.split(",")){
										args[arrayPosition]=retval;
										arrayPosition=arrayPosition+1;
									}
									LineName = args[0];//posipersent2
									String ScoreCurrent= args[1];
									FullName = args[2];//posipersent2
									
									if(dupwithinLimit==2){/// 2nd file
										
										double CurrentScore = Double.parseDouble(ScoreCurrent);
										
										double PreScore = Double.parseDouble(ScorePre);
										
										if(DUPlogonE==true)
										IJ.log("CurrentScore; "+CurrentScore+"  PreScore; "+PreScore);
										
										if(CurrentScore>PreScore){
											
											if(DUPlogonE==true)
											IJ.log(1+UniqueLineName-1+"  UniqueLineName; "+FullName);
											OverlapValueLineArray[UniqueLineName-1]=FullName+","+LineName;
											
											LineNameArray[Fposi-dupwithinLimit+1]=LineName+","+ScoreCurrent+","+FullName;
											LineNameArray[Fposi-dupwithinLimit]=LineName+","+ScorePre+","+PreFullLineName;
											
										}else{
											if(DUPlogonE==true)
											IJ.log(1+UniqueLineName-1+"  UniqueLineName; "+PreFullLineName);
											//			OverlapValueLineArray[UniqueLineName]=PreFullLineName+","+LineName;
										}
										
									}//if(dupwithinLimit==2){
									
									
									if(dupwithinLimit==1){
										
										arrayName=LineNameArray[Fposi-dupwithinLimit-1];
										
										arrayPosition=0;
										for (String retval: arrayName.split(",")){
											args[arrayPosition]=retval;
											arrayPosition=arrayPosition+1;
										}
										String PreSLineName = args[0];//posipersent2
										
										ScorePre= ScoreCurrent;
										PreFullLineName = FullName;//posipersent2
										
										int PreCheck=(LineName.indexOf(PreSLineName));
										
										if(PreCheck==-1 && DUPlogonE==true)
										IJ.log(1+UniqueLineName+"  UniqueLineName; "+FullName);
										
										OverlapValueLineArray[UniqueLineName]=FullName+","+LineName;
										UniqueLineName=UniqueLineName+1;
										//			LineNameArray[Fposi-dupwithinLimit]="0";
									}//if(dupwithinLimit==1){
								}//else if (labelmethodE==2){// Sort by value and line
								
								//		PositiveSlices=PositiveSlices-1;
							}
							duplicatedLine=0;
						}//if(initialNo!=0 && preLineNo!=LineNo && duplicatedLine<=duplineE-1){
						
						if(initialNo<=0 && Check==-1 && duplicatedLine<=duplineE-1 && Fposi>0){// && CheckPost==-1
							for(int dupwithinLimit=1; dupwithinLimit<=duplicatedLine+1; dupwithinLimit++){
								
								if(DUPlogonE==true){
									IJ.log("");
									IJ.log("dupwithinLimit start; "+LineNameArray[Fposi-dupwithinLimit]);
								}
								if(labelmethodE==0)// Sort by value
								LineNameArray[Fposi-dupwithinLimit]="10000000,10000000,100000000";// delete positive files within duplication limit from negative list
								
								else if (labelmethodE==1){// Sort by value and line
									
									arrayName=LineNameArray[Fposi-dupwithinLimit];
									
									arrayPosition=0;
									for (String retval: arrayName.split(",")){
										args[arrayPosition]=retval;
										arrayPosition=arrayPosition+1;
									}
									LineName = args[0];//posipersent2
									FullName = args[2];//posipersent2
									
									if(dupwithinLimit==1){
										if(DUPlogonE==true)
										IJ.log(1+UniqueLineName+"  UniqueLineName; "+FullName);
										OverlapValueLineArray[UniqueLineName]=FullName+","+LineName;
										UniqueLineName=UniqueLineName+1;
										//			LineNameArray[Fposi-dupwithinLimit]="0";
									}
								}
								
								//				PositiveSlices=PositiveSlices-1;
							}
							
							duplicatedLine=0;
						}//if(initialNo<=0 && Check==-1 && duplicatedLine<=duplineE-1 && Fposi>0){
						
						//		preLineNo=LineNo2;
						
					}//for(int Fposi=0; Fposi<posislice; Fposi++){
					Arrays.sort(LineNameArray, Collections.reverseOrder());// negative list
					
				}//	if(duplineE!=0){
				
				if (labelmethodE==1)
				Arrays.sort(OverlapValueLineArray, Collections.reverseOrder());// top value list
				else
				UniqueLineName=posislice2-1;
				
				//		for(int overarray=0; overarray<UniqueLineName; overarray++){
				//			IJ.log("OverlapValueLineArray;  "+OverlapValueLineArray[overarray]);
				//		}
				
				/// sorting order /////////////////////////////////////////////////////////////////
				String[] weightposi = new String [posislice];
				for(int wposi=0; wposi<posislice; wposi++){
					weightposi[wposi] = srlabels.get(wposi);
				}
				Arrays.sort(weightposi, Collections.reverseOrder());
				
				if(DUPlogonE==true)
				IJ.log("UniqueLineName number; "+UniqueLineName);
				
				if(duplineE!=0){
					for(int wposi=0; wposi<UniqueLineName; wposi++){
						for(int slicelabel=1; slicelabel<=posislice2; slicelabel++){
							
							//		if(DUPlogonE==true)
							//		IJ.log("wposi; "+wposi);
							
							if (labelmethodE==1){
								arrayName=OverlapValueLineArray[wposi];
								//		IJ.log(" arrayName;"+arrayName);
								arrayPosition=0;
								for (String retval: arrayName.split(",")){
									args[arrayPosition]=retval;
									arrayPosition=arrayPosition+1;
								}
								String LineNameTop = args[0];// Full line name of OverlapValueLineArray
								TopShortLinename = args[1];//short linename for the file
								
								IsPosi=(LineNameTop.indexOf(srlabels.get(slicelabel-1)));
								
							}//if (labelmethodE==2){
							
							if (labelmethodE!=1)
							IsPosi=(weightposi[wposi].indexOf(srlabels.get(slicelabel-1)));
							
							
							//	IJ.log(IsPosi+" LineNameTop;"+LineNameTop+"  linename; "+linename+"   dcStack; "+dcStack.getSliceLabel(slicelabel));
							
							if(IsPosi!=-1){// if top value slice is existing in dsStack
								
								if (labelmethodE==1){
									// get line name from top value array//////////////////////				
									// get 2nd 3rd line name from deleted array //////////////////////			
									
									for(int PosiSliceScan=0; PosiSliceScan<PositiveSlices; PosiSliceScan++){
										
										String PositiveArrayName=LineNameArray[PosiSliceScan];
										arrayPosition=0;
										for (String retval: PositiveArrayName.split(",")){
											args[arrayPosition]=retval;
											arrayPosition=arrayPosition+1;
										}
										
										String linenamePosi2 = args[0];//linename
										String FullLinenamePosi2 = args[2];//linename
										
										int PosiLine=(TopShortLinename.indexOf(linenamePosi2));//Top lineName and LineNameArray
										
										//		IJ.log(PosiLine+"  917 LineNameTop;"+LineNameTop+"  linename; "+linename+"   linenamePosi2; "+linenamePosi2+"   AlreadyAdded; "+AlreadyAdded+"  slicelabel; "+slicelabel);
										
										//		if(PosiLine==-1)
										//		AlreadyAdded=0;
										
										if(PosiLine!=-1 ){//&& AlreadyAdded==0// if TopShortLinename exist in LineNameArray
											
											for(int AddedSlice=0; AddedSlice<duplineE; AddedSlice++){
												
												if(slicelabel+AddedSlice<=posislice2 && PosiSliceScan+AddedSlice<PositiveSlices){
													
													PositiveArrayName=LineNameArray[PosiSliceScan+AddedSlice];
													arrayPosition=0;
													for (String retval: PositiveArrayName.split(",")){
														args[arrayPosition]=retval;
														arrayPosition=arrayPosition+1;
													}
													String ScanlinenamePosi2 = args[0];//short linename
													FullLinenamePosi2 = args[2];//LineNameArray for adding slices
													
													PosiLine=(TopShortLinename.indexOf(ScanlinenamePosi2));//Top lineName and LineNameArray
													
													if(PosiLine!=-1){// same short-line name as topValue one, if different = single file.
														for(int dcStackScan=1; dcStackScan<=posislice2; dcStackScan++){
															
															int IsPosi0=(FullLinenamePosi2.indexOf(srlabels.get(dcStackScan-1)));
															
															if(IsPosi0!=-1){
																finallbs.add(FullLinenamePosi2);
																srlabels.set(dcStackScan-1, "Done");
																//		dcStackOrigi.deleteSlice(dcStackScan);
																
																//		AlreadyAdded=1;
																//	posislice2=posislice2-1;
																//	posislice2=dcStack.size();
																
																if(DUPlogonE==true)
																IJ.log("   "+FinalAdded+"  Added Slice; "+FullLinenamePosi2);
																
																FinalAdded=FinalAdded+1;
																LineNameArray[PosiSliceScan+AddedSlice]="Deleted";
																dcStackScan=posislice2+1;// finish scan for dcStack
																
															}//if(IsPosi0!=-1){
														}//	for(int dcStackScan=1; dcStackScan<posislice2; dcStackScan++){
														//		slicelabel=1;
													}//if(PosiLine!=-1){// same short-line name as topValue one, if different = single file.
												}
											}//for(int AddedSlice=1; AddedSlice<=duplineE; AddedSlice++){
											
											int OnceDeleted=0;
											/// delete rest of duplicated slices from LineNameArray /////////////////////
											for(int deleteDuplicatedSlice=0; deleteDuplicatedSlice<posislice; deleteDuplicatedSlice++){
												
												PositiveArrayName=LineNameArray[deleteDuplicatedSlice];
												arrayPosition=0;
												for (String retval: PositiveArrayName.split(",")){
													args[arrayPosition]=retval;
													arrayPosition=arrayPosition+1;
												}
												
												linenamePosi2 = args[0];//linename
												String DelFulllinename = args[2];//linename
												
												int PosiLineDUP=(TopShortLinename.indexOf(linenamePosi2));
												//			IJ.log(" linename; 	"+linename+"  linenamePosi2; "+linenamePosi2);
												
												if(PosiLineDUP!=-1){//if there is more duplicated lines in PositiveArrayName
													
													if(DUPlogonE==true)
													IJ.log(dupdel+" Duplicated & deleted; 	"+DelFulllinename);
													dupdel=dupdel+1;
													
													LineNameArray[deleteDuplicatedSlice]="Deleted";
													OnceDeleted=1;
												}else{
													//				if(OnceDeleted==1)
													//				deleteDuplicatedSlice=posislice;
												}
											}//for(int deleteDuplicatedSlice=0; deleteDuplicatedSlice<1000; deleteDuplicatedSlice++){
											
											PosiSliceScan=PositiveSlices;// end after 1 added
										}//	if(PosiLine!=-1){// if exist
									}//for(int PosiSliceScan=0; PosiSliceScan<PositiveSlices; PosiSliceScan++){
								}//if (labelmethodE==2){
								
								
								if (labelmethodE!=1){
									if(duplineE!=0){
										int NegativeExist=0;
										/// negative slice check //////////////////////////
										for(int negativeSlice=0; negativeSlice<PositiveSlices; negativeSlice++){
											
											int arrayPosition=0;
											for (String retval: LineNameArray[negativeSlice].split(",")){
												args[arrayPosition]=retval;
												arrayPosition=arrayPosition+1;
											}
											
											String linenameNega = args[2];//linename
											
											int NegaCheck=(weightposi[wposi].indexOf( linenameNega ));
											
											if(NegaCheck!=-1){
												NegativeExist=1;
												negativeSlice=PositiveSlices;
											}
										}//for(int negativeSlice=0; negativeSlice<PositiveSlices; negativeSlice++){
										
										if(NegativeExist==0){// if no-existing negative
											finallbs.add(weightposi[wposi]);
											srlabels.remove(slicelabel-1);
										}else{//NegativeExist==1
											srlabels.remove(slicelabel-1);
											
											if(DUPlogonE==true)
											IJ.log(dupdel+" Duplicated & deleted; 	"+weightposi[wposi]);
											dupdel=dupdel+1;
										}
									}else{//if(duplineE!=0){
										finallbs.add(weightposi[wposi]);
										srlabels.remove(slicelabel-1);
									}
									slicelabel=posislice2+1;
								}//	if (labelmethodE!=2){
								
								//		posislice2=posislice2-1;
							}//if(IsPosi!=-1){
						}//	for(int slicelabel=1; slicelabel<=posislice2; slicelabel++){
					}//	for(int wposi=0; wposi<posislice; wposi++){
				}else{//duplineE==0
					for(int wposi=0; wposi<posislice; wposi++){
						for(int slicelabel=1; slicelabel<=posislice2; slicelabel++){
							if(weightposi[wposi]==srlabels.get(slicelabel-1)){
								
								finallbs.add(weightposi[wposi]);
								srlabels.remove(slicelabel-1);
								
								if(logonE==true && logNanE==false)
								IJ.log("Positive linename; 	"+weightposi[wposi]);
								
								slicelabel=posislice2;
								posislice2=posislice2-1;
							}
						}
					}
				}//}else{//duplineE==0
				
				ImageStack dcStackfinal = new ImageStack (width,height);
				ImageStack OrigiStackfinal = new ImageStack (width,height);
				//	VirtualStack OrigiStackfinal = new VirtualStack (width,height);
				
				//		try {
				long size = width*height*3;
				int slnum = finallbs.size();
				if(slnum>400){
					slnum=400;
					if(maxnumber>400)
					slnum=maxnumber+200;
				}
				
				for (int s = 0; s < slnum; s++) {
					String label = finallbs.get(s);
					SearchResult sr = srdict.get(label);
					
					// percent conversion////////////////////
					int underindex1= label.indexOf("_");
					
					double smallscore= Double.parseDouble(label.substring(0,underindex1));
					String orilineName = label.substring(underindex1,label.length());
					
					
					int LineBeginIndex=(orilineName.indexOf("MB"));
					if(LineBeginIndex==-1)
					LineBeginIndex=(orilineName.indexOf("GMR"));
					if(LineBeginIndex==-1)
					LineBeginIndex=(orilineName.indexOf("VT"));
					if(LineBeginIndex==-1)
					LineBeginIndex=(orilineName.indexOf("_TR_"));
					if(LineBeginIndex==-1)
					LineBeginIndex=(orilineName.indexOf("R_"));
					if(LineBeginIndex==-1)
					LineBeginIndex=(orilineName.indexOf("GL_"));
					if(LineBeginIndex==-1)
					LineBeginIndex=(orilineName.indexOf("TDC"));
					if(LineBeginIndex==-1)
					LineBeginIndex=(orilineName.indexOf("JHS"));
					if(LineBeginIndex==-1)
					LineBeginIndex=(orilineName.indexOf("BJD"));
					if(LineBeginIndex==-1)
					LineBeginIndex=(orilineName.indexOf("UAH"));
					if(LineBeginIndex==-1)
					LineBeginIndex=(orilineName.indexOf("OL"));
					if(LineBeginIndex==-1)
					LineBeginIndex=(orilineName.indexOf("JRC_"));
					else if(LineBeginIndex==-1)
					LineBeginIndex=(orilineName.indexOf("SS"));
					
					int DotPosi=(orilineName.indexOf("."));
					int lengthST = orilineName.length();
					
					if(LineBeginIndex>(lengthST/2))
					LineBeginIndex=-1;
					
					//	if(LineBeginIndex==-1){
					//	int underindex = (orilineName.indexOf("_"));
					
					//	if(underindex<(lengthST/3))
					//	LineBeginIndex=(orilineName.indexOf("_"));
					//}
					
					if(LineBeginIndex>-1)
					orilineName="_"+orilineName.substring(LineBeginIndex,orilineName.length());
					
					double percentscore = round(smallscore, 1);
					
					String addzero="";
					if(percentscore<10)
					addzero="0";
					
					label = addzero+percentscore+orilineName;
					
					//	if(DUPlogonE==true)
					//	IJ.log("label; "+label);
					
					if (sr != null) {
						OrigiStackfinal.addSlice(label, sr.m_iporg);
						if (ShowCoE) dcStackfinal.addSlice(label, sr.m_ipcol);
					}
				}
				
				//		} catch(IOException e) {
				//			e.printStackTrace();
				//		}
				
				if(thremethodSTR=="Combine"){
					ImageStack combstack=new ImageStack(width*2, height, OrigiStackfinal.getColorModel());
					ImageProcessor ip6 = OrigiStackfinal.getProcessor(1);
					
					for (int i=1; i<=posislice; i++) {
						IJ.showProgress((double)i/posislice);
						ip5 = ip6.createProcessor(width*2, height);
						
						if(ShowCoE==true){
							ip5.insert(dcStackfinal.getProcessor(1),0,0);
							dcStackfinal.deleteSlice(1);
						}
						ip5.insert(OrigiStackfinal.getProcessor(1),width,0);
						OrigiStackfinal.deleteSlice(1);
						combstack.addSlice(weightposi[i-1], ip5);
					}
					
					if(ShowCoE==true){
						newimp = new ImagePlus("Co-localized_And_Original.tif_"+pixThresdub2+" %_"+titles[MaskE]+"", combstack);
						newimp.show();
					}
				}else{
					
					if(ShowCoE==true){
						newimp = new ImagePlus("Co-localized.tif_"+pixThresdub2+" %_"+titles[MaskE]+"", dcStackfinal);
						newimp.show();
					}
					
					newimpOri = new ImagePlus("Original_RGB.tif_"+pixThresdub2+" %_"+titles[MaskE]+"", OrigiStackfinal);
					
				}
				
			}//if(posislice>0){
			
			end = System.currentTimeMillis();
			IJ.log("time: "+((float)(end-start)/1000)+"sec");
			
			if(posislice==0)
			IJ.log("No positive slice");
			
			impEMstack.unlock();
			impLMsegstack.unlock();
		}//if(CDMrun==1)
		//	IJ.log("Done; "+increment+" mean; "+mean3+" Totalmaxvalue; "+totalmax+" desiremean; "+desiremean);
		
		if(CDMrun==0){
			newimpOri=impLMsegstack;
			posislice=newimpOri.getStackSize();
		}
		
		if(EMsearch==true && posislice>2){
			IJ.showStatus("EM MIP sorting");
			
			if(CDMrun==1){
				if(shownormal==true)
				newimpOri.show();
			}
			
		//	RoiManager rois = new RoiManager(true);
	//		rois.runCommand(impEMstack,"Select All");
			
			ImagePlus newimp2 = CDM_area_measure (newimpOri, impEMstack,gradientDIR_,GradientOnTheFly_,ThresmE,maxnumber,mirror_maskE,showFlip,threadNumE,FLpositive,stLMsegstack,slicenumber,impOriginalCDM);
			
			if(shownormal==false){
				newimpOri.unlock();
				newimpOri.close();
			}
			
			//IJ.runMacroFile(""+plugindir+"Macros/CDM_area_measure.ijm");
		}else if(EMsearch==true && posislice>0){
			newimpOri.show();
		}else
		IJ.showMessage("no positive hit");
		impEMstack.unlock();
		
		//	System.gc();
	} //public void run(ImageProcessor ip){
	
	
	
	public static int[] get_mskpos_array(ImageProcessor msk, int thresm){
		int sumpx = msk.getPixelCount();
		ArrayList<Integer> pos = new ArrayList<Integer>();
		int pix, red, green, blue;
		for(int n4=0; n4<sumpx; n4++){
			
			pix= msk.get(n4);//MaskE
			
			red = (pix>>>16) & 0xff;//mask
			green = (pix>>>8) & 0xff;//mask
			blue = pix & 0xff;//mask
			
			if(red>thresm || green>thresm || blue>thresm)
			pos.add(n4);
		}
		return pos.stream().mapToInt(i -> i).toArray();
	}
	
	public static int[] shift_mskpos_array(int[] src, int xshift, int yshift, int w, int h){
		ArrayList<Integer> pos = new ArrayList<Integer>();
		int x, y;
		int ypitch = w;
		for(int i = 0; i < src.length; i++) {
			int val = src[i];
			x = (val % ypitch) + xshift;
			y = val / ypitch + yshift;
			if (x >= 0 && x < w && y >= 0 && y < h)
			pos.add(y*w+x);
			else
			pos.add(-1);
		}
		return pos.stream().mapToInt(i -> i).toArray();
	}
	
	public static int[][] generate_shifted_masks(int[] in, int xyshiftE, int w, int h) {
		int[][] out = new int[1+(xyshiftE/2)*8][];
		
		out[0] = in.clone();
		int maskid = 1;
		for (int i = 2; i <= xyshiftE; i += 2) {
			for (int xx = -i; xx <= i; xx += i) {
				for (int yy = -i; yy <= i; yy += i) {
					if (xx == 0 && yy == 0) continue;
					out[maskid] = shift_mskpos_array(in, xx, yy, w, h);
					maskid++;
				}
			}
		}
		return out;
	}
	
	public static int[] mirror_maskE(int[] in, int ypitch) {
		int[] out = in.clone();
		int masksize = in.length;
		int x;
		for(int j = 0; j < masksize; j++) {
			int val = in[j];
			x = val % ypitch;
			out[j] = val + (ypitch-1) - 2*x;
		}
		return out;
	}
	
	public static double round(double value, int places) {
		if (places < 0) throw new IllegalArgumentException();
		
		long factor = (long) Math.pow(10, places);
		value = value * factor;
		long tmp = Math.round(value);
		return (double) tmp / factor;
	}
	
	public static int calc_score(ImageProcessor src, int[] srcmaskposi, ImageProcessor tar, int[] tarmaskposi, int th, double pixfludub, ImageProcessor coloc_out) {
		
		int masksize = srcmaskposi.length <= tarmaskposi.length ? srcmaskposi.length : tarmaskposi.length;
		int posi = 0;
		for(int masksig=0; masksig<masksize; masksig++){
			
			if (srcmaskposi[masksig] == -1 || tarmaskposi[masksig] == -1) continue;
			
			int pix1= src.get(srcmaskposi[masksig]);
			int red1 = (pix1>>>16) & 0xff;
			int green1 = (pix1>>>8) & 0xff;
			int blue1 = pix1 & 0xff;
			
			int pix2= tar.get(tarmaskposi[masksig]);
			int red2 = (pix2>>>16) & 0xff;
			int green2 = (pix2>>>8) & 0xff;
			int blue2 = pix2 & 0xff;
			
			if(red2>th || green2>th || blue2>th){
				
				double pxGap = calc_score_px(red1, green1, blue1, red2, green2, blue2); 
				
				if(pxGap<=pixfludub){
					if(coloc_out!=null)
					coloc_out.set(tarmaskposi[masksig], pix2);
					posi++;
				}
				
			}
		}
		
		return posi;
		
	}
	
	public static double calc_score_px(int red1, int green1, int blue1, int red2, int green2, int blue2) {
		int RG1=0; int BG1=0; int GR1=0; int GB1=0; int RB1=0; int BR1=0;
		int RG2=0; int BG2=0; int GR2=0; int GB2=0; int RB2=0; int BR2=0;
		double rb1=0; double rg1=0; double gb1=0; double gr1=0; double br1=0; double bg1=0;
		double rb2=0; double rg2=0; double gb2=0; double gr2=0; double br2=0; double bg2=0;
		double pxGap=10000; 
		double BrBg=0.354862745; double BgGb=0.996078431; double GbGr=0.505882353; double GrRg=0.996078431; double RgRb=0.505882353;
		double BrGap=0; double BgGap=0; double GbGap=0; double GrGap=0; double RgGap=0; double RbGap=0;
		
		if(blue1>red1 && blue1>green1){//1,2
			if(red1>green1){
				BR1=blue1+red1;//1
				if(blue1!=0 && red1!=0)
				br1= (double) red1 / (double) blue1;
			}else{
				BG1=blue1+green1;//2
				if(blue1!=0 && green1!=0)
				bg1= (double) green1 / (double) blue1;
			}
		}else if(green1>blue1 && green1>red1){//3,4
			if(blue1>red1){
				GB1=green1+blue1;//3
				if(green1!=0 && blue1!=0)
				gb1= (double) blue1 / (double) green1;
			}else{
				GR1=green1+red1;//4
				if(green1!=0 && red1!=0)
				gr1= (double) red1 / (double) green1;
			}
		}else if(red1>blue1 && red1>green1){//5,6
			if(green1>blue1){
				RG1=red1+green1;//5
				if(red1!=0 && green1!=0)
				rg1= (double) green1 / (double) red1;
			}else{
				RB1=red1+blue1;//6
				if(red1!=0 && blue1!=0)
				rb1= (double) blue1 / (double) red1;
			}
		}
		
		if(blue2>red2 && blue2>green2){
			if(red2>green2){//1, data
				BR2=blue2+red2;
				if(blue2!=0 && red2!=0)
				br2= (double) red2 / (double) blue2;
			}else{//2, data
				BG2=blue2+green2;
				if(blue2!=0 && green2!=0)
				bg2= (double) green2 / (double) blue2;
			}
		}else if(green2>blue2 && green2>red2){
			if(blue2>red2){//3, data
				GB2=green2+blue2;
				if(green2!=0 && blue2!=0)
				gb2= (double) blue2 / (double) green2;
			}else{//4, data
				GR2=green2+red2;
				if(green2!=0 && red2!=0)
				gr2= (double) red2 / (double) green2;
			}
		}else if(red2>blue2 && red2>green2){
			if(green2>blue2){//5, data
				RG2=red2+green2;
				if(red2!=0 && green2!=0)
				rg2= (double) green2 / (double) red2;
			}else{//6, data
				RB2=red2+blue2;
				if(red2!=0 && blue2!=0)
				rb2= (double) blue2 / (double) red2;
			}
		}
		
		///////////////////////////////////////////////////////					
		if(BR1>0){//1, mask// 2 color advance core
			if(BR2>0){//1, data
				if(br1>0 && br2>0){
					if(br1!=br2){
						pxGap=br2-br1;
						pxGap=Math.abs(pxGap);
					}else
					pxGap=0;
					
					if(br1==255 & br2==255)
					pxGap=1000;
				}
			}else if (BG2>0){//2, data
				if(br1<0.44 && bg2<0.54){
					BrGap=br1-BrBg;//BrBg=0.354862745;
					BgGap=bg2-BrBg;//BrBg=0.354862745;
					pxGap=BrGap+BgGap;
				}
			}
			//		IJ.log("pxGap; "+String.valueOf(pxGap)+"  BR1;"+String.valueOf(BR1)+", br1; "+String.valueOf(br1)+", BR2; "+String.valueOf(BR2)+", br2; "+String.valueOf(br2)+", BG2; "+String.valueOf(BG2)+", bg2; "+String.valueOf(bg2));
		}else if(BG1>0){//2, mask/////////////////////////////
			if(BG2>0){//2, data, 2,mask
				
				if(bg1>0 && bg2>0){
					if(bg1!=bg2){
						pxGap=bg2-bg1;
						pxGap=Math.abs(pxGap);
						
					}else if(bg1==bg2)
					pxGap=0;
					if(bg1==255 & bg2==255)
					pxGap=1000;
				}
				//	IJ.log(" pxGap BG2;"+String.valueOf(pxGap)+", bg1; "+String.valueOf(bg1)+", bg2; "+String.valueOf(bg2));
			}else if(GB2>0){//3, data, 2,mask
				if(bg1>0.8 && gb2>0.8){
					BgGap=BgGb-bg1;//BgGb=0.996078431;
					GbGap=BgGb-gb2;//BgGb=0.996078431;
					pxGap=BgGap+GbGap;
					//			IJ.log(" pxGap GB2;"+String.valueOf(pxGap));
				}
			}else if(BR2>0){//1, data, 2,mask
				if(bg1<0.54 && br2<0.44){
					BgGap=bg1-BrBg;//BrBg=0.354862745;
					BrGap=br2-BrBg;//BrBg=0.354862745;
					pxGap=BrGap+BgGap;
				}
			}
			//		IJ.log("pxGap; "+String.valueOf(pxGap)+"  BG1;"+String.valueOf(BG1)+"  BG2;"+String.valueOf(BG2)+", bg1; "+String.valueOf(bg1)+", bg2; "+String.valueOf(bg2)+", GB2; "+String.valueOf(GB2)+", gb2; "+String.valueOf(gb2)+", BR2; "+String.valueOf(BR2)+", br2; "+String.valueOf(br2));
		}else if(GB1>0){//3, mask/////////////////////////////
			if(GB2>0){//3, data, 3mask
				if(gb1>0 && gb2>0){
					if(gb1!=gb2){
						pxGap=gb2-gb1;
						pxGap=Math.abs(pxGap);
						
						//	IJ.log(" pxGap GB2;"+String.valueOf(pxGap));
					}else
					pxGap=0;
					if(gb1==255 & gb2==255)
					pxGap=1000;
				}
			}else if(BG2>0){//2, data, 3mask
				if(gb1>0.8 && bg2>0.8){
					BgGap=BgGb-gb1;//BgGb=0.996078431;
					GbGap=BgGb-bg2;//BgGb=0.996078431;
					pxGap=BgGap+GbGap;
				}
			}else if(GR2>0){//4, data, 3mask
				if(gb1<0.7 && gr2<0.7){
					GbGap=gb1-GbGr;//GbGr=0.505882353;
					GrGap=gr2-GbGr;//GbGr=0.505882353;
					pxGap=GbGap+GrGap;
				}
			}//2,3,4 data, 3mask
		}else if(GR1>0){//4mask/////////////////////////////
			if(GR2>0){//4, data, 4mask
				if(gr1>0 && gr2>0){
					if(gr1!=gr2){
						pxGap=gr2-gr1;
						pxGap=Math.abs(pxGap);
					}else
					pxGap=0;
					if(gr1==255 & gr2==255)
					pxGap=1000;
				}
			}else if(GB2>0){//3, data, 4mask
				if(gr1<0.7 && gb2<0.7){
					GrGap=gr1-GbGr;//GbGr=0.505882353;
					GbGap=gb2-GbGr;//GbGr=0.505882353;
					pxGap=GrGap+GbGap;
				}
			}else if(RG2>0){//5, data, 4mask
				if(gr1>0.8 && rg2>0.8){
					GrGap=GrRg-gr1;//GrRg=0.996078431;
					RgGap=GrRg-rg2;
					pxGap=GrGap+RgGap;
				}
			}//3,4,5 data
		}else if(RG1>0){//5, mask/////////////////////////////
			if(RG2>0){//5, data, 5mask
				if(rg1>0 && rg2>0){
					if(rg1!=rg2){
						pxGap=rg2-rg1;
						pxGap=Math.abs(pxGap);
					}else
					pxGap=0;
					if(rg1==255 & rg2==255)
					pxGap=1000;
				}
				
			}else if(GR2>0){//4 data, 5mask
				if(rg1>0.8 && gr2>0.8){
					GrGap=GrRg-gr2;//GrRg=0.996078431;
					RgGap=GrRg-rg1;//GrRg=0.996078431;
					pxGap=GrGap+RgGap;
					//	IJ.log(" pxGap GR2;"+String.valueOf(pxGap));
				}
			}else if(RB2>0){//6 data, 5mask
				if(rg1<0.7 && rb2<0.7){
					RgGap=rg1-RgRb;//RgRb=0.505882353;
					RbGap=rb2-RgRb;//RgRb=0.505882353;
					pxGap=RbGap+RgGap;
				}
			}//4,5,6 data
		}else if(RB1>0){//6, mask/////////////////////////////
			if(RB2>0){//6, data, 6mask
				if(rb1>0 && rb2>0){
					if(rb1!=rb2){
						pxGap=rb2-rb1;
						pxGap=Math.abs(pxGap);
					}else if(rb1==rb2)
					pxGap=0;
					if(rb1==255 & rb2==255)
					pxGap=1000;
				}
			}else if(RG2>0){//5, data, 6mask
				if(rg2<0.7 && rb1<0.7){
					RgGap=rg2-RgRb;//RgRb=0.505882353;
					RbGap=rb1-RgRb;//RgRb=0.505882353;
					pxGap=RgGap+RbGap;
					//	IJ.log(" pxGap RG;"+String.valueOf(pxGap));
				}
			}
		}//2 color advance core
		
		return pxGap;
	}
	
	public class ColorMIPMaskCompare {
		
		public class Output {
			int matchingPixNum;
			double matchingPct;
			public Output (int pixnum, double pct) {
				matchingPixNum = pixnum;
				matchingPct = pct;
			}
		}
		
		ImageProcessor m_query;
		ImageProcessor m_negquery;
		int[] m_mask;
		int[] m_negmask;
		int[][] m_tarmasklist;
		int[][] m_tarmasklist_mirror;
		int[][] m_tarnegmasklist;
		int[][] m_tarnegmasklist_mirror;
		int m_th;
		double m_pixfludub;
		
		boolean m_mirror;
		boolean m_mirrorneg;
		int m_xyshift;
		
		int m_width;
		int m_height;
		
		int m_maskpos_st;
		int m_maskpos_ed;
		
		
		
		//Basic Search
		ColorMIPMaskCompare (ImageProcessor query, int mask_th, int search_th, double toleranceZ) {
			m_query = query;
			m_width = m_query.getWidth();
			m_height = m_query.getHeight();
			
			m_mask = get_mskpos_array(m_query, mask_th);
			m_negmask = null;
			m_th = search_th;
			m_pixfludub = toleranceZ;
			m_mirror = false;
			m_mirrorneg = false;
			m_xyshift = 0;
			
			m_tarmasklist = new int[1][];
			m_tarmasklist_mirror = null;
			m_tarnegmasklist = null;
			m_tarnegmasklist_mirror = null;
			
			m_tarmasklist[0] = m_mask;
			
			m_maskpos_st = m_mask[0];
			m_maskpos_ed = m_mask[m_mask.length-1];
			
		}
		
		
		//Advanced Search
		ColorMIPMaskCompare (ImageProcessor query, int mask_th, boolean mirror_maskE, ImageProcessor negquery, int negmask_th, boolean mirror_negmaskE, int search_th, double toleranceZ, int xyshiftE) {
			m_query = query;
			m_negquery = negquery;
			m_width = m_query.getWidth();
			m_height = m_query.getHeight();
			
			m_mask = get_mskpos_array(m_query, mask_th);
			if (m_negquery != null) m_negmask = get_mskpos_array(m_negquery, negmask_th);
			m_th = search_th;
			m_pixfludub = toleranceZ;
			m_mirror = mirror_maskE;
			m_mirrorneg = mirror_negmaskE;
			m_xyshift = xyshiftE;
			
			//shifting
			m_tarmasklist = generate_shifted_masks(m_mask, m_xyshift, m_width, m_height);
			if (m_negquery != null) m_tarnegmasklist = generate_shifted_masks(m_negmask, m_xyshift, m_width, m_height);
			else m_tarnegmasklist = null;
			
			//mirroring
			if (m_mirror) {
				m_tarmasklist_mirror = new int[1+(xyshiftE/2)*8][];
				for (int i = 0; i < m_tarmasklist.length; i++)
				m_tarmasklist_mirror[i] = mirror_maskE(m_tarmasklist[i], m_width);
			} else {
				m_tarmasklist_mirror = null;
			}
			if (m_mirrorneg && m_negquery != null) {
				m_tarnegmasklist_mirror = new int[1+(xyshiftE/2)*8][];
				for (int i = 0; i < m_tarnegmasklist.length; i++)
				m_tarnegmasklist_mirror[i] = mirror_maskE(m_tarnegmasklist[i], m_width);
			} else {
				m_tarnegmasklist_mirror = null;
			}
			
			m_maskpos_st = m_width*m_height;
			m_maskpos_ed = 0;
			for (int i = 0; i < m_tarmasklist.length; i++) {
				if (m_tarmasklist[i][0] < m_maskpos_st) m_maskpos_st = m_tarmasklist[i][0];
				if (m_tarmasklist[i][m_tarmasklist[i].length-1] > m_maskpos_ed) m_maskpos_ed = m_tarmasklist[i][m_tarmasklist[i].length-1];
			}
			if (m_mirror) {
				for (int i = 0; i < m_tarmasklist_mirror.length; i++) {
					if (m_tarmasklist_mirror[i][0] < m_maskpos_st) m_maskpos_st = m_tarmasklist_mirror[i][0];
					if (m_tarmasklist_mirror[i][m_tarmasklist_mirror[i].length-1] > m_maskpos_ed) m_maskpos_ed = m_tarmasklist_mirror[i][m_tarmasklist_mirror[i].length-1];
				}
			}
			if (m_negquery != null) {
				for (int i = 0; i < m_tarnegmasklist.length; i++) {
					if (m_tarnegmasklist[i][0] < m_maskpos_st) m_maskpos_st = m_tarnegmasklist[i][0];
					if (m_tarnegmasklist[i][m_tarnegmasklist[i].length-1] > m_maskpos_ed) m_maskpos_ed = m_tarnegmasklist[i][m_tarnegmasklist[i].length-1];
				}
				if (m_mirrorneg) {
					for (int i = 0; i < m_tarnegmasklist_mirror.length; i++) {
						if (m_tarnegmasklist_mirror[i][0] < m_maskpos_st) m_maskpos_st = m_tarnegmasklist_mirror[i][0];
						if (m_tarnegmasklist_mirror[i][m_tarnegmasklist_mirror[i].length-1] > m_maskpos_ed) m_maskpos_ed = m_tarnegmasklist_mirror[i][m_tarnegmasklist_mirror[i].length-1];
					}
				}
			}
			
		}
		
		public int getMaskSize() {
			return m_mask.length;
		}
		
		public int getNegMaskSize() {
			return m_negmask != null ? m_negmask.length : 0;
		}
		
		public int getMaskStartPos() {
			return m_maskpos_st;
		}
		
		public int getMaskEndPos() {
			return m_maskpos_ed;
		}
		
		public void setThreshold(int th) {
			m_th = th;
		}
		
		public void setToleranceZ(double tolerance) {
			m_pixfludub = tolerance;
		}
		
		public Output runSearch(byte[] tarimg_in, byte[] coloc_out) {
			int posi = 0;
			double posipersent = 0.0;
			int masksize = m_mask.length;
			int negmasksize = m_negquery != null ? m_negmask.length : 0;
			
			for (int mid = 0; mid < m_tarmasklist.length; mid++) {
				int tmpposi = calc_scoreb(m_query, m_mask, tarimg_in, m_tarmasklist[mid], m_th, m_pixfludub, coloc_out);
				if (tmpposi > posi) {
					posi = tmpposi;
					posipersent= (double) posi/ (double) masksize;
				}
			}
			if (m_tarnegmasklist != null) {
				int nega = 0;
				double negapersent = 0.0;
				for (int mid = 0; mid < m_tarnegmasklist.length; mid++) {
					int tmpnega = calc_scoreb(m_negquery, m_negmask, tarimg_in, m_tarnegmasklist[mid], m_th, m_pixfludub, null);
					if (tmpnega > nega) {
						nega = tmpnega;
						negapersent = (double) nega/ (double) negmasksize;
					}
				}
				posipersent -= negapersent;
				posi = (int)Math.round((double)posi - (double)nega*((double)masksize/(double)negmasksize));
			}
			
			if (m_tarmasklist_mirror != null) {
				int mirror_posi = 0;
				double mirror_posipersent = 0.0;
				for (int mid = 0; mid < m_tarmasklist_mirror.length; mid++) {
					int tmpposi = calc_scoreb(m_query, m_mask, tarimg_in, m_tarmasklist_mirror[mid], m_th, m_pixfludub, coloc_out);
					if (tmpposi > mirror_posi) {
						mirror_posi = tmpposi;
						mirror_posipersent= (double) mirror_posi/ (double) masksize;
					}
				}
				if (m_tarnegmasklist_mirror != null) {
					int nega = 0;
					double negapersent = 0.0;
					for (int mid = 0; mid < m_tarnegmasklist_mirror.length; mid++) {
						int tmpnega = calc_scoreb(m_negquery, m_negmask, tarimg_in, m_tarnegmasklist_mirror[mid], m_th, m_pixfludub, null);
						if (tmpnega > nega) {
							nega = tmpnega;
							negapersent = (double) nega/ (double) negmasksize;
						}
					}
					mirror_posipersent -= negapersent;
					mirror_posi = (int)Math.round((double)mirror_posi - (double)nega*((double)masksize/(double)negmasksize));
				}
				if (posipersent < mirror_posipersent) {
					posi = mirror_posi;
					posipersent = mirror_posipersent;
				}
			}
			
			return new Output(posi, posipersent);
		}
		
		public Output runSearch(ImageProcessor tarimg_in, ImageProcessor coloc_out) {
			int posi = 0;
			double posipersent = 0.0;
			int masksize = m_mask.length;
			int negmasksize = m_negquery != null ? m_negmask.length : 0;
			
			for (int mid = 0; mid < m_tarmasklist.length; mid++) {
				int tmpposi = calc_score(m_query, m_mask, tarimg_in, m_tarmasklist[mid], m_th, m_pixfludub, coloc_out);
				if (tmpposi > posi) {
					posi = tmpposi;
					posipersent= (double) posi/ (double) masksize;
				}
			}
			if (m_tarnegmasklist != null) {
				int nega = 0;
				double negapersent = 0.0;
				for (int mid = 0; mid < m_tarnegmasklist.length; mid++) {
					int tmpnega = calc_score(m_negquery, m_negmask, tarimg_in, m_tarnegmasklist[mid], m_th, m_pixfludub, null);
					if (tmpnega > nega) {
						nega = tmpnega;
						negapersent = (double) nega/ (double) negmasksize;
					}
				}
				posipersent -= negapersent;
				posi = (int)Math.round((double)posi - (double)nega*((double)masksize/(double)negmasksize));
			}
			
			if (m_tarmasklist_mirror != null) {
				int mirror_posi = 0;
				double mirror_posipersent = 0.0;
				for (int mid = 0; mid < m_tarmasklist_mirror.length; mid++) {
					int tmpposi = calc_score(m_query, m_mask, tarimg_in, m_tarmasklist_mirror[mid], m_th, m_pixfludub, coloc_out);
					if (tmpposi > mirror_posi) {
						mirror_posi = tmpposi;
						mirror_posipersent= (double) mirror_posi/ (double) masksize;
					}
				}
				if (m_tarnegmasklist_mirror != null) {
					int nega = 0;
					double negapersent = 0.0;
					for (int mid = 0; mid < m_tarnegmasklist_mirror.length; mid++) {
						int tmpnega = calc_score(m_negquery, m_negmask, tarimg_in, m_tarnegmasklist_mirror[mid], m_th, m_pixfludub, null);
						if (tmpnega > nega) {
							nega = tmpnega;
							negapersent = (double) nega/ (double) negmasksize;
						}
					}
					mirror_posipersent -= negapersent;
					mirror_posi = (int)Math.round((double)mirror_posi - (double)nega*((double)masksize/(double)negmasksize));
				}
				if (posipersent < mirror_posipersent) {
					posi = mirror_posi;
					posipersent = mirror_posipersent;
				}
			}
			
			return new Output(posi, posipersent);
		}
		
		public int calc_scoreb(ImageProcessor src, int[] srcmaskposi, byte[] tar, int[] tarmaskposi, int th, double pixfludub, byte[] coloc_out) {
			
			int masksize = srcmaskposi.length <= tarmaskposi.length ? srcmaskposi.length : tarmaskposi.length;
			int posi = 0;
			for(int masksig=0; masksig<masksize; masksig++){
				
				if (srcmaskposi[masksig] == -1 || tarmaskposi[masksig] == -1) continue;
				
				int pix1= src.get(srcmaskposi[masksig]);
				int red1 = (pix1>>>16) & 0xff;
				int green1 = (pix1>>>8) & 0xff;
				int blue1 = pix1 & 0xff;
				
				int p = tarmaskposi[masksig]*3;
				int red2 = tar[p] & 0xff;
				int green2 = tar[p+1] & 0xff;
				int blue2 = tar[p+2] & 0xff;
				
				if(red2>th || green2>th || blue2>th){
					
					double pxGap = calc_score_px(red1, green1, blue1, red2, green2, blue2); 
					
					if(pxGap<=pixfludub){
						if(coloc_out!=null) {
							coloc_out[p] = tar[p];
							coloc_out[p+1] = tar[p+1];
							coloc_out[p+2] = tar[p+2];
						}
						posi++;
					}
					
				}
			}
			
			return posi;
			
		}
		
	}//public class ColorMIPMaskCompare {
	
	ImagePlus CDM_area_measure (ImagePlus impCDMresult, final ImagePlus impEMstack2, final String gradientDIR, final boolean rungradientonthefly, final int ThresmEf, final int maxnumberF,final boolean mirror_maskEF, String showFlipF, final int threadNumEF, int FLpositiveF, ImageStack st3F, int slicenumberF, final ImagePlus impOriginalCDMF){
		
		int Threval=0; int stackslicenum=0;
		
		int wList [] = WindowManager.getIDList();
		if (wList==null || wList.length<2) {
			IJ.showMessage("There should be at least two windows open");
		}
		
		int [] info= impCDMresult.getDimensions();
		stackslicenum = info[3];//52
		int WW = info[0];
		int HH = info[1];
		
		int [] infoEM= impEMstack2.getDimensions();
		int stackslicenumEM = infoEM[3];//52
		
		ImageStack EMstack = new ImageStack (WW,HH);
		
		
		
		IJ.log("stackslicenumEM; "+String.valueOf(stackslicenumEM)+"  stackslicenum; "+stackslicenum+"  threadNumEF; "+threadNumEF);
		
		final int test=1;
		
		if(stackslicenumEM==1){
			ImageProcessor EMsliceSingle = impEMstack2.getProcessor();
			String EMname = impEMstack2.getTitle();
			for(int addEMslice=1; addEMslice<=stackslicenum; addEMslice++){
				EMstack.addSlice(EMname,EMsliceSingle);
			}
			
		}else
		EMstack = impEMstack2.getStack();
		
		ImageStack CDMresultstackFinal=impCDMresult.getStack();// result stack = segmented one
		ImageStack OriCDMstack=impOriginalCDMF.getStack();
		
		
		
		for(int ii=1; ii<=stackslicenum; ii++){// delete color bar
			ImageProcessor ipLM = CDMresultstackFinal.getProcessor(ii);
			ImageProcessor ipEM = EMstack.getProcessor(ii);
			
			for(int ix=WW-270; ix<WW; ix++){// deleting color scale from mask
				for(int iy=0; iy<90; iy++){
					ipLM.set(ix,iy,-16777216);
					ipEM.set(ix,iy,-16777216);
				}
			}
			
			if(WW<HH){// VNC
				for(int ix=0; ix<291; ix++){// deleting color scale from mask
					for(int iy=0; iy<90; iy++){
						ipLM.set(ix,iy,-16777216);
						ipEM.set(ix,iy,-16777216);
					}
				}
			}
		}//		for(int ii=0; ii<stackslicenum; ii++){
		
		ImagePlus imp10pxRGB_LM = impCDMresult.duplicate();
		
		
		imp10pxRGB_LM=MaxF(imp10pxRGB_LM,Integer.parseInt(negativeradiusEM),1);
		//IJ.run(imp10pxRGB_LM,"Maximum...", "radius="+negativeradiusEM+" stack");
		
		ImageStack Stack10pxRGB_LM = imp10pxRGB_LM.getStack();
		
		ImagePlus impEMgradient = impEMstack2.duplicate();
		//IJ.run(impEMgradient,"Gradient conv active img", "cpu="+threadNumEF+" maxsize=5");
		
		impEMgradient=GradientConv(impEMgradient,5);
		
		//impEMgradient.show();
		//	impEMgradient.updateAndRepaintWindow();
		//impEMgradient.hide();
		ImageStack StackEMGradient = impEMgradient.getStack();
		
		
		int [] infogra= impEMgradient.getDimensions();
		int stackslicenumgra = infogra[3];//52
		IJ.log("stackslicenumgra; "+stackslicenumgra);
		
		ImageProcessor ipgramask = impEMgradient.getProcessor();
		//	impEMgradient.show();
		
		if(stackslicenumgra==1){
			for(int iadd=1; iadd<=stackslicenum; iadd++){
				//		IJ.log("added_gra; "+iadd);
				StackEMGradient.addSlice("single_mask", ipgramask);
			}
		}
		
		
		final ImageStack StackEMGradientF=StackEMGradient;
		
		ImagePlus imp10pxRGBEM = impEMstack2.duplicate();
		
		imp10pxRGB_LM=MaxF(imp10pxRGBEM,Integer.parseInt(negativeradiusEM),1);
		//IJ.run(imp10pxRGBEM,"Maximum...", "radius="+negativeradiusEM+" stack");
			imp10pxRGBEM.show();
		ImageStack stackEM10pxRGB = imp10pxRGBEM.getStack();
		
		int [] info10pxRGB= imp10pxRGBEM.getDimensions();
		stackslicenumgra = info10pxRGB[3];//52
		
		ImageProcessor ip10pxRGB = imp10pxRGBEM.getProcessor();
		
		if(stackslicenumgra==1){
			for(int iiadd=1; iiadd<=stackslicenum; iiadd++){
				stackEM10pxRGB.addSlice("single_RGB", ip10pxRGB);
			}
		}
		
		long startT=System.currentTimeMillis();
		
		final int sumpx= WW*HH;
		
		
		
		
		//delete name
		for(int iz=1; iz<=stackslicenum; iz++){
			ImageProcessor fillip=CDMresultstackFinal.getProcessor(iz);
			
			for(int ix=0; ix<330; ix++){
				for(int iy=0; iy<100; iy++){
					
					int pixf=fillip.getPixel(ix,iy);
					
					int red1 = (pixf>>>16) & 0xff;
					int green1 = (pixf>>>8) & 0xff;
					int blue1 = pixf & 0xff;
					
					if(red1==green1 && green1==blue1)
					fillip.set(ix,iy,-16777216);
				}
			}
			
			for(int ix=WW-270; ix<WW; ix++){
				for(int iy=0; iy<85; iy++){
					
					fillip.set(ix,iy,-16777216);
				}
			}
		}
		
		long [] areaarray= new long[stackslicenum];
		// original area measurement////////////////////
		
		int [] info2= impCDMresult.getDimensions();
		
		final int WW2 = info2[0];
		final int HH2 = info2[1];
		
		int PositiveStackSlicePre=stackslicenum;
		
		if(PositiveStackSlicePre>maxnumberF+FLpositiveF+50)
		PositiveStackSlicePre=maxnumberF+FLpositiveF+50;
		
		final int PositiveStackSlice=PositiveStackSlicePre;
		
		IJ.log("2317 PositiveStackSlice; "+String.valueOf(PositiveStackSlice));
		String [] namearray=new String [PositiveStackSlice];
		String [] totalnamearray=new String[PositiveStackSlice];
		double[] scorearray = new double[PositiveStackSlice];
		
		final String [] gaparray=new String[PositiveStackSlice];
		
		double maxScore=0;
		long maxAreagap=0;
		
		/// name array creation ///////////////////
		for(int iname=1; iname<=PositiveStackSlice; iname++){
			
			if(stackslicenumEM==1 )
			namearray[iname-1] = CDMresultstackFinal.getSliceLabel(iname);
			else
			namearray[iname-1] = EMstack.getSliceLabel(iname);
			
			int spaceIndex=namearray[iname-1].indexOf(" ");
			if(spaceIndex!=-1){// replace slice label
				namearray[iname-1]=namearray[iname-1].replace(" ", "_");
				
				if(stackslicenumEM==1 )
				CDMresultstackFinal.setSliceLabel(namearray[iname-1],iname);
				else
				EMstack.setSliceLabel(namearray[iname-1],iname);
			}
			
			//		IJ.log(String.valueOf(iname)+"  "+namearray[iname-1]);
			
			int undeIndex=namearray[iname-1].indexOf("_");
			
			scorearray[iname-1]=Double.parseDouble(namearray[iname-1].substring(0,undeIndex));
			
			if(maxScore<scorearray[iname-1])
			maxScore=scorearray[iname-1];
			
		}//for(int iname=0; iname<PositiveStackSlice; iname++){
		
		IJ.log("2349");
		
		ImagePlus Stack2IMP =impCDMresult.duplicate();
		Stack2IMP=GradientConv(Stack2IMP,10);
		
		//Stack2IMP.show();
		ImageStack originalGradientStack = Stack2IMP.getStack();
		
		IJ.log("stackslicenum; "+stackslicenum+"  PositiveStackSlice; "+PositiveStackSlice);
		long startTRGB =System.currentTimeMillis();
		
		final ImageStack EMstackF = EMstack;
		
		final Thread[] threads = newThreadArray();
		final AtomicInteger ai2 = new AtomicInteger(1);
		
		for (int ithread = 0; ithread < threads.length; ithread++) {
			// Concurrently run in as many threads as CPUs
			threads[ithread] = new Thread() {
				
				{ setPriority(Thread.NORM_PRIORITY); }
				
				public void run() {
					
					for(int isli=ai2.getAndIncrement(); isli<PositiveStackSlice+1; isli = ai2.getAndIncrement()){
						
						//			IJ.log("isli; "+isli);
						//	for(int isli=1; isli<PositiveStackSlice+1; isli++){
						ImageProcessor ipresult = CDMresultstackFinal.getProcessor(isli);
						ImagePlus originalValue1imp = new ImagePlus ("SLICE"+isli+".tif",ipresult);
						
						//multipy image creation/////////////
						ImageConverter ic2 = new ImageConverter(originalValue1imp);
						ic2.convertToGray16();
						
						originalValue1imp = setsignal1(originalValue1imp,sumpx);
						
						//if(test==1 && isli==2){
						//	originalValue1imp.show();
						//				return;
						//			}
						
						ImageProcessor originalValue1IP = originalValue1imp.getProcessor();
						ImageProcessor ipEMgradient = StackEMGradientF.getProcessor(isli);
						
						for(int ivx=0; ivx<sumpx; ivx++){
							
							int pix1 = originalValue1IP.get(ivx);
							int pix2 = ipEMgradient.get(ivx);
							
							if(pix2>0)
							originalValue1IP.set(ivx, pix1*pix2);
							else
							originalValue1IP.set(ivx, 0);
							
						}// multiply 1 value slice and gradient mask
						
						//	if(test==1 && isli==1){
						//		originalValue1imp.show();
						//		return;
						//	}
						
						ImagePlus impOriStackResult = new ImagePlus ("impOriStackResult.tif",CDMresultstackFinal.getProcessor(isli));// original stack slice
						
						//		if(test==1 ){
						//							impOriStackResult.show();
						//			return;
						//		}
						
						ImagePlus	imp10pxEMRGBsingleSlice = new ImagePlus ("imp10pxEMRGBsingleSlice", stackEM10pxRGB.getProcessor(isli));
						int Zmatching=0;
						
						if(Zmatching==1)
						originalValue1imp = deleteMatchZandCreateZnegativeScoreIMG (originalValue1imp,impOriStackResult,imp10pxEMRGBsingleSlice,sumpx);
						
						//	if(test==1){
						//		originalValue1imp.updateAndRepaintWindow();
						//		originalValue1imp.show();// 
						//		return; 
						//	}
						
						long EMtoSampleNegativeScore=sumPXmeasure(originalValue1imp);
						
						originalValue1imp.unlock();
						originalValue1imp.close();
						
						imp10pxEMRGBsingleSlice.unlock();
						imp10pxEMRGBsingleSlice.close();
						
						impOriStackResult.unlock();
						impOriStackResult.close();
						
						/// just for initialization of impOrigiGradientSlice /////////////////////////
						// OriginalCDM gradient X EM 1 value MIP
						ImageProcessor ipOrigiGradientSlice = originalGradientStack.getProcessor(isli);// already gradient stack
						String titleslice = originalGradientStack.getSliceLabel(isli);
						
						ImagePlus impOrigiGradientSlice = new ImagePlus (titleslice,ipOrigiGradientSlice);
						
						ImageProcessor ipLM10pxRGBslice = Stack10pxRGB_LM.getProcessor(isli);
						String CDMtitleslice = CDMresultstackFinal.getSliceLabel(isli);
						
						ImagePlus impRGBOrigiCDMslice = new ImagePlus (CDMtitleslice,ipLM10pxRGBslice);
						
						//	if(test==1){
						//		impRGBOrigiCDMslice.show();// 
						//					return; 
						//	}
						
						// to make EM value 1
						
						ImageProcessor ipEMslice = EMstackF.getProcessor(isli);
						String EMsliceTitle = EMstackF.getSliceLabel(isli);
						
						ImagePlus impEMsliceValue1 = new ImagePlus ("Slice"+isli+" "+EMsliceTitle,ipEMslice);
						
						ImageProcessor ipEMslice2 = impEMsliceValue1.getProcessor();
						
						//	IJ.run(impEMsliceValue1,"Maximum...", "radius="+negativeradiusEM+"");
						
						for(int ipix=0; ipix<sumpx; ipix++){// 255 binary mask creation
							int RGBpix=ipEMslice2.get(ipix);
							
							int redval = (RGBpix>>>16) & 0xff;
							int greenval = (RGBpix>>>8) & 0xff;
							int blueval = RGBpix & 0xff;
							
							if(redval<=ThresmEf && greenval<=ThresmEf && blueval<=ThresmEf )
							ipEMslice2.set(ipix, -16777216);
						}
						
						ImageConverter ic = new ImageConverter(impEMsliceValue1);
						ic.convertToGray16();
						
						impEMsliceValue1 = setsignal1(impEMsliceValue1,sumpx);
						ImageProcessor ipEMvalue1 = impEMsliceValue1.getProcessor();
						
						//	if(test==1 && isli==62){
						//			impEMsliceValue1.show();// 
						//			return; 
						//		}
						
						for(int ivx2=0; ivx2<sumpx; ivx2++){//multiply images
							int pix1 = ipOrigiGradientSlice.get(ivx2);
							int pix2 = ipEMvalue1.get(ivx2);
							
							
							//		if(pix1>0)
							ipEMvalue1.set(ivx2, pix1*pix2);
							//		else
							//		ipEMvalue1.set(ivx2, 0);
							
						}// multiply slice and gradient mask
						
						//		if(test==1  && isli==62){
						//			impEMsliceValue1.show();// 
						//			impOrigiGradientSlice.show();
						//			return; 
						//		}
						
						ipEMslice = EMstackF.getProcessor(isli);
						
						ImagePlus impEMslice = new ImagePlus (EMsliceTitle,ipEMslice);
						
						if(Zmatching==1)
						impEMsliceValue1 = deleteMatchZandCreateZnegativeScoreIMG (impEMsliceValue1,impEMslice,impRGBOrigiCDMslice,sumpx);
						
						
						//	if(test==1){
						
						//		impEMsliceValue1.updateAndRepaintWindow();
						//		impEMsliceValue1.show();// 
						//		return; 
						//	}
						
						long SampleToMask=sumPXmeasure(impEMsliceValue1);
						
						//		IJ.log("SampleToMask; "+SampleToMask);
						
						//				if(test==1 && isli==1){
						//		impOrigiGradientSlice.show();
						//		impEMsliceValue1.show();
						//			return;
						//	}
						
						if(IJ.escapePressed()){
							IJ.log("esc canceled");
							return;
						}
						
						impOrigiGradientSlice.close();
						long normalval=(SampleToMask+EMtoSampleNegativeScore)/2;// /2
						long realval=normalval;
						
						
						//		IJ.log(String.valueOf(isli)+"SampleToMask; "+SampleToMask+"  EMtoSampleNegativeScore; "+EMtoSampleNegativeScore+"  normalval; "+String.valueOf(normalval));
						
						impRGBOrigiCDMslice.close();
						impEMsliceValue1.close();
						
						areaarray[isli-1]=realval;
						
					}//2385 for(int isli=1; isli<=slices; isli++){
			}};
		}//	for (int ithread = 0; ithread < threads.length; ithread++) {
		startAndJoin(threads);
		
		
		for(int iscorescan=0; iscorescan<PositiveStackSlice; iscorescan++){
			if(maxAreagap<areaarray[iscorescan])
			maxAreagap=areaarray[iscorescan];
		}
		
		long endTRGB =System.currentTimeMillis();
		long gapT2=endTRGB-startTRGB;
		
		IJ.log(gapT2/1000+" sec for 2D distance score & RGB 3D score generation");
		
		impEMgradient.unlock();
		//	impEMgradient.hide();
		impEMgradient.close();
		
		imp10pxRGBEM.unlock();
		imp10pxRGBEM.close();
		
		imp10pxRGB_LM.unlock();
		imp10pxRGB_LM.close();
		
		impCDMresult.unlock();
		
		//	IJ.log("Slice length; "+PositiveStackSlice);
		double [] normScorePercent=new double[PositiveStackSlice];
		/// normalize score ////////////////////
		for(int inorm=0; inorm<PositiveStackSlice; inorm++){
			
			double OrinormAreaPercent=(double)areaarray[inorm]/(double)maxAreagap;
			normScorePercent[inorm]=scorearray[inorm]/maxScore;
			
			double normAreaPercent=OrinormAreaPercent;
			if(normAreaPercent>1)
			normAreaPercent=1;
			
			if(normAreaPercent<0.002)
			normAreaPercent=0.002;
			
			double doubleGap=(normScorePercent[inorm]/normAreaPercent)*100;
			
			
			
			//	IJ.log(inorm+1+"   normAreaPercent; "+normAreaPercent+"  normScorePercent[inorm]; "+normScorePercent[inorm]+"  doubleGap; "+doubleGap+"  OrinormAreaPercent;"+OrinormAreaPercent);
			
			String addST="";
			if(doubleGap<10000 && doubleGap>999.999999)
			addST=("0");
			else if(doubleGap<1000 && doubleGap>99.999999)
			addST=("00");
			else if(doubleGap<100 && doubleGap>9.999999)
			addST=("000");
			else if(doubleGap<10)
			addST=("0000");
			
			String finalpercent=String.format("%.10f",(normScorePercent[inorm]/normAreaPercent)*100);
			
			if(finalpercent.equals("100.0000000000")){
				finalpercent="50000.0000";
				addST="";
			}
			
			//	IJ.log(inorm+1+"  finalpercent; "+finalpercent+"   normAreaPercent; "+normAreaPercent+"  normScorePercent[inorm]; "+normScorePercent[inorm]+"  doubleGap; "+doubleGap);
			
			
			
			gaparray[inorm]=addST.concat(finalpercent);//,10
			
			String S1=gaparray[inorm].concat(" ");
			
			totalnamearray[inorm]=	S1.concat(namearray[inorm]);
			
			
			//		IJ.log(String.valueOf(inorm)+"  "+gaparray[inorm]);
		}
		
		//Array.show(totalnamearray);
		
		//Arrays.sort(gaparray);
		Arrays.sort(gaparray, Collections.reverseOrder());
		//Array.show(gaparray);
		
		int Finslice=PositiveStackSlice;
		//	if(Finslice>maxnumberF)
		//	Finslice=maxnumberF;
		int [] fixedFL = new int [totalnamearray.length+1];
		IJ.log("Finslice; "+Finslice+"   totalnamearray.length; "+totalnamearray.length);
		
		ImageStack Stackfinal = new ImageStack (WW,HH);
		
		for(int ifill=0; ifill<fixedFL.length; ifill++ ){
			fixedFL[ifill]=-1;	
			
			//	IJ.log(ifill+"totalnamearray[ifill]; "+totalnamearray[ifill]);
		}
		int addedslice=0;
		
		for(int inew=0; inew<Finslice; inew++){// score and sorting
			
			if(addedslice>=maxnumberF)
			break;
			
			int FLindex=-1;
			double Totalscore = Double.parseDouble(gaparray[inew]);
			String slicename="";
			
			String oribodyID="";
			
			
			for(int iscan=0; iscan<totalnamearray.length; iscan++){
				String [] arg2=totalnamearray[iscan].split(" ");
				
				//		IJ.log(iscan+" arg2[0]; "+arg2[0]+"   totalnamearray[iscan]; "+totalnamearray[iscan]);
				double arg2_0=Double.parseDouble(arg2[0]);
				
				//if(test==1){
				//		return;
				//	}
				oribodyID=arg2[1];
				//			IJ.log("2977 DUPslicename; "+DUPslicename+"   FLindex; "+FLindex);
				
				FLindex=oribodyID.lastIndexOf("_FL");
				if(FLindex!=-1){
					
					int underindex = oribodyID.indexOf("_");	
					String oribodyIDtrue = oribodyID.substring(underindex+1,FLindex)+".tif";// body ID of FL slice
					
					
					for(int ibodyID=0; ibodyID<totalnamearray.length; ibodyID++){
						String [] argsearch = totalnamearray[ibodyID].split(" ");
						
						double targetScore = Double.parseDouble(argsearch[0]);
						String targetName = argsearch[1];
						if(!targetName.equals("NN")){
							
							int FLindex2=targetName.lastIndexOf("_FL");
							if(FLindex2==-1){
								
								int underindextarget = targetName.indexOf("_");
								String targetName2 = targetName.substring(underindextarget+1,targetName.length());// body ID of FL slice
								
								if(targetName2.equals(oribodyIDtrue)){
									
									////			if(oribodyIDtrue.equals("517514142_RT_18U.tif"))
									//		IJ.log(iscan+" targetName2; "+targetName2+"  oribodyIDtrue; "+oribodyIDtrue+"  ibodyID; "+ibodyID+"  arg2_0; "+arg2_0+"  targetScore; "+targetScore);
									
									//				IJ.log("targetName2; "+targetName2+"  oribodyIDtrue; "+oribodyIDtrue);
									
									totalnamearray[iscan] = "0 NN";
									
									if(arg2_0>targetScore){
										
										ImageProcessor hitslice = CDMresultstackFinal.getProcessor(ibodyID+1);//original search MIP stack
										
										
										hitslice.setFont(new Font("SansSerif", Font.PLAIN, 26));
										
										//			IJ.log(ibodyID+" targetName2; "+targetName2+"  oribodyIDtrue; "+oribodyIDtrue+"  arg2_0; "+arg2_0+"   targetScore; "+targetScore+"  iscan; "+iscan);
										totalnamearray[ibodyID] = arg2_0+" "+targetName;
										//		IJ.log("targetName; "+targetName+"   iscan; "+iscan);
										
										fixedFL[ibodyID]=ibodyID;
										break;
									}else{
										totalnamearray[ibodyID] = targetScore+" "+targetName;
										
										//				IJ.log("2893targetName; "+targetName);
										break;
									}
								}//	if(targetName2.equals(oribodyIDtrue)){
							}
							
						}//if(!targetName.equals("NN")){
					}
					
				}
			}//	for(int iscan=0; iscan<totalnamearray.length; iscan++){
			
			
			for(int iscan=0; iscan<totalnamearray.length; iscan++){
				String [] arg2=totalnamearray[iscan].split(" ");
				
				//IJ.log("arg2[0]; "+arg2[0]+"   totalnamearray[iscan]; "+totalnamearray[iscan]);
				double arg2_0=Double.parseDouble(arg2[0]);
				
				//if(test==1){
				//		return;
				//	}
				
				if(arg2_0==Totalscore && arg2_0!=0){
					slicename=arg2[1];
					
					//			if(slicename.equals("001.04_517514142_RT_18U.tif"))
					//			IJ.log("slicename; "+slicename+"  Totalscore; "+Totalscore);
					
					arg2_0=0;
					totalnamearray[iscan]=String.valueOf(arg2[0])+" "+arg2[1];
					iscan=totalnamearray.length;
					break;
				}
			}//for(int iscan=0; iscan<totalnamearray.length; iscan++){
			
			//		IJ.log("slicename; "+slicename);
			
			String ADD0="0";
			if(inew<10)
			ADD0="00";
			else if(inew>99)
			ADD0="";
			
			int slicenameFL=slicename.lastIndexOf("_FL");
			int underSliceIndex=slicename.indexOf("_");
			if(slicenameFL>0)
			slicename=slicename.substring(0,slicenameFL)+".tif";
			
			slicename=slicename.substring(underSliceIndex+1,slicename.length());
			
			
			if(!slicename.equals("NN")){
				String Newslicename="N";
				int skipnext=0;
				for(int searchS=1; searchS<=Finslice; searchS++){
					
					String [] slititle = totalnamearray[searchS-1].split(" ");//newly CDMsearch result title
					
					
					int underindex1 = slititle[1].indexOf("_");
					slititle[1]=slititle[1].substring(underindex1+1,slititle[1].length());
					
					//		IJ.log("2585 "+searchS+" slititle[1]; "+slititle[1]+"   slicename"+slicename);
					
					//FL check to new CDM result from original CDM title//////////
					for(int iorititle=1; iorititle<=Finslice; iorititle++){
						String orititleFL = OriCDMstack.getSliceLabel(iorititle);
						
						FLindex = orititleFL.lastIndexOf("_FL");
						
						if(FLindex>0){
							int orititleindex = orititleFL.indexOf(slititle[1]);
							if(orititleindex>0){
								slititle[1]=slititle[1]+"_FL";
								break;
							}
							
						}//if(FLindex>0){
					}
					
					FLindex = slititle[1].lastIndexOf("_FL");
					
					
					
					if(FLindex!=-1){//if(FLindex!=-1){
						
						Newslicename=slititle[1].substring(0, FLindex);//+".tif";
						
						//		if(slicename.equals("001.04_517514142_RT_18U.tif"))
						//		IJ.log("2612 Newslicename; "+Newslicename+"   slicename; "+slicename+"  fixedFL[searchS-1]; "+fixedFL[searchS-1]);
						
						if(Newslicename.equals(slicename)){
							if(fixedFL[searchS-1]==-1){// does not have the bodyIDfile open
								
								String savename=slititle[1];
								ImagePlus impvst=null;
								ImageProcessor hitslice = null;
								
								if (st3F.isVirtual()){
									VirtualStack vst = (VirtualStack)st3F;
									String dirtmp = vst.getDirectory();
									
									//			IJ.log("dirtmp; "+dirtmp+"   slicename; "+Newslicename);
									
									while(impvst==null){
										impvst = IJ.openImage(dirtmp+Newslicename);
									}
									hitslice = impvst.getProcessor();
								}else{
									hitslice = CDMresultstackFinal.getProcessor(searchS);
									//				IJ.log("Not virtual stack  "+searchS);
								}
								
								
								//	ImagePlus hitsliceIP = new ImagePlus ("SLICE",hitslice);
								
								//	ImageProcessor hitsliceDUP = hitsliceIP.getProcessor();
								//	hitslice.setFont(new Font("SansSerif", Font.PLAIN, 26));
								//	hitsliceDUP.flipHorizontal();
								
								Stackfinal.addSlice(ADD0+inew+"_"+gaparray[inew].substring(0,gaparray[inew].indexOf("."))+"_"+savename, hitslice);
								//		IJ.log("2993 savename; "+savename);
								totalnamearray[searchS-1]="0 NN";
								
								skipnext=1;
								
								addedslice=addedslice+1;
								//			IJ.log("Add slice 2651"+"_"+addedslice);
								//		hitsliceIP.unlock();
								//		hitsliceIP.close();
								
								if(impvst!=null){
									impvst.unlock();
									impvst.close();
								}
								break;
								
								
							}
						}
						
					}//if(FLindex!=-1){
					
					if(FLindex==-1){
						
						//	int underindex=slititle[1].indexOf("_");
						String savename2=slititle[1];
						if(fixedFL[searchS-1]!=-1){
							int dotindex=slititle[1].lastIndexOf(".");
							
							//	IJ.log("3010savename; "+savename+"  dotindex; "+dotindex);
							
							if(dotindex!=-1){
								savename2=slititle[1].substring(0, dotindex)+"_FL.tif";
							}else{
								
								savename2=slititle[1]+"_FL.tif";
							}
							//		IJ.log("3016savename; "+savename2+"  dotindex; "+dotindex+"  slicename; "+slicename);
						}
						//		if(slicename.equals("001.04_517514142_RT_18U.tif"))
						//		IJ.log("2685 "+searchS+" slititle[1]; "+slititle[1]+"   slicename"+slicename+"  Finslice; "+Finslice);
						
						if(slititle[1].equals(slicename)){
							ImageProcessor hitslice = CDMresultstackFinal.getProcessor(searchS);//original search MIP stack
							
							
							Stackfinal.addSlice(ADD0+inew+"_"+gaparray[inew].substring(0,gaparray[inew].indexOf("."))+"_"+savename2, hitslice);
							addedslice=addedslice+1;
							totalnamearray[searchS-1]="0 NN";
							//		IJ.log("Add slice 2694_"+addedslice);
							//	IJ.log(ADD0+inew+gaparray[inew].substring(0,gaparray[inew].indexOf("."))+"_"+savename2);
							
							skipnext=1;
							break;
						}
						//		addslice=1;
						//			IJ.log("slititle; "+slititle);
					}//if(slititle==slicename){
				}//for(searchS=1; seachS<nSlices; searchS++){
				
				if(skipnext==0){
					for(int searchS=1; searchS<=Finslice; searchS++){
						
						String [] slititle = totalnamearray[searchS-1].split(" ");
						
						double scorefinal = Double.parseDouble(slititle[0]);
						
						FLindex = slititle[1].lastIndexOf("_FL");
						
						if(FLindex!=-1 && Totalscore==scorefinal){//if(FLindex!=-1){
							
							int underindex=slititle[1].indexOf("_");
							FLindex = slititle[1].indexOf("_FL");
							
							//	String savename=slititle[1].substring(0, FLindex)+"tif";
							Newslicename=slititle[1].substring(underindex+1, FLindex)+".tif";
							
							//	if(slicename.equals("001.04_517514142_RT_18U.tif"))
							//	IJ.log("Newslicename; "+Newslicename+"  fixedFL[searchS-1]; "+fixedFL[searchS-1]);
							
							
							if(fixedFL[searchS-1]==-1){// does not have the bodyIDfile open
								
								String savename=slititle[1];
								ImageProcessor hitslice =null;
								ImagePlus impvst=null;
								
								if (st3F.isVirtual()){
									VirtualStack vst = (VirtualStack)st3F;
									String dirtmp = vst.getDirectory();
									
									//		IJ.log("dirtmp; "+dirtmp+"   slicename; "+Newslicename);
									
									while(impvst==null){
										impvst = IJ.openImage(dirtmp+Newslicename);
									}
									hitslice = impvst.getProcessor();
								}else{
									hitslice = CDMresultstackFinal.getProcessor(searchS);
									
								}
								
								//hitslice.setFont(new Font("SansSerif", Font.PLAIN, 26));
								
								//	ImagePlus hitsliceIP = new ImagePlus ("SLICE",hitslice);
								
								//		ImageProcessor hitsliceDUP = hitsliceIP.getProcessor();
								//			hitsliceDUP.flipHorizontal();
								//		if(slicename.equals("001.04_517514142_RT_18U.tif"))
								IJ.log("2738 searchS; "+searchS+"  Newslicename; "+Newslicename);
								
								Stackfinal.addSlice(ADD0+inew+"_"+gaparray[inew].substring(0,gaparray[inew].indexOf("."))+"_"+savename, hitslice);
								//		IJ.log("2993 savename; "+savename);
								totalnamearray[searchS-1]="0 NN";
								addedslice=addedslice+1;
								
								//		hitsliceIP.unlock();
								//		hitsliceIP.close();
								
								if(impvst!=null){
									impvst.unlock();
									impvst.close();
								}
								break;
								
								
							}
						}//if(FLindex!=-1){
						
						if(FLindex==-1){
							
							//	int underindex=slititle[1].indexOf("_");
							String savename2=slititle[1];
							if(fixedFL[searchS-1]!=-1){
								int dotindex=slititle[1].lastIndexOf(".");
								
								//	IJ.log("3010savename; "+savename+"  dotindex; "+dotindex);
								
								if(dotindex!=-1){
									savename2=slititle[1].substring(0, dotindex)+"_FL.tif";
								}else{
									
									savename2=slititle[1]+"_FL.tif";
								}
								//		IJ.log("3016savename; "+savename2+"  dotindex; "+dotindex+"  slicename; "+slicename);
							}
							
							int underindex2778 = slititle[1].indexOf("_");
							String newName = slititle[1].substring(underindex2778+1,slititle[1].length());
							
							//		if(slicename.equals("001.04_517514142_RT_18U.tif"))
							//			IJ.log("2781 "+searchS+" newName; "+newName+"   slicename"+slicename+"  Finslice; "+Finslice);
							
							if(newName.equals(slicename)){
								ImageProcessor hitslice = CDMresultstackFinal.getProcessor(searchS);//original search MIP stack
								
								
								Stackfinal.addSlice(ADD0+inew+"_"+gaparray[inew].substring(0,gaparray[inew].indexOf("."))+"_"+savename2, hitslice);
								addedslice=addedslice+1;
								
								totalnamearray[searchS-1]="0 NN";
								//				if(slicename.equals("001.04_517514142_RT_18U.tif"))
								//				IJ.log("3037 savename2; "+savename2+"  fixedFL[searchS-1]; "+fixedFL[searchS-1]+"  searchS-1; "+searchS);
								break;
							}
							//		addslice=1;
							//			IJ.log("slititle; "+slititle);
						}//if(slititle==slicename){
					}//for(searchS=1; seachS<nSlices; searchS++){
				}
				
			}//if(!slicename.equals("NN")){
		}//for(int inew=0; inew<Finslice; inew++){
		
		//		for(int ipri=0; ipri<=Finslice; ipri++)
		//		IJ.log(totalnamearray[ipri]+"  "+ipri);
		
		ImageStack StackEMfinal = new ImageStack (WW,HH);
		ImageStack StackOriginalLMsorted = new ImageStack (WW,HH);
		
		int [] info3= impCDMresult.getDimensions();
		int stackslicenumfinal = info3[3];//52
		
		ImagePlus newimp = new ImagePlus("Search_Result", Stackfinal);
		int [] infofinal= newimp.getDimensions();
		int stackslicenumfinal2 = infofinal[3];//52
		
		IJ.log("stackslicenumfinal; "+stackslicenumfinal+"  stackslicenumfinal2; "+stackslicenumfinal2);
		int orislice=0;
		
		for(int iEMname=1; iEMname<=stackslicenumfinal; iEMname++){
			
			String finalSliceTitle = Stackfinal.getSliceLabel(iEMname);
			
			//		IJ.log("finalSliceTitle; "+finalSliceTitle);
			
			if(stackslicenumEM>1){
				for(int iscan=1; iscan<stackslicenumfinal; iscan++){
					String EMsliceTitle = EMstackF.getSliceLabel(iscan);
					
					
					
					int matching = finalSliceTitle.indexOf(EMsliceTitle);
					if(matching!=-1){
						ImageProcessor ipEM = EMstack.getProcessor(iscan);
						
						int FLindexEM = EMsliceTitle.lastIndexOf("_FL"); 
						
						ImagePlus hitsliceIP = new ImagePlus ("SLICE",ipEM);
						ImageProcessor ipEMdup = hitsliceIP.getProcessor();
						
						
						if(FLindexEM>0){
							ipEMdup.flipHorizontal();
						}
						
						StackEMfinal.addSlice(EMsliceTitle,ipEMdup);
					}
				}	//for(int iscan=1; iscan<stackslicenum; iscan++){
			}//	if(stackslicenumEM>1){
			
			if(stackslicenumEM==1){//if(stackslicenumEM>1){
				
				ImageStack OrigiLMstack = impOriginalCDMF.getStack();
				
				for(int iori=1; iori<=slicenumberF; iori++){
					
					String origiST = OrigiLMstack.getSliceLabel(iori);
					
					int underindex = origiST.indexOf("_");
					int dotindex = origiST.lastIndexOf(".");
					
					if(dotindex<10)
					dotindex=0;
					
					//IJ.log("origiST; "+origiST);
					
					if(dotindex>0)
					linename = origiST.substring(underindex+1,dotindex);
					else
					linename = origiST.substring(underindex+1,origiST.length());
					
					int oriFLindex = linename.lastIndexOf("_FL");
					if(oriFLindex>0)
					linename=linename.substring(0,oriFLindex);
					
					int lineNameIndex = finalSliceTitle.indexOf(linename);
					if(lineNameIndex==-1){
						linename= linename.substring(1,linename.length());
						lineNameIndex = finalSliceTitle.indexOf(linename);
					}
					
					if(lineNameIndex!=-1){
						ImageProcessor ipOriLM = OrigiLMstack.getProcessor(iori);
						//		IJ.log("finalSliceTitle.indexOf(linename); "+finalSliceTitle.indexOf(linename)+"  finalSliceTitle; "+finalSliceTitle+"  linename; "+linename);
						
						ImagePlus hitsliceIP = new ImagePlus ("SLICE",ipOriLM);
						ImageProcessor ipOriLM2 = hitsliceIP.getProcessor();
						
						if(oriFLindex!=-1)
						ipOriLM2.flipHorizontal();
						
						StackOriginalLMsorted.addSlice(origiST,ipOriLM2);
						orislice=orislice+1;
						//		IJ.log("_"+orislice+"_"+origiST);
						
						hitsliceIP.close();
						
						break;
					}
				}//for(int iori=1; iori<=stackslicenum; iori++){
			}
		}//for(int iEMname=1; iEMname<=stackslicenum; iEMname++){
		
		Stack2IMP.close();
		
		
		newimp.unlock();
		newimp.show();
		
		ImagePlus newLMimp = new ImagePlus("original_CDM", StackOriginalLMsorted);
		newLMimp.unlock();
		newLMimp.show();
		
		
		if(stackslicenumEM>1){
			ImagePlus newimpEM = new ImagePlus("Search_Sorted_EM", StackEMfinal);
			newimpEM.unlock();
			newimpEM.show();
		}
		
		long endT=System.currentTimeMillis();
		long gapT=endT-startT;
		
		IJ.log(gapT/1000+" sec for the total sorting");
		System.gc();
		
		return newimp;
	}//public class CDM_area_measure 
	
	ImageProcessor multiply (ImageProcessor ipmaskFlipgradient, ImageProcessor SLICEtifipf, int WWf, int HHf){
		
		int sumpx= WWf*HHf;
		
		ImagePlus impfunction = new ImagePlus ("funcimp", SLICEtifipf);
		ImagePlus impfunction2 = impfunction.duplicate();
		ImageProcessor ipfunction = impfunction2.getProcessor();
		
		for(int ivx=0; ivx<sumpx; ivx++){
			
			int pixneuron = SLICEtifipf.get(ivx);
			int pixgradient = ipmaskFlipgradient.get(ivx);
			
			ipfunction.set(ivx, pixgradient*pixneuron);
			
		}// multiply slice and gradient mask
		
		impfunction.unlock();
		impfunction.hide();
		impfunction.close();
		
		impfunction2.unlock();
		impfunction2.hide();
		impfunction2.close();
		
		return ipfunction;
	}//	public static ImageProcessor multiply (ImagePlus MaskFlipIMPf, ImagePlus SLICEtifimpf){
	
	ImagePlus GradientConv (ImagePlus Stack2IMPF, final int sizenum){
		///////// gradient conversion///////////////////////////////
		final int slicenumber = Stack2IMPF.getStackSize();
		
		int [] info3= Stack2IMPF.getDimensions();
		final int WW2 = info3[0];
		final int HH2 = info3[1];
		
		final AtomicInteger ai = new AtomicInteger(1);
		final Thread[] threads = newThreadArray();
		
		new ImageConverter(Stack2IMPF).convertToGray8();
		new ImageConverter(Stack2IMPF).convertToGray16();
		
		IJ.log("3043 slicenumber; "+String.valueOf(slicenumber));
		
		if(slicenumber>1){
			ImageStack originalGradientStack = Stack2IMPF.getStack();
			for (int ithread = 0; ithread < threads.length; ithread++) {
				threads[ithread] = new Thread() {
					
					{ setPriority(Thread.NORM_PRIORITY); }
					
					public void run() {
						for(int ii=ai.getAndIncrement(); ii<=slicenumber; ii = ai.getAndIncrement()){
							
							ImageProcessor EightIMG = originalGradientStack.getProcessor(ii);
							
							for(int ix=WW2-270; ix<WW2; ix++){
								for(int iy=0; iy<100; iy++){
									
									EightIMG.set(ix,iy,0);
								}
							}
							
							for(int ix=0; ix<330; ix++){
								for(int iy=0; iy<105; iy++){
									
									EightIMG.set(ix,iy,0);//-16777216
								}
							}
							
							ImagePlus impFF = new ImagePlus("singleslice",EightIMG);
							
					//		IJ.run(impFF,"Maximum...", "radius="+sizenum+"");
							impFF=MaxF(impFF,sizenum,1);
							
							new ImageConverter(impFF).convertToGray16();
							EightIMG = impFF.getProcessor();
							
							ImagePlus newimp = NewImage.createFloatImage("distance", WW2, HH2, 1, NewImage.FILL_BLACK);
							ImageProcessor out_ip = newimp.getProcessor();
							
							dt(EightIMG, out_ip, 1);
							newimp.setDisplayRange(0, 65535);
							new ImageConverter(newimp).convertToGray16();
							originalGradientStack.setProcessor(out_ip, ii);
							newimp.close();
							impFF.close();
							
						}//for(int ii=ai.getAndIncrement(); ii<FXYtotalbrick; ii = ai.getAndIncrement()){
				}};
			}//	for (int ithread = 0; ithread < threads.length; ithread++) {
			startAndJoin(threads);
		}else{
			ImageProcessor EightIMG = Stack2IMPF.getProcessor();
			
			for(int ix=WW2-270; ix<WW2; ix++){
				for(int iy=0; iy<100; iy++){
					
					EightIMG.set(ix,iy,0);
				}
			}
			
			for(int ix=0; ix<330; ix++){
				for(int iy=0; iy<105; iy++){
					
					EightIMG.set(ix,iy,0);//-16777216
				}
			}
			
			ImagePlus impFF = new ImagePlus("singleslice",EightIMG);
			
		//	IJ.run(impFF,"Maximum...", "radius="+sizenum+"");
			impFF=MaxF(impFF,sizenum,1);
			
			new ImageConverter(impFF).convertToGray16();
			EightIMG = impFF.getProcessor();
			
		//	ImagePlus newimp = NewImage.createFloatImage("distance", WW2, HH2, 1, NewImage.FILL_BLACK);
			//ImageProcessor out_ip = newimp.getProcessor();
			
			final int pxnum = HH2 * WW2;
			
			for (int i = 0; i < pxnum; i++) {
				if (EightIMG.getf(i) > 1)
				EightIMG.setf(i, 0.0f);
				else
				EightIMG.setf(i, Float.MAX_VALUE);
			}
			
			dt(EightIMG);
			ImagePlus newimp = NewImage.createFloatImage("distance", WW2, HH2, 1, NewImage.FILL_BLACK);
			ImageProcessor out_ip = newimp.getProcessor();
			
			newimp.setDisplayRange(0, 65535);
			new ImageConverter(newimp).convertToGray16();
			EightIMG=newimp.getProcessor();
			newimp.close();
			impFF.close();
			
		}
		return Stack2IMPF;
	}
	
	public long sumPXmeasure (ImagePlus ipmf){
		final int width = ipmf.getWidth();
		final int height = ipmf.getHeight();
		
		int morethan=3;
		ImageProcessor ip2;
		
		ip2=ipmf.getProcessor();
		
		int sumpx = ip2.getPixelCount();
		
		long sumvalue=0;
		for(int i=0; i<sumpx; i++){
			
			int pix = ip2.get (i);	//input
			
			if(pix>morethan){
				sumvalue=sumvalue+pix;
			}
			
			
		}//	for(int i=1; i<sumpx; i++){
		
		ipmf.unlock();
		ipmf.close();
		
		return sumvalue;
	}
	
	ImagePlus setsignal1 (ImagePlus impfunc, int sumpxfunc){
		for(int iff=0; iff<sumpxfunc; iff++){
			
			ImageProcessor ipfunc = impfunc.getProcessor();
			int pix = ipfunc.get (iff);	//input
			
			if(pix>2)
			ipfunc.set (iff, 1);//out put
			
		}//	for(int iff=1; iff<sumpx; iff++){
		
		impfunc.unlock();
		impfunc.close();
		//	System.gc();
		return impfunc;
	}
	
	ImageProcessor gradientslice (ImageProcessor ipgra){
		
		int nextmin=0;	int Stop=0; int Pout=0;
		
		ImagePlus impfunc = new ImagePlus ("funIMP",ipgra);
		
		int Fmaxvalue=255;
		if(impfunc.getType()==impfunc.GRAY8)
		Fmaxvalue=255;	
		else if(impfunc.getType()==impfunc.GRAY16)
		Fmaxvalue=65535;	
		
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
	
	ImageProcessor DeleteOutSideMask (ImageProcessor ipneuron, ImageProcessor ipEMmask){
		
		ImagePlus impneuron = new ImagePlus ("neuronIMP",ipneuron);
		
		int [] infof= impneuron.getDimensions();
		int width = infof[0];
		int height = infof[1];
		int sumpxf=width*height;
		for(int idel=0; idel<sumpxf; idel++){
			
			int neuronpix=ipneuron.get(idel);
			int maskpix=ipEMmask.get(idel);	
			
			
			if(impneuron.getType()!=ImagePlus.COLOR_RGB){
				if(neuronpix>0){
					if(maskpix==0){
						ipneuron.set(idel, 0);
					}
				}
			}else{//if(impneuron.getType()!=ImagePlus.COLOR_RGB){
				if(neuronpix!=-16777216){
					if(maskpix==0){
						ipneuron.set(idel, -16777216);
					}
				}
			}
		}//for(idel=0; idel<sumpxfl idel++){
		
		impneuron.unlock();
		impneuron.close();
		
		return ipneuron;
	}//ImageProcessor DeleteOutSideMask
	
	ImagePlus deleteMatchZandCreateZnegativeScoreIMG (ImagePlus SLICEtifimpF,ImagePlus impOriStackResultF,ImagePlus imp10pxRGBmaskF, int sumpxF) {
		// SLICEtifipF is score gradient 16bit image
		//IPOriStackResultF is original RGBimage from 3D hit stack
		//IP10pxRGBmaskF is dilated RGB mask
		
		ImageProcessor SLICEtifipF=SLICEtifimpF.getProcessor();
		ImageProcessor IPOriStackResultF = impOriStackResultF.getProcessor();
		ImageProcessor IP10pxRGBmaskF = imp10pxRGBmaskF.getProcessor();
		
		int colorFlux=40; // color fluctuation of matching, 5 microns
		
		for(int icompare=0; icompare<sumpxF; icompare++){
			
			int dilatedmaskpix = IP10pxRGBmaskF.get(icompare);
			
			if(dilatedmaskpix!=-16777216){// if 10px expand RGBmask is not 0
				
				int orimaskpix= IPOriStackResultF.get(icompare);
				
				if(orimaskpix!=-16777216){// / if original RGBmask is not 0
					
					//	IJ.log("orimaskpix; "+orimaskpix);
					
					int redDileate = (dilatedmaskpix>>>16) & 0xff;
					int greenDileate = (dilatedmaskpix>>>8) & 0xff;
					int blueDileate = dilatedmaskpix & 0xff;
					
					
					int red1 = (orimaskpix>>>16) & 0xff;
					int green1 = (orimaskpix>>>8) & 0xff;
					int blue1 = orimaskpix & 0xff;
					
					
					int pxGapSlice = calc_slicegap_px(red1, green1, blue1, redDileate, greenDileate, blueDileate,icompare); 
					
					if(colorFlux<=pxGapSlice-colorFlux){// set negative score value to gradient SLICEtifipF
						//		IJ.log("pxGapSlice; "+pxGapSlice);
						SLICEtifipF.set(icompare,pxGapSlice-colorFlux);
					}
				}
			}//if(dilatedmaskpix!=-16777216){// if mask is not 0 RGB
			
		}//for(int icompare=0; icompare<sumpxF; icompare++){
		
		imp10pxRGBmaskF.unlock();
		imp10pxRGBmaskF.close();
		
		impOriStackResultF.unlock();
		impOriStackResultF.close();
		
		SLICEtifimpF.unlock();
		SLICEtifimpF.close();
		
		//	System.gc();
		
		return SLICEtifimpF;
	}//ImageProcessor deleteMatchZandCreateZnegativeScoreIMG
	
	public static int calc_slicegap_px(int red1, int green1, int blue1, int red2, int green2, int blue2, int icompareF) {
		
		int max1stvalMASK=0,max2ndvalMASK=0,max1stvalDATA=0,max2ndvalDATA=0,maskslinumber=0,dataslinumber=0;
		String mask1stMaxColor="Black",mask2ndMaxColor="Black",data1stMaxColor="Black",data2ndMaxColor="Black";
		
		if(red1>=green1 && red1>=blue1){
			max1stvalMASK=red1;
			mask1stMaxColor="red";
			if(green1>=blue1){
				max2ndvalMASK=green1;
				mask2ndMaxColor="green";
			}else{
				max2ndvalMASK=blue1;
				mask2ndMaxColor="blue";
			}
		}else if(green1>=red1 && green1>=blue1){
			max1stvalMASK=green1;
			mask1stMaxColor="green";
			if(red1>=blue1){
				mask2ndMaxColor="red";
				max2ndvalMASK=red1;
			}else{
				max2ndvalMASK=blue1;
				mask2ndMaxColor="blue";
			}
		}else if(blue1>=red1 &&  blue1>=green1){
			max1stvalMASK=blue1;
			mask1stMaxColor="blue";
			if(red1>=green1){
				max2ndvalMASK=red1;
				mask2ndMaxColor="red";
			}else{
				max2ndvalMASK=green1;
				mask2ndMaxColor="green";
			}
		}
		
		if(red2>=green2 && red2>=blue2){
			max1stvalDATA=red2;
			data1stMaxColor="red";
			if(green2>=blue2){
				max2ndvalDATA=green2;
				data2ndMaxColor="green";
			}else{
				max2ndvalDATA=blue2;
				data2ndMaxColor="blue";
			}
		}else if(green2>=red2 && green2>=blue2){
			max1stvalDATA=green2;
			data1stMaxColor="green";
			if(red2>=blue2){
				max2ndvalDATA=red2;
				data2ndMaxColor="red";
			}else{
				max2ndvalDATA=blue2;
				data2ndMaxColor="blue";
			}
		}else if(blue2>=red2 &&  blue2>=green2){
			max1stvalDATA=blue2;
			data1stMaxColor="blue";
			if(red2>=green2){
				max2ndvalDATA=red2;
				data2ndMaxColor="red";
			}else{
				max2ndvalDATA=green2;
				data2ndMaxColor="green";
			}
		}
		
		double maskratio= (double)max2ndvalMASK / (double) max1stvalMASK;
		double dataratio= (double)max2ndvalDATA / (double) max1stvalDATA;
		//	IJ.log("Line 2985");
		String LUT = "127_0_255,125_3_255,124_6_255,122_9_255,121_12_255,120_15_255,119_18_255,118_21_255,116_24_255,115_27_255,114_30_255,113_33_255,112_36_255,110_39_255,109_42_255,108_45_255,106_48_255,105_51_255,104_54_255,103_57_255,101_60_255,100_63_255,99_66_255,98_69_255,96_72_255,95_75_255,94_78_255,93_81_255,92_84_255,90_87_255,89_90_255,87_93_255,86_96_255,84_99_255,83_102_255,81_105_255,80_108_255,78_111_255,77_114_255,75_117_255,74_120_255,72_123_255,71_126_255,69_129_255,68_132_255,66_135_255,65_138_255,63_141_255,62_144_255,60_147_255,59_150_255,57_153_255,56_156_255,54_159_255,53_162_255,51_165_255,50_168_255,48_171_255,47_174_255,45_177_255,44_180_255,42_183_255,41_186_255,39_189_255,38_192_255,36_195_255,35_198_255,33_201_255,32_204_255,30_207_255,29_210_255,27_213_255,26_216_255,24_219_255,23_222_255,21_225_255,20_228_255,18_231_255,16_234_255,14_237_255,12_240_255,9_243_255,6_246_255,3_249_255,1_252_255,0_254_255,3_255_252,6_255_249,9_255_246,12_255_243,15_255_240,18_255_237,21_255_234,24_255_231,27_255_228,30_255_225,33_255_222,36_255_219,39_255_216,42_255_213,45_255_210,48_255_207,51_255_204,54_255_201,57_255_198,60_255_195,63_255_192,66_255_189,69_255_186,72_255_183,75_255_180,78_255_177,81_255_174,84_255_171,87_255_168,90_255_165,93_255_162,96_255_159,99_255_156,102_255_153,105_255_150,108_255_147,111_255_144,114_255_141,117_255_138,120_255_135,123_255_132,126_255_129,129_255_126,132_255_123,135_255_120,138_255_117,141_255_114,144_255_111,147_255_108,150_255_105,153_255_102,156_255_99,159_255_96,162_255_93,165_255_90,168_255_87,171_255_84,174_255_81,177_255_78,180_255_75,183_255_72,186_255_69,189_255_66,192_255_63,195_255_60,198_255_57,201_255_54,204_255_51,207_255_48,210_255_45,213_255_42,216_255_39,219_255_36,222_255_33,225_255_30,228_255_27,231_255_24,234_255_21,237_255_18,240_255_15,243_255_12,246_255_9,249_255_6,252_255_3,254_255_0,255_252_3,255_249_6,255_246_9,255_243_12,255_240_15,255_237_18,255_234_21,255_231_24,255_228_27,255_225_30,255_222_33,255_219_36,255_216_39,255_213_42,255_210_45,255_207_48,255_204_51,255_201_54,255_198_57,255_195_60,255_192_63,255_189_66,255_186_69,255_183_72,255_180_75,255_177_78,255_174_81,255_171_84,255_168_87,255_165_90,255_162_93,255_159_96,255_156_99,255_153_102,255_150_105,255_147_108,255_144_111,255_141_114,255_138_117,255_135_120,255_132_123,255_129_126,255_126_129,255_123_132,255_120_135,255_117_138,255_114_141,255_111_144,255_108_147,255_105_150,255_102_153,255_99_156,255_96_159,255_93_162,255_90_165,255_87_168,255_84_171,255_81_173,255_78_174,255_75_175,255_72_176,255_69_177,255_66_178,255_63_179,255_60_180,255_57_181,255_54_182,255_51_183,255_48_184,255_45_185,255_42_186,255_39_187,255_36_188,255_33_189,255_30_190,255_27_191,255_24_192,255_21_193,255_18_194,255_15_195,255_12_196,255_9_197,255_6_198,255_3_199,255_0_200";
		
		String [] LUTarray = LUT.split(",");
		//	IJ.log("LUTarray.length; "+LUTarray.length);
		if(mask1stMaxColor.equals("red")){//cheking slice num 172-256
			
			if(mask2ndMaxColor.equals("green"))//172-213
			maskslinumber = calc_slicenumber(171, 212, LUTarray,maskratio);
			if(mask2ndMaxColor.equals("blue"))//214-256
			maskslinumber = calc_slicenumber(213, 255, LUTarray,maskratio);
			
		}else if(mask1stMaxColor.equals("green")){//cheking slice num 87-171
			if(mask2ndMaxColor.equals("red"))//129-171
			maskslinumber = calc_slicenumber(128, 170, LUTarray,maskratio);
			if(mask2ndMaxColor.equals("blue"))//87-128
			maskslinumber = calc_slicenumber(86, 127, LUTarray,maskratio);
			
		}else if(mask1stMaxColor.equals("blue")){//cheking slice num 1-86 = 0-85
			if(mask2ndMaxColor.equals("red"))//1-30
			maskslinumber = calc_slicenumber(0, 29, LUTarray,maskratio);
			if(mask2ndMaxColor.equals("green"))//31-86
			maskslinumber = calc_slicenumber(30, 85, LUTarray,maskratio);
		}
		
		
		if(data1stMaxColor.equals("red")){//cheking slice num 172-256
			if(data2ndMaxColor.equals("green")){//172-213
				dataslinumber = calc_slicenumber(171, 212, LUTarray,dataratio);
				//			IJ.log("dataratio; "+dataratio+"  max1stvalDATA; "+max1stvalDATA+"  max2ndvalDATA; "+max2ndvalDATA);
			}else if(data2ndMaxColor.equals("blue"))//214-256
			dataslinumber = calc_slicenumber(213, 255, LUTarray,dataratio);
			
		}else if(data1stMaxColor.equals("green")){//cheking slice num 87-171
			if(data2ndMaxColor.equals("red"))//129-171
			dataslinumber = calc_slicenumber(128, 170, LUTarray,dataratio);
			if(data2ndMaxColor.equals("blue"))//87-128
			dataslinumber = calc_slicenumber(86, 127, LUTarray,dataratio);
			
		}else if(data1stMaxColor.equals("blue")){//cheking slice num 1-86 = 0-85
			if(data2ndMaxColor.equals("red"))//1-30
			dataslinumber = calc_slicenumber(0, 29, LUTarray,dataratio);
			if(data2ndMaxColor.equals("green"))//31-86
			dataslinumber = calc_slicenumber(30, 85, LUTarray,dataratio);
		}
		
		//	IJ.log("dataslinumber; "+dataslinumber+"   maskslinumberl; "+maskslinumber);
		
		if(dataslinumber==0 || maskslinumber==0){
			
			int xx= icompareF%1210;
			int yy=icompareF/1210;
			
			//	IJ.log("no slice matching;  maskslinumber; "+maskslinumber+"   dataslinumber; "+dataslinumber+"  xx; "+xx+"  yy; "+yy);
			//	IJ.log("mask1stMaxColor; "+mask1stMaxColor+"   mask2ndMaxColor; "+mask2ndMaxColor+"   data1stMaxColor; "+data1stMaxColor+"   data2ndMaxColor; "+data2ndMaxColor);
			//	IJ.log("dataratio; "+dataratio+"  max1stvalDATA; "+max1stvalDATA+"  max2ndvalDATA; "+max2ndvalDATA);
			
			return (int) dataslinumber;
		}
		
		
		int gapslicenum = Math.abs(maskslinumber-dataslinumber);
		//	IJ.log("gapslicenum; "+gapslicenum);
		return gapslicenum;
	}//	public static double calc_slicegap_px(i
	
	public static int calc_slicenumber(int countSTnum, int countENDnum, String [] LUTarrayF, double maskratioF){
		//	IJ.log("calc_slicenumber");
		int maskslinumberF=0;
		double mingapratio=1000;
		for(int icolor=countSTnum; icolor<=countENDnum; icolor++){
			
			String [] coloraray= LUTarrayF[icolor].split("_");
			double LUTratio = 0;
			
			//		IJ.log("coloraray.length"+coloraray.length+"  LUTarrayF[icolor]; "+LUTarrayF[icolor]+"   LUTarrayF.length"+LUTarrayF.length);
			double colorR = Double.parseDouble(coloraray[0]);
			double colorG = Double.parseDouble(coloraray[1]);
			double colorB = Double.parseDouble(coloraray[2]);
			
			if(colorB>colorR && colorB>colorG){
				if(colorR>colorG)
				LUTratio = colorR/colorB;
				else if(colorG>colorR)
				LUTratio = colorG/colorB;
				
			}else if(colorG>colorR && colorG>colorB){
				if(colorR>colorB)
				LUTratio = colorR/colorG;
				else if(colorB>colorR)
				LUTratio = colorB/colorG;
				
			}else if(colorR>colorG && colorR>colorB){
				if(colorG>colorB)
				LUTratio = colorG/colorR;
				else if(colorB>colorG)
				LUTratio = colorB/colorR;
			}//	if(coloraray[2]>coloraray[0] && coloraray[2]>coloraray[1]){//	if(coloraray[2]>coloraray[0] && coloraray[2]>coloraray[1]){
			
			if(LUTratio==maskratioF){
				maskslinumberF = icolor+1;
				//		IJ.log("maskslinumber; "+maskslinumberF);
				break;
			}
			
			double gapratio = Math.abs(maskratioF-LUTratio);
			
			if(gapratio<mingapratio){
				mingapratio=gapratio;
				maskslinumberF = icolor+1;
			}
		}
		return maskslinumberF;
	}
	
	
	private Thread[] newThreadArray() {
		int n_cpus = Runtime.getRuntime().availableProcessors();
		//	IJ.log("3282 n_cpus; "+n_cpus+"  threadNumE; "+threadNumE);
		
		if (n_cpus > threadNumE) n_cpus = threadNumE;
		if (n_cpus <= 0) n_cpus = 1;
		//	IJ.log("3284 n_cpus; "+n_cpus);
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
	
	void write16gs(ImagePlus imp, String path) throws Exception {
		ShortProcessor sp = (ShortProcessor)imp.getProcessor();
		BufferedImage bi = sp.get16BitBufferedImage();
		File f = new File(path);
		ImageIO.write(bi, "png", f);
	}
	
	/* dt of binary image using squared distance */
	public void dt(ImageProcessor im, ImageProcessor out, int th) {
		final int width = im.getWidth();
		final int height = im.getHeight();
		
		final int pxnum = width * height;
		
		for (int i = 0; i < pxnum; i++) {
			if (im.getf(i) > th)
			out.setf(i, 0.0f);
			else
			out.setf(i, Float.MAX_VALUE);
		}
		dt(out);
	}
	
	/* dt of 2d function using squared distance */
	public void dt(ImageProcessor im) {
		final int width = im.getWidth();
		final int height = im.getHeight();
		final float[] f = new float[Math.max(width,height)];
		final float[] d = new float[Math.max(width,height)];
		final int[] v = new int[Math.max(width,height)];
		final float[] z = new float[Math.max(width,height)+1];
		int n = 0;
		
		// transform along columns
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				f[y] = im.getf(x, y);
			}
			
			int k = 0;
			n = height;
			v[0] = 0;
			z[0] = -Float.MAX_VALUE;
			z[1] = Float.MAX_VALUE;
			for (int q = 1; q <= n-1; q++) {
				float s  = ((f[q]+q*q)-(f[v[k]]+v[k]*v[k]))/(2*q-2*v[k]);
				while (s <= z[k]) {
					k--;
					s  = ((f[q]+q*q)-(f[v[k]]+v[k]*v[k]))/(2*q-2*v[k]);
				}
				k++;
				v[k] = q;
				z[k] = s;
				z[k+1] = Float.MAX_VALUE;
			}
			k = 0;
			for (int q = 0; q <= n-1; q++) {
				while (z[k+1] < q)
				k++;
				d[q] = (q-v[k])*(q-v[k]) + f[v[k]];
			}
			for (int y = 0; y < height; y++) {
				im.setf(x, y, d[y]);
			}
		}
		
		// transform along rows
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				f[x] = im.getf(x, y);
			}
			
			int k = 0;
			n = width;
			v[0] = 0;
			z[0] = -Float.MAX_VALUE;
			z[1] = Float.MAX_VALUE;
			for (int q = 1; q <= n-1; q++) {
				float s  = ((f[q]+q*q)-(f[v[k]]+v[k]*v[k]))/(2*q-2*v[k]);
				while (s <= z[k]) {
					k--;
					s  = ((f[q]+q*q)-(f[v[k]]+v[k]*v[k]))/(2*q-2*v[k]);
				}
				k++;
				v[k] = q;
				z[k] = s;
				z[k+1] = Float.MAX_VALUE;;
			}
			k = 0;
			for (int q = 0; q <= n-1; q++) {
				while (z[k+1] < q)
				k++;
				d[q] = (q-v[k])*(q-v[k]) + f[v[k]];
			}
			
			for (int x = 0; x < width; x++) {
				im.setf(x, y, d[x]);
			}
		}
		
		final int pxnum = width * height;
		for (int i = 0; i < pxnum; i++) {
			im.setf(i, (float)Math.sqrt(im.getf(i)));
		}
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

	
} //public class Two_windows_mask_search implements PlugInFilter{


