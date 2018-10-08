# DeepPVP - Developer Guide
This document is aimed to provide an overview of DeepPVP training and evaluation. 
For information on using DeepPVP for the analysis of personal exomes/genomes, please refer to the main README file at the repository.

## Required files (Part of DeepPVP data distribution file)
 - a-inferred.owl (for computing similarity score)
 - diseasephenotypes.txt (for computing similarity score))
 - modelphenotypes.txt (for computing similarity score)
 - omim_mode.txt (for annotating disease mode of inheritence) 
 - toplevel.txt (for annotating top level phenotypes features)
 
## Other Requirements
 - At least 32 GB RAM.
 - Any Unix-based operating system
 - JAVA 8
 - Groovy 
 - Python 2.7, with (Tensorflow(1.3.0), Keras(2.0.7), h5py(2.7), numpy, scipy, yaml, pyyaml, pandas(0.20.3), hyperopt, hyperas) libraries
 - CADD, DANN, and GWAVA datbases (see main README file for download guide)
 - 1 TB free disk space to accommodate the necessary databases for annotation

## Similarity Score Generation 
    
 1. Download http://compbio.charite.de/jenkins/job/hpo.annotations.monthly/lastStableBuild/artifact/annotation/ALL_SOURCES_ALL_FREQUENCIES_genes_to_phenotype.txt
 2. Download http://www.informatics.jax.org/downloads/reports/HMD_HumanPhenotype.rpt
 3. Download https://zfin.org/downloads/ortho.txt
 4. Run the script makeSimilarityMatrix.groovy
 5. Run the script normalizeMatrix.groovy, the resulting file norm-phenosim-matrix.txt should be used to annotate similarity between genes and OMIM disease IDs.

## ClinVar Data Preparation 
  1. Download and Unzip ClinVar data ftp://ftp.ncbi.nlm.nih.gov/pub/clinvar/tab_delimited/variant_summary.txt.gz
  2. For pathogenic instances, select GRCh37, with valid OMIM ID, single gene annotation, clinical significance as "Pathogenic", valid RSID, and filter out instances with "conflicting interpretations"
  3. For benign instances, select GRCh37, single gene annotation, clinical significance as "Benign", valid RSID, and filter out instances with "conflicting interpretations"
  4. If REF or ALT alleles are missing, check if available at ftp://ftp.ncbi.nlm.nih.gov/pub/clinvar/vcf_GRCh37/clinvar.vcf.gz, and update accordingly
  5. For pathogenic instances, if an instance is annotated with x OMIM IDS, create x instances with each OMIM ID.
  6. Process the pathogenic instances into variant-disease-zygosity triples, and benign instances into variant-zygosity pairs (As described in DeepPVP manuscript https://www.biorxiv.org/content/early/2018/05/02/311621).
  7. Annotate the training data from step 6, by training features (described in DeepPVP manuscript).

## Model Design 
  1. Preprocess training data as described in DeepPVP manuscript ( handling missing values and categorical features)
  2. The file PVP_data.csv is the output from step 1 (Available at http://bio2vec.net/pvp/PVP_data.csv)

## Model Evaluation 
 - The script rf_final_training.py trains a random forest classifier with the same training and validation data.
 - The script nested_stratification.py splits the data into nested 5-fold cross-validation data.
 - The script hyper_tuning.py is for tuning hyper parameters of the network in nested cross-validation. The file best_model_param_per_fold.txt shows best model parameters obtained. 
 - The script final_split.py to split the data into training and validation sets.
 - The script nn_final_training.py trains the final full model with optimal hyperparameters obtained.
 
