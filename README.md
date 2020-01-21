# EM_MIP Mask Search [![LinkToJanelia][(https://github.com/JaneliaSciComp/EM_MIP_search/blob/master/images/jrc_logo_180x40.png)](https://www.janelia.org)
The algorithm created by Hideo Otsuna.  
FIJI plugins created by Hideo Otsuna and Takashi Kawase.  

## Before starting
 1. Launch and update FIJI.
 2. Drag and drop `ColorMIP_Mask_Search.jar` on FIJI then quit (the plugin installation).  
	(you can get the .jar file from [here](https://github.com/JaneliaSciComp/EM_MIP_Search/blob/master/EM_MIP_Mask_Search.jar)) 

## Startup
			Download EM datdaset: https://www.janelia.org/open-science/color-depth-mip
			After unzip, there are 5 folders:
			• EMhemibrain_CDM_JRC2018U_radi1: EM hemibrain CDM with radius 1, Poor searching ability, good looking but the buttons are too small.
			• EMhemibrain_CDM_JRC2018U_radi2: EM hemibrain CDM with radius 2, Good searching ability, buttons are nice size but the cell body is too big. 
			• EMhemibrain_SWC_2018U_12232019: SWC files, open with [![VVDviewer] (https://github.com/takashi310/VVD_Viewer/releases)
			• Gradient_tif_JRC2018:Unzip and copy the folder to local SSD. This file needs for LM-EM shape mathching.
				• NBLAST_EMhemibrain_1223_2019: NBLATST file. NBLAST Fiji plugin: https://github.com/JaneliaSciComp/NBLAST_Scripts/releases
				
			Drag the "EMhemibrain_CDM_JRC2018U_radi2" folder (containing EM hemibrain color depth MIPs) into fiji, use virtual stack option. 

## Create mask of neuron of interest from the GAL4 color depth MIP (CDM)
			1. Open a single tiff that containing the neuron from the GAL4 image aligned to JRC2018 template space. (The newly aligned CDM also in https://www.janelia.org/open-science/color-depth-mip)
 2. Trace area of interest on a duplicated slice (use polygon tool and try to be as accurate as possible).
 3. Edit > Clear Outside.

## Search stacks with mask
Plugins > EM MIP Mask Search  
				![ScreenShot0](../images/screen.png)
### considerations/ tips:
 - Show log // -> show NaN for log may be useful for a first pass, just to keep track of all slices.
				- If background is too high in the mask, increase the Threshold for mask (max value is 255).  
				- Pix Color Fluctuation: better to be 1 for precise matchimng.
				- The search can stop by pushing escape.

## Synchronize windows
To make sure the position between the mask (the mask neuron) and hits (EM MIP), synchronizing the wingdows is useful function.
 1. Analyze > Tools > Synchronize Windows.  
 2. Select the two windows to synchronize.  
<!-- dummy -->
![ScreenShot1](../images/scr1.png)  
<br />

## Create a list of candidate lines
`realtime_Result_ctrl_click_substack.ijm` is useful for quickly making a list of lines and the substack while scrolling through the stack of potential hits. 
 1. Open `realtime_Result_ctrl_click_substack.ijm`.
 2. Click window with colorMIP search result stacks.
 3. Run macro (only accepts one open window). Then Result window will be open.
 4. Shift + click on the result stack will add the image name into the Result table.
 5. ctrl + click will create a substack with the Result table from the result stack.
 
