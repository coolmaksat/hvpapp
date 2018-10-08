gunzip -c whole_genome_SNVs_inclAnno.tsv.gz | awk -v OFS='\t' '{print $1,$2,$3,$5,$10,$11,$96,$116}' > cadd.txt
bgzip -c cadd.txt > cadd.txt.gz
tabix -p vcf cadd.txt.gz 
