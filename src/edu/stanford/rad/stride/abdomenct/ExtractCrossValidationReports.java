package edu.stanford.rad.stride.abdomenct;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import edu.stanford.rad.stride.ExtractReportsDates;

public class ExtractCrossValidationReports {

	public static void main(String[] args) throws FileNotFoundException,
			IOException, ParseException {
		long startTime = System.currentTimeMillis();
		final File folder = new File("/Users/saeedhp/Dropbox/Stanford/Data/STRIDE/Reports");
		String outputFolder = "files/stride/abdomenCT/";

		Map<Integer, ArrayList<Date>> patientDates = new HashMap<Integer, ArrayList<Date>>();
		Map<Integer, ArrayList<String>> patientProcedureDesc = new HashMap<Integer, ArrayList<String>>();
		Map<Integer, ArrayList<String>> patientReport = new HashMap<Integer, ArrayList<String>>();

		int ndate = 0;
		DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
		Date noDate = formatter.parse("01/01/1900");

		for (final File fileEntry : folder.listFiles()) {
			if (!fileEntry.isDirectory()
					&& !fileEntry.getName().startsWith(".")) {
				String fileName = fileEntry.getName();
				String filepath = fileEntry.getPath();
				System.out.println("Working on " + fileName + "...");

				Scanner scanner = new Scanner(new File(filepath), "UTF-8");
				String text = scanner.useDelimiter("\\Z").next();
				scanner.close();

				String records[] = text.split("\n(?=\\d{9}\t)");

				for (String record : records) {
					String fields[] = record.split("\t\\s*");

					int patID = Integer.parseInt(fields[1]);
					String report = fields[4].trim();

					// Get dates
					Date date = null;
					int trimIndex = report
							.indexOf(" I have personally reviewed the images for this examination and agree");
					if (trimIndex != -1) { //
						String signature = report.substring(trimIndex);
						date = ExtractReportsDates.findDateinString(signature);
					}

					if (date == null || date.equals(noDate)) {
						String firstLine = report.substring(0,
								Math.min(report.length(), 140));
						date = ExtractReportsDates.findDateinString(firstLine);
					}

					if (date.equals(noDate)) {
						ndate++;
					}

					// add procedure Description
					ArrayList<String> procedureDescList = new ArrayList<String>();
					if (patientProcedureDesc.containsKey(patID)) {
						procedureDescList.addAll(patientProcedureDesc
								.get(patID));
					}
					procedureDescList.add(fields[3].trim());
					patientProcedureDesc.put(patID, procedureDescList);

					// add report
					ArrayList<String> reportList = new ArrayList<String>();
					if (patientReport.containsKey(patID)) {
						reportList.addAll(patientReport.get(patID));
					}
					reportList.add(fields[4].trim());
					patientReport.put(patID, reportList);

					// add date
					ArrayList<Date> dateList = new ArrayList<Date>();
					if (patientDates.containsKey(patID)) {
						dateList.addAll(patientDates.get(patID));
					}
					dateList.add(date);
					patientDates.put(patID, dateList);
				}
			}

		}

		int skipped = 0, counter = 0;
		for (int patID : patientProcedureDesc.keySet()) {

			ArrayList<Date> sortedDteList = new ArrayList<Date>();
			sortedDteList.addAll(patientDates.get(patID));
			Collections.sort(sortedDteList);

			Date first = sortedDteList.get(0);
			if (first.equals(noDate)) {
				++skipped;
				continue;
			}
			
			if(sortedDteList.size() < 5) //k
			{
				++skipped;
				continue;
			}
			Date second = sortedDteList.get(1);
			Date third = sortedDteList.get(2);
			Date forth = sortedDteList.get(3);
			Date fifth = sortedDteList.get(4);



			boolean skp = true;
			ArrayList<String> procList = patientProcedureDesc.get(patID);
			for (String proc : procList) {
				if (proc.toLowerCase().contains("ct abdomen")) {
					skp = false;
					break;
				}
			}
			
			String tag = "5-10"; //

			if (!skp) {
				++counter;
				List<String> reportList = patientReport.get(patID);
				List<Date> dateList = patientDates.get(patID);
				int k = reportList.size();
				
				if (k == 5) { //
					PrintWriter pw = new PrintWriter(outputFolder + "crossValidation/"+tag+"/corpus/negative/" + patID + ".txt", "UTF-8");
					for (int i = 0; i < dateList.size(); ++i) {
						if (dateList.get(i).equals(first) || dateList.get(i).equals(second) || dateList.get(i).equals(third) || dateList.get(i).equals(forth) || dateList.get(i).equals(fifth)) {
							pw.println(reportList.get(i));
						}
					}
					pw.close();
				}else if (k > 10) {
					PrintWriter pw = new PrintWriter(outputFolder + "crossValidation/"+tag+"/corpus/positive/" + patID + ".txt", "UTF-8"); 
					for (int i = 0; i < dateList.size(); ++i) {
						if (dateList.get(i).equals(first) || dateList.get(i).equals(second) || dateList.get(i).equals(third) || dateList.get(i).equals(forth) || dateList.get(i).equals(fifth)) {
							pw.println(reportList.get(i));
						}
					}
					pw.close();
				}
			}
		}

		System.out.println("counter: " + counter);
		System.out.println("Records without dates: " + ndate);
		System.out.println("Number of skipped: " + skipped);
		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Finshed in " + totalTime / 1000.0 + " seconds");
	}
}
