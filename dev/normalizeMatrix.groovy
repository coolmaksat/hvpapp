def omim_max = [:]
def omim_min = [:]

PrintWriter fout = new PrintWriter(new BufferedWriter(new FileWriter("norm-phenosim-matrix.txt")))
count=0
new File("phenosim-matrix.txt").splitEachLine("\t") { line ->
	def omim = line[0]
	double score = Double.parseDouble(line[3])
	if (!(omim in omim_max)) {
		omim_max[omim] = score
		omim_min[omim] = score
	}
	else { //update scores
		double oldmax = omim_max[omim]
		double oldmin = omim_min[omim]
		if (score < oldmin)
			omim_min[omim] = score
		if (score > oldmax)
			omim_max[omim] = score
	}
	count=count+1
	println count
}
println "Finished processing"

//generate the new matrix normalized
new File("phenosim-matrix.txt").splitEachLine("\t") { line ->
	def omim = line[0]
	def entrez = line[1]
	def gene = line[2]
	double score = Double.parseDouble(line[3])
	double newscore = (score - omim_min[omim])/(omim_max[omim] - omim_min[omim])
	fout.println(omim + "\t" + entrez + "\t" + gene + "\t" + newscore)
}

fout.flush()
fout.close()

