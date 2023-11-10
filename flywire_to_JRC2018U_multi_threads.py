import flybrains
import navis
import fafbseg
import os
from multiprocessing import Pool
import shutil

def process_swc(args):
    flywire_swc_path, savedir, directory = args
    neuron = navis.read_swc(flywire_swc_path)
    filename = flywire_swc_path.split('/')[-1]
    ID = filename.split('.')[0]

    transformed_fafb = navis.xform_brain(neuron * 1000, source='FLYWIRE', target='FAFB14', via='FAFB14raw')
    transformed_neuron = navis.xform_brain(transformed_fafb, source='FAFB14', target='JRC2018U')

    trans_swc_path = os.path.join(savedir, f"{ID}_JRC2018U.swc")
    navis.write_swc(transformed_neuron, trans_swc_path)

    # Create a Done directory if it doesnt exist
    done_dir = os.path.join(directory, 'Done')
    os.makedirs(done_dir, exist_ok=True)

    # Move the original .swc to the Done directory
    shutil.move(flywire_swc_path, os.path.join(done_dir, f"{ID}.swc"))

    print(f"Processed and moved {flywire_swc_path} -> {os.path.join(done_dir, f'{ID}_JRC2018U.swc')}")

if __name__ == "__main__":
    directory = "Path to/flywire/swcs/"
    savedir = "Path to/flywire/JRC2018U_swc/"

    # Get a list of all .swc files in the directory
    swc_files = [os.path.join(directory, f) for f in os.listdir(directory) if f.endswith('.swc')]
   # process_swc(swc_files[0])
    # Number of processes you want to run in parallel, for example using 8 cores
    num_processes = 7

    # Use a pool of worker processes to process the .swc files
    with Pool(num_processes) as pool:
        pool.map(process_swc, [(f, savedir, directory) for f in swc_files])