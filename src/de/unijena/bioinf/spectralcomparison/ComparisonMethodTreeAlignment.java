package de.unijena.bioinf.spectralcomparison;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.unijena.bioinf.decoy.model.MassBank;
import de.unijena.bioinf.deocy.Utils;
import de.unijena.bioinf.spectralcomparison.ParametersSpectralComparison.COMPARISONPOSTPROCESS;
import de.unijena.bioinf.statistics.Result;

public class ComparisonMethodTreeAlignment implements ComparisonMethod{

	static public final String methodNameLong="TreeAlignment";
	ParametersSpectralComparison p;
	public List<MassBank> s1;
	public List<MassBank> s2;


	public ComparisonMethodTreeAlignment(ParametersSpectralComparison p) {
		this.p=p;
	}

	public String getMethodNameLong(){
		return methodNameLong;
	}

	public void setSpectra(List<MassBank> s1, List<MassBank> s2) {
		this.s1=s1;
		this.s2=s2;
	}
	
	public List<MassBank>[] getSpectra() {
		return new List[]{s1,s2};
	}

	@Override
	public AlignmentMatrix getAlignmentMatrix() {
		if(!p.getOutputFileComparison().getParentFile().exists())p.getOutputFileComparison().getParentFile().mkdirs();
		String s1="-z -x -j " +
    			"-m "+p.getOutputFileComparison()+" " +
    			"--align "+p.p1.getOutputFolderDot()+" " +
    			"--with "+p.p2.getOutputFolderDot();
    	String[] args=s1.split(" ");
        new de.unijena.bioinf.ftalign.Main().run(args);
    	try{
	    	String queryFile=p.p1.getOutputFolderMassbank().getAbsolutePath();
			String resultFile=p.p2.getOutputFolderMassbank().getAbsolutePath();
			BufferedReader br=new BufferedReader(new FileReader(p.getOutputFileComparison()));
			List<String> left=new ArrayList<String>();
			List<String> right=new ArrayList<String>();
			String[] l=br.readLine().split(",");
			for(int i=1;i<l.length;i++)right.add(l[i].replaceAll("\"",""));	
			String line;
			while((line=br.readLine())!=null){
				left.add(line.split(",")[0].replaceAll("\"",""));
			}
			br.close();
			Result results[][]=new Result[left.size()][right.size()];
			br=new BufferedReader(new FileReader(p.getOutputFileComparison()));
			line=br.readLine();
			int i=0;
			while((line=br.readLine())!=null){
				l=line.split(",");
				for(int j=1;j<l.length;j++){
					results[i][j-1]=new Result(0,Double.parseDouble(l[j]));
				}
				i++;
			}
			
			br.close();
	    	return new AlignmentMatrix(queryFile, resultFile, left, right, results);
    	}catch(IOException e){
    		System.err.println(e);
    	}
    	return null;
	}


	public Result getResult(List<double[]> peaksLeft, List<double[]> peaksRight){

		double cos=0;
		double length1=0;
		double length2=0;
		int numberMatches=0;
			

		double maxLeft=0;
		for(double[] p:peaksLeft)maxLeft=Math.max(maxLeft, p[1]);
		double maxRight=0;
		for(double[] p:peaksRight)maxRight=Math.max(maxRight, p[1]);

		List<double[]> p1=new ArrayList<double[]>();		
		for(double[] p:peaksLeft){
//			if(1000*p[1]/maxLeft>=5)
				p1.add(new double[]{p[0],Math.pow(1000*p[1]/maxLeft,0.5)*p[0]/10});
		}
		List<double[]> p2=new ArrayList<double[]>();
		for(double[] p:peaksRight){
//			if(1000*p[1]/maxRight>=5)
				p2.add(new double[]{p[0],Math.pow(1000*p[1]/maxRight,0.5)*p[0]/10});
		}
		
		for(double[] p:p1)length1+=Math.pow(p[1],2);
		for(double[] p:p2)length2+=Math.pow(p[1],2);

		for(double[] peaks1:p1){
			double[] bestMatchingPeak=null;
			double error=Utils.getAbsoluteErrorForMass(peaks1[0], p.ppm, p.ae);
			for(double[] peaks2:p2){
				double diff=Math.abs(peaks1[0]-peaks2[0]);
				if(diff<error&&(bestMatchingPeak==null||peaks1[1]>bestMatchingPeak[1])){
					bestMatchingPeak=peaks2;
				}
			}
			if(bestMatchingPeak!=null){
				cos+=peaks1[1]*bestMatchingPeak[1];
				numberMatches++;
			}
		}

		Result r=new Result(numberMatches, cos/Math.pow(length1,0.5)/Math.pow(length2,0.5));
		return r;
	}

	public double[][] calculateFP(double [][] scores) {

		double[] means = new double[scores.length];
		for (int i=0; i<scores.length; i++){
			double sum =0;
			int n=0;
			for (int j=0; j<scores[i].length; j++){                    	
				if(!Double.isInfinite(scores[i][j])){
					sum += scores[i][j];
					n++;
				}
			}
			means[i] = sum/n;
		}

		double[][] correlationCoefficient = new double[scores.length][scores.length];
		for (int i=0; i<correlationCoefficient.length; i++){
			for (int j=0; j<correlationCoefficient.length; j++){

				double combinedSum =0;
				double isum =0;
				double jsum=0;

				for (int k=0; k<correlationCoefficient[i].length; k++){
					if(!Double.isInfinite(scores[i][k])&&!Double.isInfinite(scores[j][k])){
						combinedSum += (scores[i][k]-means[i])*(scores[j][k]-means[j]);
						isum += Math.pow(scores[i][k]-means[i],2);
						jsum += Math.pow(scores[j][k]-means[j],2);
					}
				}

				correlationCoefficient[i][j]=combinedSum/(Math.pow(isum,0.5)*Math.pow(jsum,0.5));
			}
		}
		return correlationCoefficient;
	}


}