# EM_MIP Mask Search [![LinkToJanelia](https://github.com/JaneliaSciComp/EM_MIP_search/blob/master/images/jrc_logo_180x40.png)](https://www.janelia.org)
This EM MIP Mask Search plugin does bi-directional shape matching between your mask and the EM single neuron CDM dataset. For the shape matching, it requres "gradient files".
The algorithm created by Hideo Otsuna.  
FIJI plugins created by Hideo Otsuna and Takashi Kawase.  

## Before starting
 1. Launch and update FIJI.
 2. Drag and drop `EM_MIP_Search.jar` on FIJI. Then quit (the plugin installation).  
	(you can get the .jar file from [here](https://github.com/JaneliaSciComp/EM_MIP_Search/blob/master/EM_MIP_Mask_Search.jar)) 

## Startup
Download EM datdaset: (https://www.janelia.org/open-science/color-depth-mip)<br/><br/>
  • EM_Hemibrain_Ver1.2_CDM_PackBits_gradient.zip, The EM CDM has neuron name on their file name. It can search neuron by file browser. It also containeds gradient files for neuron shape matching.<br/><br/>
  • EM_Hemibrain1.2_SWC.zip: SWC files, open with [VVDviewer.](https://github.com/takashi310/VVD_Viewer/releases)<br/><br/>

 

Drag the EM_Hemibrain CDM folder (containing EM hemibrain color depth MIPs) into FIJI, use virtual stack option. 

## Neuron mask creation from the GAL4 color depth MIP (CDM)
1. Open a single tiff that containing the neuron; the image is aligned to JRC2018 template space. (The newly aligned CDM also in https://www.janelia.org/open-science/color-depth-mip)
2. Trace area of interest on the image (use polygon tool and try to be as accurate as possible).
3. Edit > Clear Outside.

## Search EM stack with the mask
Plugins > EM MIP Mask Search    
![ScreenShot0](https://github.com/JaneliaSciComp/EM_MIP_search/blob/master/images/screen.png)
### considerations/ tips:
- If background is too high in the mask, increase the Threshold for mask (max value is 255).  
- "ShowFlip hits on a same side" does X-filp of the image with hit by flipping, "Flip" mark will appear on the top center of the image.
- Pix Color Fluctuation, ±Z slice: better to be 1 for precise matchimng.  
- The search can stop by pushing escape.  

## Synchronize windows
To make sure the position between the mask (the mask neuron) and hits (EM MIP), synchronizing the wingdows is useful function.
 1. Analyze > Tools > Synchronize Windows.  
 2. Select the two windows to synchronize.  
<!-- dummy -->


## Create a list of hit result
`realtime_Result_ctrl_click_substack.ijm` is useful for quickly making a list of lines and the substack while scrolling through the stack of potential hits. 
 1. Open `realtime_Result_ctrl_click_substack.ijm`.
 2. Click window with colorMIP search result stacks.
 3. Run macro (only accepts one open window). Then Result window will be open.
 4. Shift + click on the result stack will add the image name into the Result table.
 5. ctrl + click will create a substack with the Result table from the result stack.  

## Check the EM body ID and the next synaptic connection  
[Neuprint web site](https://neuprint.janelia.org/)<br/><br/>  

## CDM library creation from own EM datasets
1. To create CDM from the swc files with the JRC2018 unisex template transferred. The CDM creation needs SWC_draw2.jar FIJI plugin. 
2. <b>Set up:</b> copy "PsychedelicRainBow2.lut" in to /Fiji.app/luts/ folder. Copy "swc_draw2.jar" & "Gradient_conv_multithread.jar" into /Fiji.app/plugins/ folder
<br> Turn ON "Scijava Jupyter Kernel" in menu: /Help/Update.../Manage upodate sites/. Then restsart Fiji.   
 <br>3. <b>Run the swc_draw2 plugin;</b> you need to set the input folder that has all of the swc with JRC2018 unisex transferred.
	<br><b>1-A.</b> Set output directory.
	<br><b>1-B.</b> Set parameters; JRC2018 Unisex brain; Width 1210, Height 566, Depth 174, Voxel W 0.5189161, Voxel H 0.5189161, Radius 2, Keep OFF ignore swc radius.
	<br><b>1-C.</b> The output is PackBits tiff. This format is supported by “Color_MIP_Mask_Search.jar” and “EM_MIP_Mask_Search.jar”.
	
4. Run the plugin "Gradient_conv_multithread.jar" for the creation of the gradient distance files for the negative shape matching score. The input folder is the EM-CDM files that you made in step 3. 

5. If your CDM dataset is EM-hemibrain, copy the "MAX_hemi_to_JRC2018U_fineTune.png" image into the gradient folder.
