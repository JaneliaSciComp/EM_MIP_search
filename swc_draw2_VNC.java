import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.PlugIn;

import java.awt.*;
import java.io.*;
import java.util.*;

import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;

import org.apache.commons.io.*;

class Vec4 {
	public double x;
	public double y;
	public double z;
	public double w;
	Vec4(double x_, double y_, double z_, double w_) {
		x = x_; y = y_; z = z_; w = w_;
	}
}

class iVec2 {
	public int x;
	public int y;
	iVec2(int x_, int y_) {
		x = x_; y = y_;
	}
}

public class swc_draw2_VNC implements PlugIn {

	static final String WIDTHV = "SWCD.widthV.int";
	static final String HEIGHTV = "SWCD.heightV.int";
	static final String DEPTHV = "SWCD.depthV.int";
	static final String VOXELWV = "SWCD.voxelwV.double";
	static final String VOXELHV = "SWCD.voxelhV.double";
	static final String VOXELDV = "SWCD.voxeldV.double";
	static final String RADIUSV = "SWCD.radiusV.int";
	static final String IGNOREV = "SWCD.ignoreV.boolean";

	int m_w = (int)Prefs.get(WIDTHV, 1024);
	int m_h = (int)Prefs.get(HEIGHTV, 512);
	int m_d = (int)Prefs.get(DEPTHV, 218);
	double m_vw = (double)Prefs.get(VOXELWV, 0.6200000);
	double m_vh = (double)Prefs.get(VOXELHV, 0.6200000);
	double m_vd = (double)Prefs.get(VOXELDV, 1.0000000);
	int m_r = (int)Prefs.get(RADIUSV, 1);
	boolean m_ignore = (boolean)Prefs.get(IGNOREV, false);

	void draw3dSphere(ImageProcessor ip, ImageProcessor distip, int x, int y, int z, double r, int w, int h, int d, double zratio)
	{
		int ir = (int)Math.ceil(r);
		int izr = (int)Math.ceil(r/zratio);
		for (int dz = -izr; dz <= izr; dz++) {
			for (int dy = -ir; dy <= ir; dy++) {
				for (int dx = -ir; dx <= ir; dx++) {
					int xx = x + dx;
					int yy = y + dy;
					int zz = z + dz;
					double dd = dx*dx + dy*dy + dz*zratio*dz*zratio;
					if (dd <= r*r) {
						if (xx >= 0 && xx < w && yy >= 0 && yy < h && zz >= 0 && zz < d && distip.getf(xx, yy) > dd) {
							distip.setf(xx, yy, (float)dd);
							ip.setf(xx, yy, (float)zz);
						}
					}
				}
			}
		}
	}
	
	void drawRainbowLine(ImageProcessor ip, ImageProcessor distip, int w, int h, int d, double x1, double y1, double z1, double r1, double x2, double y2, double z2, double r2, double zratio)
	{
		double a = Math.abs(x2 - x1), b = Math.abs(y2 - y1), c = Math.abs(z2 - z1);
		int pattern = 0;
		if(a >= b && a >= c) pattern = 0;
		else if(b >= a && b >= c) pattern = 1;
		else if(c >= a && c >= b) pattern = 2;

		if (r1 <= 0.0) r1 = 0.0;
		if (r2 <= 0.0) r2 = 0.0;
		int ir1 = r1 > 0.0 ? (int)Math.ceil(r1) : 0;
		int ir2 = r2 > 0.0 ? (int)Math.ceil(r2) : 0;
		
		double stx = x1, sty = y1, stz = z1;
		double edx = x2, edy = y2, edz = z2;
		double dx, dy, dz, dr;
		int ix, iy, iz;
		int iteration;
		int sign;
		double dtmp;
		
		switch (pattern) {
			case 0:
				sign = x2 > x1 ? 1 : -1;
				dx = sign;
				dy = (y2 - y1) / (x2 - x1);
				dz = (z2 - z1) / (x2 - x1);
				dr = (r2 - r1) / (x2 - x1);
				stx = (int)x1;
				sty = (stx-x1)*dy + y1;
				stz = (stx-x1)*dz + z1;
				edx = (int)x2;
				edy = (edx-x2)*dy + y1;
				edz = (edx-x2)*dz + z1;
				iteration = (int)Math.abs(edx - stx);
				for (int i = 0; i <= iteration; i++) {
					ix = (int)(stx + i*sign);
					iy = (int)(sty + dy*i*sign);
					iz = (int)(stz + dz*i*sign);
					if (ir1 == 0 && ir2 == 0) {
						if (ix >= 0 && ix < w && iy >= 0 && iy < h && iz >= 0 && iz < d) {
							distip.setf(ix, iy, 0.0f);
							ip.setf(ix, iy, (float)iz);
						}
					} else {
						draw3dSphere(ip, distip, ix, iy, iz, r1+dr*i*sign, w, h, d, zratio);
					}
				}
				break;
			case 1:
				sign = y2 > y1 ? 1 : -1;
				dy = sign;
				dz = (z2 - z1) / (y2 - y1);
				dx = (x2 - x1) / (y2 - y1);
				dr = (r2 - r1) / (y2 - y1);
				sty = (int)y1;
				stz = (sty-y1)*dz + z1;
				stx = (sty-y1)*dx + x1;
				edy = (int)y2;
				edz = (edy-y2)*dz + z1;
				edx = (edy-y2)*dx + x1;
				iteration = (int)Math.abs(edy - sty);
				for (int i = 0; i <= iteration; i++) {
					iy = (int)(sty + i*sign);
					iz = (int)(stz + dz*i*sign);
					ix = (int)(stx + dx*i*sign);
					if (ir1 == 0 && ir2 == 0) {
						if (ix >= 0 && ix < w && iy >= 0 && iy < h && iz >= 0 && iz < d) {
							distip.setf(ix, iy, 0.0f);
							ip.setf(ix, iy, (float)iz);
						}
					} else {
						draw3dSphere(ip, distip, ix, iy, iz, r1+dr*i*sign, w, h, d, zratio);
					}
				}
				break;
			case 2:
				sign = z2 > z1 ? 1 : -1;
				dz = sign;
				dx = (x2 - x1) / (z2 - z1);
				dy = (y2 - y1) / (z2 - z1);
				dr = (r2 - r1) / (z2 - z1);
				stz = (int)z1;
				stx = (stz-z1)*dx + x1;
				sty = (stz-z1)*dy + y1;
				edz = (int)z2;
				edx = (edz-z2)*dx + x1;
				edy = (edz-z2)*dy + y1;
				iteration = (int)(Math.abs(edz - stz));
				for (int i = 0; i <= iteration; i++) {
					iz = (int)(stz + i*sign);
					ix = (int)(stx + dx*i*sign);
					iy = (int)(sty + dy*i*sign);
					if (ir1 == 0 && ir2 == 0) {
						if (ix >= 0 && ix < w && iy >= 0 && iy < h && iz >= 0 && iz < d) {
							distip.setf(ix, iy, 0.0f);
							ip.setf(ix, iy, (float)iz);
						}
					} else {
						draw3dSphere(ip, distip, ix, iy, iz, r1+dr*i*sign, w, h, d, zratio);
					}
				}
				break;
		}
	}

	public boolean showDialog() {
        GenericDialog gd = new GenericDialog("Template Settings");
		
		gd.addNumericField("Width",  m_w, 0);
		gd.addNumericField("Height",  m_h, 0);
		gd.addNumericField("Depth",  m_d, 0);
		gd.addNumericField("Voxel_W",  m_vw, 7);
		gd.addNumericField("Voxel_H",  m_vh, 7);
		gd.addNumericField("Voxel_D",  m_vd, 7);
		gd.addNumericField("Radius",  m_r, 0);
		gd.addCheckbox("ignore swc radius", m_ignore);
		
		gd.showDialog();
		if(gd.wasCanceled()){
			return false;
		}
		
		m_w = (int)gd.getNextNumber();
		m_h = (int)gd.getNextNumber();
		m_d = (int)gd.getNextNumber();
		m_vw = (double)gd.getNextNumber();
		m_vh = (double)gd.getNextNumber();
		m_vd = (double)gd.getNextNumber();
		m_r = (int)gd.getNextNumber();
		m_ignore = gd.getNextBoolean();
		
		Prefs.set(WIDTHV, m_w);
		Prefs.set(HEIGHTV, m_h);
		Prefs.set(DEPTHV, m_d);
		Prefs.set(VOXELWV, m_vw);
		Prefs.set(VOXELHV, m_vh);
		Prefs.set(VOXELDV, m_vd);
		Prefs.set(RADIUSV, m_r);
		Prefs.set(IGNOREV, m_ignore);
		
        return true;
    }

	public void run(String arg) {
		
		DirectoryChooser dcin = new DirectoryChooser("input directory");
		String indir = dcin.getDirectory();
		DirectoryChooser dcout = new DirectoryChooser("output directory");
		String outdir = dcout.getDirectory();
		if (!showDialog())
            return;
	
		//ImagePlus bimp = IJ.openImage("C:\\Users\\install.BRUNSC-ML-WW1\\Desktop\\jfrc2_back.png");
		int w = m_w;
		int h = m_h;
		int d = m_d;
		ImagePlus bimp = NewImage.createFloatImage("swc", w, h, 1, NewImage.FILL_BLACK);
		IJ.run(bimp, "PsychedelicRainBow2", "");
		LUT[] luts = bimp.getLuts();
		Color black = new Color(255, 255, 255);
		double pxsize = m_vw;
		//int lw = 2;
		//double scalefac = 0.15;
		double dimz = d*m_vd/pxsize;
		double zratio = m_vd/pxsize;
		
		final File folder = new File(indir);
		for (final File fileEntry : folder.listFiles()) {

			if (!FilenameUtils.getExtension(fileEntry.getName()).equals("swc"))
				continue;

			Map<Integer, Integer> id_corresp = new HashMap<Integer, Integer>();
			ArrayList<Vec4> verts = new ArrayList<Vec4>();
			ArrayList<iVec2> edges = new ArrayList<iVec2>();
			
			try {
				BufferedReader br = new BufferedReader(new FileReader(fileEntry));
				StringBuilder sb = new StringBuilder();
				String line = br.readLine();
				while (line != null) {
					
					if (line.isEmpty() || line.charAt(0) == '#')
					{
						line = br.readLine();
						continue;
					}
					
					String[] tokenstemp = line.split(" ", 0);
					ArrayList<String> tokens = new ArrayList<String>();
					for (int i = 0; i < tokenstemp.length; i++) {
						if (!tokenstemp[i].isEmpty()) tokens.add(tokenstemp[i]);
					}
					if (tokens.size() >= 7)
					{
						double[] v4 = new double[4];
						int ival;
						int id = Integer.parseInt(tokens.get(0));
						v4[0] = Double.parseDouble(tokens.get(2));
						v4[1] = Double.parseDouble(tokens.get(3));
						v4[2] = Double.parseDouble(tokens.get(4));
						if (!tokens.get(5).equals("NA"))
							v4[3] = Double.parseDouble(tokens.get(5));
						else
							v4[3] = 0.0;

						int newid = verts.size();
						id_corresp.put(id, newid);
						verts.add( new Vec4(v4[0], v4[1], v4[2], v4[3]) );

						//IJ.log(""+v4[0]+"  "+v4[1]+"  "+v4[2]+"  "+v4[3]+"  ");
		
						ival = Integer.parseInt(tokens.get(6));
						if (ival != -1)
							edges.add(new iVec2(id, ival));
					}
					line = br.readLine();
				}
				
				for (int i = 0; i < edges.size(); i++)
				{
					iVec2 e = edges.get(i);
					e.x = id_corresp.get(e.x);
					e.y = id_corresp.get(e.y);
				}
				br.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			ImagePlus rgb = NewImage.createRGBImage("swclut", w, h, 1, NewImage.FILL_BLACK);
			ImageProcessor rgbip = rgb.getProcessor();
			ImageProcessor ip = new FloatProcessor(w, h);
			ip.setValue(-1.0);
			ip.fill();
			ImageProcessor distip = new FloatProcessor(w, h);
			distip.setValue(1000000.0);
			distip.fill();

			for (int i = 0; i < edges.size(); i++) {
				iVec2 e = edges.get(i);
				Vec4 v1 = verts.get(e.x);
				Vec4 v2 = verts.get(e.y);
				double x1 = v1.x / pxsize;
				double y1 = v1.y / pxsize;
				double z1 = v1.z / m_vd;
				double r1 = (!m_ignore && v1.w > 0.0) ? v1.w/pxsize*(m_r>0?m_r:1) : m_r ;
				double x2 = v2.x / pxsize;
				double y2 = v2.y / pxsize;
				double z2 = v2.z / m_vd;
				double r2 = (!m_ignore && v2.w > 0.0) ? v2.w/pxsize*(m_r>0?m_r:1) : m_r ;
				drawRainbowLine(ip, distip, w, h, d, x1, y1, z1, r1, x2, y2, z2, r2, zratio);
			}

			//ip.setFont(new Font("Arial", Font.PLAIN, 12));
			//ip.drawString(FilenameUtils.getBaseName(fileEntry.getName()), 5, 20, new Color(0.0f, 0.0f, 0.0f, 0.5f));
			for (int ind=0; ind < w*h; ind++) {
				float val = ip.getf(ind);
				if (val >= 0.0f) {
					int c = (int)(val/d*255.0f);
					if (c > 255) c = 255;
					else if (c < 0) c = 0;
					rgbip.set(ind, luts[0].getRGB(c));
				}
			}
			
			BufferedImage image = rgb.getBufferedImage(); // Your input image
			Iterator<ImageWriter> writer1 = ImageIO.getImageWritersByFormatName("TIFF"); // // Assuming a TIFF plugin is installed
			//	while(writer1.hasNext())
			ImageWriter writer = writer1.next();
			
			try{ // Your output file or stream
				
				int dotindex = fileEntry.getName().lastIndexOf(".");
				String truename = fileEntry.getName().substring(0,dotindex);
				
				ImageOutputStream out = ImageIO.createImageOutputStream(new File(outdir + File.separator + FilenameUtils.getBaseName(fileEntry.getName()) +".tif"));//new FileOutputStream(SdirectoryF+addst+totalslicenum+".tif")
				writer.setOutput(out);
				
				//		IJ.log("file save; "+SdirectoryF+addst+totalslicenum+".tif");
				
				ImageWriteParam param = writer.getDefaultWriteParam();
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionType("PackBits");
				
				writer.write(null, new IIOImage(image, null, null), param);//
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			writer.dispose();
			
			rgb.close();
		}

		bimp.close();
	}

}
