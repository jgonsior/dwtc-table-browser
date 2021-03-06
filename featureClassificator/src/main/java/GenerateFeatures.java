import com.google.common.base.Optional;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import webreduce.extraction.mh.features.FeaturesP2;
import webreduce.extraction.mh.tools.TableConvert;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 *  Generates features and appends them to the sqlite database
 */
public class GenerateFeatures {
	
	public static void main(String[] args) {
		try {
			FeaturesP2 phase2Features = new FeaturesP2();
			TableConvert tableConvert = new TableConvert(2, 2);
			Instances dataSet = new Instances("TestInstances", phase2Features.getAttrVector(), 0);
			
			Class.forName("org.sqlite.JDBC");
			Connection connection = DriverManager.getConnection("jdbc:sqlite:dwtcTableManualClassificator/data.db");
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT count(originalTableType), originalTableType FROM `table` GROUP BY originalTableType ");
			while (resultSet.next()) {
				System.out.println("Original table type: " + resultSet.getString(2) + ": " + resultSet.getInt(1));
			}
			
			System.out.println("\n----------------------------------------\n");
			
			//convert ANDRA to OTHER etc. (caused by usage of google translator)
			PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `table` SET newTableType = ? WHERE newTableType=?");
			preparedStatement.setString(1, "OTHER");
			preparedStatement.setString(2, "ANDRA");
			
			preparedStatement.executeUpdate();
			
			preparedStatement.setString(1, "ENTITY");
			preparedStatement.setString(2, "ENTITET");
			
			preparedStatement.executeUpdate();
			
			preparedStatement.setString(1, "RELATION");
			preparedStatement.setString(2, "RELATIONSHIP");
			
			preparedStatement.executeUpdate();
			
			resultSet = statement.executeQuery("SELECT count(newTableType), newTableType FROM `table` GROUP BY newTableType");
			while (resultSet.next()) {
				System.out.println("New table type: " + resultSet.getString(2) + ": " + resultSet.getInt(1));
			}
			
			System.out.println("\n----------------------------------------\n");
			
			resultSet = statement.executeQuery("SELECT * FROM `table` WHERE newTableType IS NOT NULL");
			
			/*int hori = 0;
			int verti = 0;*/
			
			HashMap<Integer, ArrayList<Instance>> instances = new HashMap<>();
			instances.put(1, new ArrayList<>());
			instances.put(2, new ArrayList<>());
			instances.put(3, new ArrayList<>());
			instances.put(4, new ArrayList<>());
			
			
			while (resultSet.next()) {
				
				//parse database json contents
				JSONArray jsonArrayTable = new JSONArray(resultSet.getString("cells"));
				
				/*
				transformation code which isn't doing much…
				//transform table
				double averageCellLengthRowStd = CellTools.caluclateAverageCellLengthRowStd(jsonArrayTable);
				double averageCellLengthColumnStd = CellTools.caluclateAverageCellLengthColumnStd(jsonArrayTable);
				
				if(averageCellLengthColumnStd < averageCellLengthRowStd) {
					//horizontal
					hori++;
				} else {
					//vertical
					jsonArrayTable = CellTools.transposeTable(jsonArrayTable);
					verti++;
				}*/
				
				String htmlTable = "";
				if (resultSet.getString("htmlCode") != null) {
					htmlTable = "<table>" + resultSet.getString("htmlCode") + "</table>";
				} else {
					// convert json to html code
					htmlTable = "<table>";
					for (int i = 0; i < jsonArrayTable.length(); i++) {
						htmlTable += "<tr>";
						JSONArray row = jsonArrayTable.getJSONArray(i);
						for (int j = 0; j < row.length(); j++) {
							String cell = row.getString(j);
							htmlTable += "<td>" + cell + "</td>";
						}
						htmlTable += "</tr>";
					}
					htmlTable += "</table>";
				}
				
				// let jsoup parse the html code again
				Document document = Jsoup.parse(htmlTable);
				
				Element table = document.select("table").get(0);
				Optional<Element[][]> convertedTable = tableConvert.toTable(table);
				if (!convertedTable.isPresent()) {
					System.out.println("No converted table present…exiting because computer is sad…");
					return;
				}
				
				//calculate all the features and transform them into a weka readable format
				Instance instance = phase2Features.computeFeatures(convertedTable.get());
				
				/*for(int i=0;i<instance.numAttributes();i++) {
					System.out.println(instance.attribute(i).name() + ": " + instance.value(i));
				}
				System.out.println("#################################");*/
				//instance.setValue(0, resultSet.getInt("id"));
				
				//@TODO: should be changed to an enum!
				
				//set new class
				int classAttribute = -1;
				
				switch (resultSet.getString("newTableType")) {
					case "RELATION_V":
					case "ENTITY":
						classAttribute = 2;
						break;
					case "RELATION":
						classAttribute = 1;
						break;
					case "MATRIX":
						classAttribute = 3;
						break;
					case "OTHER":
						classAttribute = 4;
						break;
				}
				
				instance.setValue(instance.classAttribute(), classAttribute);
				
				instances.get(classAttribute).add(instance);
				
			}
			
			//duplicate everything
			/*
				"ENTITY"	"1798"
				"MATRIX"	"846"
				"OTHER"	"831"
				"RELATION"	"2302"
			 */
			Random random = new Random();
			//@todo hardcoded 2302?!
			for (int i = 0; i < 2302; i++) {
				Instance instance = null;
				
				if (i >= 1798) {
					instance = instances.get(2).get(random.nextInt(instances.get(2).size()));
				} else {
					instance = instances.get(2).get(i);
				}
				
				dataSet.add(instance);
				
				dataSet.setClassIndex(instance.classIndex());
			}
			
			for (int i = 0; i < 2302; i++) {
				Instance instance = null;
				
				if (i >= 846) {
					instance = instances.get(3).get(random.nextInt(instances.get(3).size()));
				} else {
					instance = instances.get(3).get(i);
				}
				
				dataSet.add(instance);
				
				dataSet.setClassIndex(instance.classIndex());
			}
			
			for (int i = 0; i < 2302; i++) {
				Instance instance = null;
				
				if (i >= 831) {
					instance = instances.get(4).get(random.nextInt(instances.get(4).size()));
				} else {
					instance = instances.get(4).get(i);
				}
				
				dataSet.add(instance);
				
				dataSet.setClassIndex(instance.classIndex());
			}
			
			for (int i = 0; i < 2302; i++) {
				Instance instance = null;
				
				if (i >= 2302) {
					instance = instances.get(1).get(random.nextInt(instances.get(1).size()));
				} else {
					instance = instances.get(1).get(i);
				}
				
				dataSet.add(instance);
				
				dataSet.setClassIndex(instance.classIndex());
			}
			
			//System.out.println("Hori: " + hori + " verti" + verti);
			ArffSaver arffSaver = new ArffSaver();
			arffSaver.setInstances(dataSet);
			arffSaver.setFile(new File("data.arff"));
			arffSaver.writeBatch();
			
			TrainAndCompareClassifier trainAndCompareClassifier = new TrainAndCompareClassifier();
			trainAndCompareClassifier.main(null);
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}