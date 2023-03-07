
run("Misc...", "divide=Infinity save");
List.clear();
setBatchMode(true);


testArg=0;

if(testArg!=0)
args = split(testArg,",");
else
args = split(getArgument(),",");

savedir = args[0];// save dir
path = args[1];// full file path for inport LSM

dotIndexend = lastIndexOf(path, ".swc");

print("savedir; "+savedir);
print("path; "+path);

extsave=File.exists(savedir);

if(extsave!=1)
File.makeDirectory(savedir);

swcname="";

lastfilesep=lastIndexOf(path,"/");
if(lastfilesep<dotIndexend)
swcname=substring(path,lastfilesep+1,dotIndexend);

if(dotIndexend!=-1 && swcname!=""){
	run("swc draw single 3d", "input="+path+" width=1210 height=566 depth=174 voxel_w=0.5189161 voxel_h=0.5189161 voxel_d=1.0000000 radius=1 ignore");
	
	//origi=getTitle();
	
	//run("Duplicate...", "title=dup duplicate");
	//run("Flip Horizontally", "stack");
	//imageCalculator("Max create stack", origi,"dup");
	
	run("Nrrd Writer", "compressed nrrd="+savedir+swcname+".nrrd");
	run("Close All");
}


run("Quit");



