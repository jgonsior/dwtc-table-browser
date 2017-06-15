import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.sql.SQLException;
import java.util.Random;

/**
 * @author: Julius Gonsior
 */
public class TrainAndCompareClassifier {
	
	public static void main(String[] args) {
		try {
			
			DataSource source = new DataSource("data.arff");
			Instances dataSet = source.getDataSet();
			dataSet.setClassIndex(dataSet.numAttributes()-1);
			
			// train new classifier
			RandomForest randomForest = new RandomForest();
			//randomForest.buildClassifier(dataSet);
			
			// evaluate
			Evaluation newEvaluation = new Evaluation(dataSet);
			newEvaluation.crossValidateModel(randomForest, dataSet, 10, new Random(1));
			
			System.out.println(newEvaluation.toSummaryString());
			System.out.println(newEvaluation.toClassDetailsString());
			System.out.println(newEvaluation.toMatrixString());
			
			System.out.println("----------------------------------------");
			
			// compare with results from old Classifier
			Classifier oldClassifier = (Classifier) weka.core.SerializationHelper.read("RandomForest_P2.mdl");
			
			Evaluation oldEvaluation = new Evaluation(dataSet);
			oldEvaluation.evaluateModel(oldClassifier, dataSet);
			
			System.out.println(oldEvaluation.toSummaryString());
			System.out.println(oldEvaluation.toClassDetailsString());
			System.out.println(oldEvaluation.toMatrixString());
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}  catch (Exception e) {
			e.printStackTrace();
		}
	}
}