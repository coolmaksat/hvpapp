# phenomenet-vp: A phenotype-based tool to annotate and prioritize disease variants in WES and WGS data: Command Line Executable

## Software and Hardware requirements
 - At least 32 GB RAM.
 - Any Unix-based operating system
 - Java 8 or above
 - At least 170GB free disk space to accommodate the necessary databases for annotation

 
## Installation 
    
 1. Download the distribution file [phenomenet-vp-1.0.zip](https://github.com/bio-ontology-research-group/phenomenet-vp/releases/download/v1.0/phenomenet-vp-1.0.zip)
 2. Download the data files [phenomenet-vp-1.0-data.zip](http://www.cbrc.kaust.edu.sa/onto/pvp/data-v1.0.tar.gz)
 3. Extract the distribution files `phenomenet-vp-1.0.zip `
 4. Extract the data files `data.tar.gz` inside the directory phenomenet-vp-1.0
 5. cd `phenomenet-vp-1.0 `
 6. Run the command: `bin/phenomenet-vp` to display help and parameters.

## Database requirements 
  1. Download [CADD](http://krishna.gs.washington.edu/download/CADD/v1.3/whole_genome_SNVs_inclAnno.tsv.gz) database file.
  2. Unzip `whole_genome_SNVs_inclAnno.tsv.gz`
  3. Download and run the script [generate.sh](http://www.cbrc.kaust.edu.sa/onto/pvp/generate.sh) (Requires [TABIX](http://www.htslib.org/doc/tabix.html)).
  4. Copy the generated files `cadd.txt.gz` and `cadd.txt.gz.tbi` to directory `phenomenet-vp-1.0/data/db`.
  5. Download [DANN](https://cbcl.ics.uci.edu/public_data/DANN/data/DANN_whole_genome_SNVs.tsv.bgz) database file and its [indexed](https://cbcl.ics.uci.edu/public_data/DANN/data/DANN_whole_genome_SNVs.tsv.bgz.tbi) file to directory `phenomenet-vp-1.0/data/db`.
  6. Rename the above two files as `dann.txt.gz` and `dann.txt.gz.tbi` respectively. 
  

## Parameters
    --file, -f
       Path to VCF file
    --inh, -i
       Mode of inheritance (dominant, recessive, x-linked, others, or unknown)
       Default: unknown
    --model, -m
       Prioritization model to be used (Coding or Noncoding)
       Default: Coding
    --omim, -o
       OMIM ID of the input VCF file
    --phenotypes, -p
       List of phenotype ids separated by commas (HPO or MPO terms)
    --human, -h
        Propagate human disease phenotypes to genes only    
    --sp, -s
        Propagate mouse and fish disease phenotypes to genes only	
    --all, -a
       Keep all variants for analysis (i.e. Do not filter variants based on their annotation type as coding variants or noncoding variants)
       Default: false

## Usage:

To run the tool, the user needs to provide a **VCF file** along with either an **OMIM ID** of the disease or a **list of phenotypes (HPO or MPO terms)**.

a) Prioritize disease-causing variants using OMIM ID and coding model while keeping all variants:

	bin/phenomenet-vp -f data/Pfeiffer.vcf -o OMIM:101600 -m Coding -a
	
b) Prioritize disease-causing variants using a set of phenotypes, and parameters: coding model, and dominant inheritence mode, and filter noncoding variants from the result file

	bin/phenomenet-vp -f data/Pfeiffer.vcf -p HP:0000006,HP:0000174,HP:0000194,HP:0000218,HP:0000238,HP:0000244,HP:0000272,HP:0000303,HP:0000316,HP:0000322,HP:0000324,HP:0000327,HP:0000348,HP:0000431,HP:0000452,HP:0000453,HP:0000470,HP:0000486,HP:0000494,HP:0000508,HP:0000586,HP:0000678,HP:0001156,HP:0001249,HP:0002308,HP:0002676,HP:0002780,HP:0003041,HP:0003070,HP:0003196,HP:0003272,HP:0003307,HP:0003795,HP:0004209,HP:0004322,HP:0004440,HP:0005048,HP:0005280,HP:0005347,HP:0006101,HP:0006110,HP:0009602,HP:0009773,HP:0010055,HP:0010669,HP:0011304 -m Coding -i dominant 
   
   The result file will be at the directory containg the input file. The output file has the same name as input file with .res extension.
   
# Analysis of Rare Variants:

In order to effectively analysis rare variants, it is strongly recommended to *filter the input VCF files by MAF* prior to running phenomenet-vp on it. To do so, follow the instructions below:

a) Install [VCFtools](https://vcftools.github.io/index.html).

b) Run the following command using VCFtools on your input VCF file *to filter out variants with MAF > 1%*:

	vcftools --vcf input_file.vcf --recode --max-maf 0.01 --out filtered
	
c) Run **phenomenet-vp** on the output file *filtered.recode.vcf* generated from the command above.
 
# Synthetic Exomes

Our prepared set of synthetic exomes are available [here](http://www.cbrc.kaust.edu.sa/onto/pvp/raw_exomes/). This directory contains VCF-format synthetic exomes. The file `clinvar_variants.txt` contains a list of pathogenic ClinVar variants used to create the synthetic exomes (with their OMIM IDs). The i-th variant in `clinvar_variants.txt` is used to create `var_i.vcf` synthetic exome. The subdirectory `wo_maf` contains unfiltered VCF files while `with_maf/` contains pre-filtered exomes based on MAF (i.e filter out variants with MAF > 1%).

# Synthetic Genomes

Our prepared set of synthetic genomes are available [here](http://www.cbrc.kaust.edu.sa/onto/pvp/raw_genomes/). This directory contains VCF-format synthetic genomes. The file `clinvar_variants.txt` contains a list of pathogenic ClinVar variants used to create the synthetic genomes (with their OMIM IDs). The i-th variant in `clinvar_variants.txt` is used to create `var_i.vcf` synthetic exome. The subdirectory `wo_maf` contains unfiltered VCF files while `with_maf/` contains pre-filtered exomes based on MAF (i.e filter out variants with MAF > 1%).

## Results for the above sets of synetheic exomes and genomes can be found [here](http://www.cbrc.kaust.edu.sa/onto/pvp/exome_results/) and  [here](http://www.cbrc.kaust.edu.sa/onto/pvp/genome_results/). Please note that these results were obtained using PVP version 1.0.

# Contact

# Publications

# License
<pre>
Copyright (c) 2016, King Abdullah University of Science and Technology
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
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
