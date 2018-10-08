# PhenomeNet Variant Predictor (PVP) - User Guide
A phenotype-based tool to annotate and prioritize disease variants in WES and WGS data

This user guide have been tested on Ubuntu version 16.04.

For details regarding model training and evaluation, please refer to  dev/ directory above.

## Hardware requirements
 - At least 32 GB RAM.
 - At least 1TB free disk space to process and accommodate the necessary databases for annotation

## Software requirements (for native installation)
 - Any Unix-based operating system
 - Java 8
 - Python 2.7 and install the dependencies with:
 ```
	pip install -r requirements.txt
 ```
 
## Native Installation 
    
 1. Download the distribution file [phenomenet-vp-2.1.zip](https://github.com/bio-ontology-research-group/phenomenet-vp/releases/download/v2.0/phenomenet-vp-2.1.zip)
 2. Download the data files [phenomenet-vp-2.1-data.zip](http://bio2vec.net/pvp/data-v2.1.tar.gz)
 3. Extract the distribution files `phenomenet-vp-2.1.zip `
 4. Extract the data files `data.tar.gz` inside the directory phenomenet-vp-2.1
 5. cd `phenomenet-vp-2.1 `
 6. Run the command: `bin/phenomenet-vp` to display help and parameters.

## Database requirements 
  1. Download [CADD](http://krishna.gs.washington.edu/download/CADD/v1.3/whole_genome_SNVs_inclAnno.tsv.gz) database file.
  2. Download and run the script [generate.sh](http://www.bio2vec.net/pvp/generate.sh) (Requires [TABIX](http://www.htslib.org/doc/tabix.html)).
  3. Copy the generated files `cadd.txt.gz` and `cadd.txt.gz.tbi` to directory `phenomenet-vp-1.0/data/db`.
  4. Download [DANN](https://cbcl.ics.uci.edu/public_data/DANN/data/DANN_whole_genome_SNVs.tsv.bgz) database file and its [indexed](https://cbcl.ics.uci.edu/public_data/DANN/data/DANN_whole_genome_SNVs.tsv.bgz.tbi) file to directory `phenomenet-vp-1.0/data/db`.
  5. Rename the DANN files as `dann.txt.gz` and `dann.txt.gz.tbi` respectively. 

## Docker Container

1. Install [Docker](https://docs.docker.com/)
2. Download the data files
   [phenomenet-vp-2.1-data.zip](http://bio2vec.net/pvp/data-v2.1.tar.gz)
   and database requirements
3. Build phenomenet-vp docker image:
```
   docker build -t phenomenet-vp .
```
4. Run phenomenet
```
   docker run -v $(pwd)/data:/data phenomenet-vp -f data/Miller.vcf -o OMIM:263750 
```

## Parameters
    --file, -f
       Path to VCF file
    --outfile, -of
       Path to results file
    --inh, -i
       Mode of inheritance
       Default: unknown
    --json, -j
       Path to PhenoTips JSON file containing phenotypes
    --omim, -o
       OMIM ID
    --phenotypes, -p
       List of phenotype ids separated by commas
    --human, -h
       Propagate human disease phenotypes to genes only
       Default: false
    --sp, -s
       Propagate mouse and fish disease phenotypes to genes only
       Default: false
    --digenic, -d
       Rank digenic combinations
       Default: false
    --trigenic, -t
       Rank trigenic combinations
       Default: false
    --combination, -c
       Maximum Number of variant combinations to prioritize (for digenic and
       trigenic cases only)
       Default: 1000
     --ngenes, -n
       Number of genes in oligogenic combinations (more than three)
       Default: 4
    --oligogenic, -og
       Rank oligogenic combinations
       Default: false


## Usage:

To run the tool, the user needs to provide a **VCF file** along with either an **OMIM ID** of the disease or a **list of phenotypes (HPO or MPO terms)**.

a) Prioritize disease-causing variants using an OMIM ID:

	bin/phenomenet-vp -f data/Miller.vcf -o OMIM:263750
	
b) Prioritize digenic disease-causing variants using an OMIM ID, and gene-to-phenotype datta from human studies only:

	bin/phenomenet-vp -f data/Miller.vcf -o OMIM:263750 --human --digenic
	
c) Prioritize disease-causing variants using a set of phenotypes, and recessive inheritence mode

	bin/phenomenet-vp -f data/Miller.vcf -p HP:0000007,HP:0000028,HP:0000054,HP:0000077,HP:0000175 -i recessive 
   
The result file will be at the directory containg the input file. The output file has the same name as input file with **.res** extension. For digenic, trigenic or oligogenic prioritization, the result file will have ***.digenic, .trigenic, or .oligogenic*** extension repectivly.
   
# Analysis of Rare Variants:

In order to effectively analysis rare variants, it is strongly recommended to *filter the input VCF files by MAF* prior to running phenomenet-vp on it. To do so, follow the instructions below:

a) Install [VCFtools](https://vcftools.github.io/index.html).

b) Run the following command using VCFtools on your input VCF file *to filter out variants with MAF > 1%*:

	vcftools --vcf input_file.vcf --recode --max-maf 0.01 --out filtered
	
c) Run **PVP** on the output file *filtered.recode.vcf* generated from the command above.
 
# PVP 1.0

The original random-forest-based PVP tool is available to download [here](https://github.com/bio-ontology-research-group/phenomenet-vp/releases/download/v1.0/phenomenet-vp-1.0.zip) along with its required data files [here](http://bio2vec.net/pvp/data-v1.0.tar.gz). The prepared set of exomes and genomes used for the analysis and results are provided [here](http://bio2vec.net/pvp/pvp-1.0/). 

# DeepPVP

The updated neural-network model, DeepPVP is available to download [here](https://github.com/bio-ontology-research-group/phenomenet-vp/releases/download/v2.0/phenomenet-vp-2.1.zip) along with its required data files [here](http://bio2vec.net/pvp/data-v2.1.tar.gz). The prepared set of exomes used for the analysis and comparative results are provided [here](http://bio2vec.net/pvp/deepPVP/clinvar/). The comparison with PVP is based on PVP-1.1 available [here](https://github.com/bio-ontology-research-group/phenomenet-vp/releases/download/v1.1/phenomenet-vp-1.1.zip) along with its required data files [here](http://bio2vec.net/pvp/data-v1.1.tar.gz).

# OligoPVP

OligoPVP is provided as part of DeepPVP tool using the parameters --digenic, --trigenicm and --oligogenic for ranking candidate disease-causing variant pairs and triples. Our prepared set of synthetic genomes digenic combinations are available [here](http://bio2vec.net/pvp/deepPVP/dida/) using data from the DIgenic diseases DAtabase (DIDA). The comparison results with other methods are also provided. Results were obtained using DeepPVP v2.0.


# People

PVP is jointly developed by researchers at the University of Birmingham ([Prof George Gkoutos](https://www.birmingham.ac.uk/staff/profiles/cancer-genomic/gkoutos-georgios.aspx) and his team), University of Cambridge ([Dr Paul Schofield](https://www.pdn.cam.ac.uk/directory/paul-schofield) and his team), and King Abdullah University of Science and Technology ([Prof Vladimir Bajic](https://www.kaust.edu.sa/en/study/faculty/vladimir-bajic), [Robert Hoehndorf](https://borg.kaust.edu.sa/), and teams). 

# Publications

[1] Boudellioua I, Mahamad Razali RB, Kulmanov M, Hashish Y, Bajic VB, Goncalves-Serra E, Schoenmakers N, Gkoutos GV., Schofield PN., and Hoehndorf R. (2017) Semantic prioritization of novel causative genomic variants. PLOS Computational Biology. https://doi.org/10.1371/journal.pcbi.1005500

[2] Boudellioua I, Kulmanov M, Schofield PN., Gkoutos GV., and Hoehndorf R . (2018) OligoPVP: Phenotype-driven analysis of individual genomic information to prioritize oligogenic disease variants. Scientific Reports. https://doi.org/10.1038/s41598-018-32876-3

# License
<pre>
Copyright (c) 2016-2018, King Abdullah University of Science and Technology
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgment:
   This product includes software developed by the King Abdullah University
   of Science and Technology.
4. Neither the name of the King Abdullah University of Science and Technology
   nor the names of its contributors may be used to endorse or promote products
   derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY KING ABDULLAH UNIVERSITY OF SCIENCE AND TECHNOLOGY
''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL KING ABDULLAH UNIVERSITY OF SCIENCE AND TECHNOLOGY 
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
</pre>
