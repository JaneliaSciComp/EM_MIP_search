

savedir=$1
swcpath=$2

echo "savedir; "$savedir
echo "swcpath; "$swcpath

if [  -f "/groups/terraincognita/home/otsunah/Desktop/Fiji.app/ImageJ-linux64" ]; then 
  LSF=1
  MBP=0
  desktop=0
fi

if [ -f "/Applications/Fiji.app/Contents/MacOS/ImageJ-macosx" ]; then
  LSF=0

  if [ -d "/Users/otsunah" ]; then
    MBP=1
    desktop=0
  else
    MBP=0
    desktop=1
  fi

fi

bodyID=${bodypath%.*}

echo "MBP; "$MBP"  desktop; "$desktop"  LSF; "$LSF


if [ ! -d $savedir ]; then 
  mkdir "$savedir"
fi



	if [[ $MBP == 1 ]]; then
	CMTK=/Applications/FijizOLD.app/bin/cmtk
	FIJI=/Applications/Fiji.app/Contents/MacOS/ImageJ-macosx
MACRO_DIR="/Users/otsunah/test/EM_SYNAPSE"
	
	fi
	
if [[ $desktop == 1 ]]; then

  FIJI=/Applications/Fiji.app/Contents/MacOS/ImageJ-macosx
  MACRO_DIR=/Volumes/otsuna/Macros

fi
	if [[ $LSF == 1 ]]; then
	
	CMTK=/nrs/scicompsoft/otsuna/CMTK_new2019
	MACRO_DIR=/nrs/scicompsoft/otsuna/Macros
	FIJI="/groups/terraincognita/home/otsunah/Desktop/Fiji.app/ImageJ-linux64"
	TempDir=/nrs/scicompsoft/otsuna/template
	Rscript=/groups/terraincognita/home/otsunah/R-3.6.1/bin/Rscript
	R=/groups/terraincognita/home/otsunah/R-3.6.1/bin/R
	MBP=0
	fi


echo "Macrodir; "$MACRO_DIR

swcmacro=$MACRO_DIR"/SWC_to_nrrd_converter_cluster.ijm"


	
$FIJI --headless -macro $swcmacro "${savedir}/,${swcpath}"
	
	exit 0